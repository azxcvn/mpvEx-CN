package app.marlboroadvance.mpvex.ui.browser.networkstreaming.clients

import android.net.Uri
import android.util.Log
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.DavResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

class WebDavClient(private val connection: NetworkConnection) : NetworkClient {
  companion object {
    private const val TAG = "WebDavClient"
  }

  // Note: Sardine-Android uses OkHttp which properly handles UTF-8 encoding by default
  private var sardine: Sardine? = null

  /**
   * Create a custom OkHttpClient with fixed User-Agent to avoid issues with
   * app name (e.g., "mpvExCN") being detected by servers like PikPak
   * Also sets up preemptive authentication for WebDAV
   */
  private fun createCustomOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
          .header("User-Agent", "mpvEx/1.0 WebDAV")
        
        // Add preemptive Basic authentication if credentials are available
        if (!connection.isAnonymous && connection.username.isNotBlank()) {
          val credentials = okhttp3.Credentials.basic(connection.username, connection.password)
          requestBuilder.header("Authorization", credentials)
          Log.d(TAG, "Added Authorization header for ${connection.username}")
        }
        
        val request = requestBuilder.build()
        Log.d(TAG, "Request: ${request.method} ${request.url}")
        Log.d(TAG, "Headers: ${request.headers}")
        
        val response = chain.proceed(request)
        Log.d(TAG, "Response: ${response.code} ${response.message}")
        
