package app.marlboroadvance.mpvex.ui.browser.networkstreaming.clients

import android.net.Uri
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

class SmbClient(private val connection: NetworkConnection) : NetworkClient {
  private var smbClient: SMBClient? = null
  private var smbConnection: Connection? = null
  private var session: Session? = null
  private var baseUrl: String = ""
  private var resolvedHostIp: String = ""
  private var shareName: String = ""

  override suspend fun connect(): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        // Configure SMBJ for SMB 2/3 with Android-compatible settings
        // Note: SMBJ uses Unicode (UTF-16LE) by default for SMB 2.0+ which properly handles international characters
        // Disable signing and encryption to avoid cryptographic issues on Android
        val config = SmbConfig.builder()
          .withTimeout(30000, TimeUnit.MILLISECONDS)
          .withSoTimeout(35000, TimeUnit.MILLISECONDS)
          .withDialects(
            com.hierynomus.mssmb2.SMB2Dialect.SMB_3_1_1,
            com.hierynomus.mssmb2.SMB2Dialect.SMB_3_0_2,
            com.hierynomus.mssmb2.SMB2Dialect.SMB_3_0,
            com.hierynomus.mssmb2.SMB2Dialect.SMB_2_1,
            com.hierynomus.mssmb2.SMB2Dialect.SMB_2_0_2,
          )
          .withDfsEnabled(false)
          .withMultiProtocolNegotiate(true)
          .withSigningRequired(false) // Fix for Android: Disable signing to avoid Key.getEncoded() error
          .withEncryptData(false) // Fix for Android: Disable encryption
          .build()

        smbClient = SMBClient(config)

        // Resolve and verify the host
        val resolvedAddress = try {
          withTimeout(5000) {
            java.net.InetAddress.getByName(connection.host)
          }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
          return@withContext Result.failure(Exception("解析主机超时：${connection.host}"))
        } catch (e: java.net.UnknownHostException) {
          return@withContext Result.failure(Exception("找不到主机：${connection.host}"))
        }

        // Check if the resolved host is reachable
        val isHostReachable = try {
          withTimeout(3000) {
            resolvedAddress.isReachable(2000)
          }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
          false
        } catch (e: Exception) {
          true // Continue anyway if ping fails
        }

        if (!isHostReachable) {
          return@withContext Result.failure(Exception("主机 ${connection.host} 在网络中不可达"))
        }

        // Use the resolved IP address
        val hostForUrl = resolvedAddress.hostAddress ?: connection.host
        resolvedHostIp = hostForUrl

        // Parse the connection path - it should ONLY be the share name
        // Expected format: /ShareName (not /ShareName/folder)
        shareName = connection.path.trim('/')

        if (shareName.isEmpty()) {
          return@withContext Result.failure(
            Exception("缺少共享名称。路径应仅填写共享名称。\n\n示例：/Media 或 /Public\n\n请勿包含文件夹，连接后再进入文件夹。"),
          )
        }

        // Reject paths with subfolders
        if (shareName.contains('/')) {
          return@withContext Result.failure(
            Exception("路径只能填写共享名称，不能是文件夹路径。\n\n示例：使用 /Media，而不是 /Media/Movies\n\n连接后可以进入文件夹。"),
          )
        }

        baseUrl = "smb://${hostForUrl}${if (connection.port != 445) ":${connection.port}" else ""}/${shareName}"

        // Connect to the server
        val connectionResult = try {
          withTimeout(10000) {
            smbConnection = smbClient!!.connect(hostForUrl, connection.port)

            // Create authentication context
            val authContext = if (connection.isAnonymous) {
              AuthenticationContext.anonymous()
            } else {
              AuthenticationContext(
                connection.username,
                connection.password.toCharArray(),
                null, // domain can be null
              )
            }

            // Authenticate
            session = smbConnection!!.authenticate(authContext)

            // Test connection by connecting to the share
            val diskShare = session!!.connectShare(shareName) as? DiskShare
              ?: return@withTimeout Result.failure<Unit>(Exception("共享“${shareName}”不是磁盘共享"))

            try {
              // Test access by listing the share root
              diskShare.list("")

              // Success
              diskShare.close()
              Result.success(Unit)
            } catch (e: Exception) {
              diskShare.close()
              if (e.message?.contains("STATUS_ACCESS_DENIED", ignoreCase = true) == true ||
                e.message?.contains("Access is denied", ignoreCase = true) == true
              ) {
                Result.failure<Unit>(Exception("身份验证失败，请检查用户名和密码。"))
              } else if (e.message?.contains("STATUS_OBJECT_NAME_NOT_FOUND", ignoreCase = true) == true ||
                e.message?.contains("does not exist", ignoreCase = true) == true
              ) {
                Result.failure<Unit>(Exception("共享不存在"))
              } else {
                Result.failure<Unit>(Exception("Connection failed: ${e.message}"))
              }
            }
          }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
          Result.failure(Exception("连接超时，服务器无响应。"))
        } catch (e: Exception) {
          Result.failure(Exception("连接失败：${e.message ?: "未知错误"}"))
        }