        response
      }
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(60, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  /**
   * Build full WebDAV URL from connection and relative path
   * Standard approach: protocol://host:port/basePath/relativePath
   * relativePath should be relative to the basePath (connection.path)
   * Important: WebDAV URLs should end with / for directories (e.g., Jianguoyun requires this)
   */
  private fun buildUrl(relativePath: String): String {
    val protocol = if (connection.useHttps) "https" else "http"
    val basePath = connection.path.trim('/')
    val cleanPath = relativePath.trim('/')
    
    // If relativePath is "/" or empty, it means we're at the root of the connection
    // In that case, just use the basePath (connection.path)
    // Always ensure the URL ends with / for directory operations
    return when {
      cleanPath.isEmpty() || cleanPath == "/" -> {
        if (basePath.isEmpty()) {
          "$protocol://${connection.host}:${connection.port}/"
        } else {
          "$protocol://${connection.host}:${connection.port}/$basePath/"
        }
      }
      basePath.isEmpty() -> "$protocol://${connection.host}:${connection.port}/$cleanPath/"
      else -> "$protocol://${connection.host}:${connection.port}/$basePath/$cleanPath/"
    }
  }

  override suspend fun connect(): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        // Create Sardine with custom OkHttpClient
        // Note: We handle authentication in the interceptor, not via Sardine's setCredentials
        val okHttpClient = createCustomOkHttpClient()
        val client = OkHttpSardine(okHttpClient)
        
        Log.d(TAG, "Connecting - username: ${connection.username}, isAnonymous: ${connection.isAnonymous}")

        // Test connection by listing the base path (PROPFIND instead of HEAD/EXISTS)
        // Some WebDAV servers handle PROPFIND better than HEAD
        val testUrl = buildUrl("")
        Log.d(TAG, "Testing connection to: $testUrl")
        try {
          client.list(testUrl, 0)
          Log.d(TAG, "Connection successful!")
        } catch (e: Exception) {
          Log.e(TAG, "PROPFIND failed, trying EXISTS", e)
          // Fallback to exists check
          client.exists(testUrl)
          Log.d(TAG, "Connection successful via EXISTS!")
        }

        sardine = client
        Result.success(Unit)
      } catch (e: Exception) {
        Log.e(TAG, "Connection failed: ${e.message}", e)
        Result.failure(e)
      }
    }

  override suspend fun disconnect() {
    withContext(Dispatchers.IO) {
      sardine = null
    }
  }

  override fun isConnected(): Boolean = sardine != null

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> =
    withContext(Dispatchers.IO) {
      try {
        val client = sardine ?: return@withContext Result.failure(Exception("Not connected"))

        val url = buildUrl(path)
        val resources = client.list(url)

        val files =
          resources
            .drop(1) // Skip the directory itself
            .map { resource: DavResource ->
              val resourceName = resource.name ?: ""
              
              // Build child path by appending filename to current path
              val filePath = if (path.isEmpty() || path == "/") {
                resourceName
              } else {
                "${path.trimEnd('/')}/$resourceName"
              }

              NetworkFile(
                name = resourceName,
                path = filePath,
                isDirectory = resource.isDirectory,
                size = resource.contentLength ?: 0,
                lastModified = resource.modified?.time ?: 0,
                mimeType = if (!resource.isDirectory) getMimeType(resourceName) else null,
              )
            }

        Result.success(files)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  /**
   * Get file size for a specific file path
   * This is useful for the proxy server to support range requests
   */
  suspend fun getFileSize(path: String): Result<Long> =
    withContext(Dispatchers.IO) {
      try {
        val client = sardine ?: return@withContext Result.failure(Exception("Not connected"))

        val url = buildUrl(path)
        
        // Use PROPFIND to get file properties including size
        val resources = client.list(url, 0) // depth 0 = only the resource itself
        if (resources.isNotEmpty() && !resources[0].isDirectory) {
          val size = resources[0].contentLength ?: -1L
          Result.success(size)
        } else {
          Result.failure(Exception("File not found or is a directory"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileStream(path: String): Result<InputStream> =
    withContext(Dispatchers.IO) {
      try {
        // Create a fresh Sardine client for this stream to avoid connection conflicts
        // Authentication is handled by the custom OkHttpClient interceptor
        val okHttpClient = createCustomOkHttpClient()
        val streamClient = OkHttpSardine(okHttpClient)

        val url = buildUrl(path)
        val rawStream = streamClient.get(url)

        if (rawStream == null) {
          return@withContext Result.failure(Exception("Failed to open WebDAV stream"))
        }

        // Wrap the stream
        val wrappedStream = object : InputStream() {
          override fun read(): Int = rawStream.read()

          override fun read(b: ByteArray): Int = rawStream.read(b)

          override fun read(b: ByteArray, off: Int, len: Int): Int = rawStream.read(b, off, len)

          override fun available(): Int = rawStream.available()

          override fun close() {
            try {
              rawStream.close()
            } catch (e: Exception) {
              // Ignore
            }
          }
        }

        Result.success(wrappedStream)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileUri(path: String): Result<Uri> =
    withContext(Dispatchers.IO) {
      try {
        val protocol = if (connection.useHttps) "https" else "http"
        val basePath = connection.path.trim('/')
        val cleanPath = path.trim('/')
        
        val fullPath = when {
          cleanPath.isEmpty() -> basePath
          basePath.isEmpty() -> cleanPath
          else -> "$basePath/$cleanPath"
        }

        // Build WebDAV URI with credentials embedded for mpv
        val uriString = if (connection.isAnonymous) {
          "$protocol://${connection.host}:${connection.port}/$fullPath"
        } else {
          "$protocol://${connection.username}:${connection.password}@${connection.host}:${connection.port}/$fullPath"
        }

        Result.success(Uri.parse(uriString))
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun getMimeType(fileName: String): String? {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when (extension) {
      "mp4", "m4v" -> "video/mp4"
      "mkv" -> "video/x-matroska"
      "avi" -> "video/x-msvideo"
      "mov" -> "video/quicktime"
      "wmv" -> "video/x-ms-wmv"
      "flv" -> "video/x-flv"
      "webm" -> "video/webm"
      "mpeg", "mpg" -> "video/mpeg"
      "3gp" -> "video/3gpp"
      "ts" -> "video/mp2t"
      else -> null
    }
  }
}