        connectionResult
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun disconnect() {
    withContext(Dispatchers.IO) {
      try {
        session?.close()
      } catch (_: Exception) {
      }
      try {
        smbConnection?.close()
      } catch (_: Exception) {
      }
      try {
        smbClient?.close()
      } catch (_: Exception) {
      }
      session = null
      smbConnection = null
      smbClient = null
      baseUrl = ""
      resolvedHostIp = ""
      shareName = ""
    }
  }

  override fun isConnected(): Boolean = session != null && smbConnection != null

  override suspend fun listFiles(path: String): Result<List<NetworkFile>> =
    withContext(Dispatchers.IO) {
      try {
        // Check if we're still connected, if not reconnect
        if (!isConnected() || smbConnection?.isConnected != true) {
          android.util.Log.w("SmbClient", "Connection is stale, reconnecting...")
          disconnect()
          val reconnectResult = connect()
          if (reconnectResult.isFailure) {
            return@withContext Result.failure(Exception("重新连接失败：${reconnectResult.exceptionOrNull()?.message}"))
          }
        }

        val sess = session ?: return@withContext Result.failure(Exception("尚未连接"))

        android.util.Log.d("SmbClient", "=== listFiles called ===")
        android.util.Log.d("SmbClient", "  Input path: '$path'")
        android.util.Log.d("SmbClient", "  Share name: '$shareName'")

        // Build the relative path within the share
        // The 'path' parameter is the navigation path from the share root
        val relativePath = when {
          path.startsWith("smb://") -> {
            // Extract path from smb:// URL
            // Use try-catch for URI parsing as spaces might not be encoded
            val extracted = try {
              val uri = java.net.URI(path)
              // uri.path is like: /shareName/folder/file
              // Remove /shareName to get: folder/file
              val pathParts = uri.path.trim('/').split('/', limit = 2)
              pathParts.getOrNull(1) ?: ""
            } catch (e: Exception) {
              // If URI parsing fails (e.g., due to spaces), extract manually
              val pathAfterProtocol = path.substringAfter("smb://")
              val pathPart = pathAfterProtocol.substringAfter("/") // Remove host
              val pathParts = pathPart.trim('/').split('/', limit = 2)
              pathParts.getOrNull(1) ?: ""
            }
            android.util.Log.d("SmbClient", "  Extracted from SMB URL: '$extracted'")
            extracted
          }

          path == "/" || path.isEmpty() -> {
            // Root of the share
            android.util.Log.d("SmbClient", "  Using share root (empty path)")
            ""
          }

          else -> {
            // Check if path is just the share name (means root)
            val cleaned = path.trim('/')
            if (cleaned.equals(shareName, ignoreCase = true)) {
              android.util.Log.d("SmbClient", "  Path equals share name - using root")
              ""
            } else if (cleaned.startsWith("$shareName/", ignoreCase = true)) {
              // Path includes share name prefix - remove it
              val withoutShare = cleaned.substring(shareName.length + 1)
              android.util.Log.d("SmbClient", "  Removed share prefix: '$withoutShare'")
              withoutShare
            } else {
              // Normal subfolder navigation
              android.util.Log.d("SmbClient", "  Using cleaned path: '$cleaned'")
              cleaned
            }
          }
        }

        android.util.Log.d("SmbClient", "  Final relativePath: '$relativePath'")
        android.util.Log.d("SmbClient", "  Will call: diskShare.list('$relativePath')")

        val diskShare = try {
          sess.connectShare(shareName) as? DiskShare
            ?: return@withContext Result.failure(Exception("共享“${shareName}”不是磁盘共享"))
        } catch (e: Exception) {
          android.util.Log.e("SmbClient", "Failed to connect to share: ${e.message}", e)
          return@withContext Result.failure(Exception("连接共享失败：${e.message}"))
        }

        try {
          // Use a timeout for list operation
          val rawFiles: List<FileIdBothDirectoryInformation> = try {
            withTimeout(15000) {
              diskShare.list(relativePath)
            }
          } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            diskShare.close()
            return@withContext Result.failure(Exception("操作超时，服务器可能较慢或无响应。"))
          }

          android.util.Log.d("SmbClient", "  Listed ${rawFiles.size} items")

          val files = rawFiles.mapNotNull { fileInfo ->
            try {
              val fileName = fileInfo.fileName

              // Skip special entries
              if (fileName == "." || fileName == "..") {
                return@mapNotNull null
              }

              // Skip Windows administrative/system shares
              if (fileName.endsWith("$", ignoreCase = true)) {
                return@mapNotNull null
              }

              // Skip IPC$ and other system shares
              if (fileName.equals("IPC", ignoreCase = true) ||
                fileName.equals("print", ignoreCase = true) ||
                fileName.equals("print$", ignoreCase = true)
              ) {
                return@mapNotNull null
              }

              val isDirectory = fileInfo.fileAttributes and 0x10 != 0L // FILE_ATTRIBUTE_DIRECTORY
              val fileSize = if (isDirectory) 0 else fileInfo.endOfFile

              // Build the full SMB path for this file
              // Store the path with the actual file names (not URL encoded)
              // URL encoding will be done when creating URIs for external use
              val fullPath = if (relativePath.isEmpty()) {
                "smb://${resolvedHostIp}/${shareName}/${fileName}"
              } else {
                "smb://${resolvedHostIp}/${shareName}/${relativePath}/${fileName}"
              }

              NetworkFile(
                name = fileName,
                path = fullPath,
                isDirectory = isDirectory,
                size = fileSize,
                lastModified = fileInfo.lastWriteTime.toEpochMillis(),
                mimeType = if (!isDirectory) getMimeType(fileName) else null,
              )
            } catch (e: Exception) {
              null // Skip files that can't be accessed
            }
          }

          diskShare.close()
          Result.success(files)
        } catch (e: Exception) {
          diskShare.close()
          Result.failure(Exception("列出文件失败：${e.message}"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileStream(path: String): Result<InputStream> =
    withContext(Dispatchers.IO) {
      try {
        val sess = session ?: return@withContext Result.failure(Exception("尚未连接"))

        // Parse the SMB path to get relative path within share
        val relativePath = when {
          path.startsWith("smb://") -> {
            // Extract path from smb:// URL
            // Use try-catch for URI parsing as spaces might not be encoded
            try {
              val uri = java.net.URI(path)
              val pathParts = uri.path.trim('/').split('/', limit = 2)
              pathParts.getOrNull(1) ?: ""
            } catch (e: Exception) {
              // If URI parsing fails (e.g., due to spaces), extract manually
              val pathAfterProtocol = path.substringAfter("smb://")
              val pathPart = pathAfterProtocol.substringAfter("/") // Remove host
              val pathParts = pathPart.trim('/').split('/', limit = 2)
              pathParts.getOrNull(1) ?: ""
            }
          }

          else -> {
            path.trim('/')
          }
        }

        val diskShare = sess.connectShare(shareName) as? DiskShare
          ?: return@withContext Result.failure(Exception("共享“${shareName}”不是磁盘共享"))

        try {
          val file = diskShare.openFile(
            relativePath,
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null,
          )

          val inputStream = file.inputStream

          // Wrap the stream to handle cleanup
          val wrappedStream = object : InputStream() {
            override fun read(): Int = inputStream.read()
            override fun read(b: ByteArray): Int = inputStream.read(b)
            override fun read(b: ByteArray, off: Int, len: Int): Int = inputStream.read(b, off, len)
            override fun available(): Int = inputStream.available()

            override fun close() {
              try {
                inputStream.close()
              } catch (_: Exception) {
              }
              try {
                file.close()
              } catch (_: Exception) {
              }
              try {
                diskShare.close()
              } catch (_: Exception) {
              }
            }
          }

          Result.success(wrappedStream)
        } catch (e: Exception) {
          diskShare.close()
          Result.failure(Exception("打开文件失败：${e.message}"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  override suspend fun getFileUri(path: String): Result<Uri> =
    withContext(Dispatchers.IO) {
      try {
        // For SMB, we'll use the smb:// URI with credentials embedded
        val fullPath =
          if (path.startsWith("smb://")) path else "$baseUrl${if (path.startsWith("/")) path else "/$path"}"

        // Build URI with credentials for mpv
        val uriString =
          if (connection.isAnonymous) {
            fullPath
          } else {
            val hostPart = "${connection.host}${if (connection.port != 445) ":${connection.port}" else ""}"
            val pathPart = if (path.startsWith("/")) path else "/$path"
            "smb://${connection.username}:${connection.password}@$hostPart${connection.path}$pathPart"
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
