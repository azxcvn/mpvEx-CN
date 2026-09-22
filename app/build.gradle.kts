import com.android.build.api.variant.FilterConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.aboutlibraries)
}

// ---------------------------------------------------------------------------
// Release signing
//
// Credentials are read from `keystore.properties` in the project root, which is
// git-ignored. If that file does not exist the release variant stays unsigned,
// so a fresh clone / CI can still build.
//
// keystore.properties format (use forward slashes, backslash is an escape char):
//   storeFile=C:/path/to/your.jks      # absolute, or relative to the project root
//   storePassword=****
//   keyAlias=****
//   keyPassword=****
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    // Read as UTF-8 on purpose: keystore paths may contain non-ASCII characters.
    keystorePropertiesFile.reader(Charsets.UTF_8).use { load(it) }
  }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

fun signingProperty(name: String): String =
  keystoreProperties.getProperty(name)
    ?: error("keystore.properties is missing '$name' (expected keys: storeFile, storePassword, keyAlias, keyPassword)")

android {
  namespace = "app.marlboroadvance.mpvex"
  compileSdk = 37

  defaultConfig {
    applicationId = "app.marlboroadvance.mpvex"
    minSdk = 26
    targetSdk = 36
    versionCode = 132
    versionName = "1.3.2"

    vectorDrawables {
      useSupportLibrary = true
    }

    buildConfigField("String", "GIT_SHA", "\"${getCommitSha()}\"")
    buildConfigField("int", "GIT_COUNT", getCommitCount())
  }

  flavorDimensions += "distribution"

  productFlavors {
    create("standard") {
      dimension = "distribution"
      isDefault = true
      buildConfigField("boolean", "ENABLE_UPDATE_FEATURE", "true")
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "false")
    }

    create("playstore") {
      dimension = "distribution"
      versionNameSuffix = "-playstore"
      buildConfigField("boolean", "ENABLE_UPDATE_FEATURE", "false")
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "true")
    }

    create("fdroid") {
      dimension = "distribution"
      versionNameSuffix = "-fdroid"
      buildConfigField("boolean", "ENABLE_UPDATE_FEATURE", "false")
      buildConfigField("boolean", "SCOPED_STORAGE_ONLY", "false")
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = false
    }
  }

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = rootProject.file(signingProperty("storeFile"))
        storePassword = signingProperty("storePassword")
        keyAlias = signingProperty("keyAlias")
        keyPassword = signingProperty("keyPassword")
        // Keep the same signing scheme as the already published APKs (v2 only).
        enableV1Signing = false
        enableV2Signing = true
        enableV3Signing = false
      }
    }
  }

  buildTypes {
    named("release") {
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      ndk {
        debugSymbolLevel = "none"
      }
    }

    create("preview") {
      initWith(getByName("release"))
      signingConfig = null
      applicationIdSuffix = ".preview"
      versionNameSuffix = "-${getCommitCount()}"
    }

    named("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-${getCommitCount()}"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/LICENSE*"
      excludes += "META-INF/NOTICE*"
      excludes += "META-INF/*.kotlin_module"
      excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
    }
    jniLibs {
      useLegacyPackaging = true
    }
  }

  @Suppress("UnstableApiUsage")
  androidResources {
    generateLocaleConfig = true
  }
}

androidComponents {
  val abiCodes = mapOf(
    "armeabi-v7a" to 1,
    "arm64-v8a" to 2,
    "x86" to 3,
    "x86_64" to 4
  )

  onVariants { variant ->
    variant.outputs.forEach { output ->
      val abi = output.filters
        .find { it.filterType == FilterConfiguration.FilterType.ABI }
        ?.identifier

      output.versionCode.set(
        (output.versionCode.orNull ?: 0) * 10 + (abiCodes[abi] ?: 0)
      )
    }
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xcontext-parameters",
      "-Xannotation-default-target=param-property",
      "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
      "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    )
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

composeCompiler {
  includeSourceInformation = true
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3.android)
  implementation("com.google.android.material:material:1.13.0")
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.ui.tooling.preview)
  debugImplementation(libs.androidx.ui.tooling)
  implementation(libs.bundles.compose.navigation3)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.constraintlayout)
  implementation("androidx.preference:preference-ktx:1.2.1")
  implementation("androidx.constraintlayout:constraintlayout:2.2.0")
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.mediasession)
  implementation(libs.androidx.documentfile)
  implementation(libs.saveable)

  implementation(platform(libs.koin.bom))
  implementation(libs.bundles.koin)

  // Use local AAR instead of JitPack
  // implementation(libs.seeker)
  implementation(files("libs/seeker-2.0.1.aar"))
  implementation(libs.compose.prefs)
  implementation(libs.aboutlibraries.compose.m3)

  implementation(libs.accompanist.permissions)

  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)

  implementation(libs.truetype.parser)
  implementation(libs.fsaf)
  // Use local AAR instead of JitPack
  // implementation(libs.mediainfo.lib)
  implementation(files("libs/mediainfoAndroid-v1.0.0-fix.aar"))
  implementation(files("libs/mpv-android-lib-v0.0.1.aar"))

  // Network protocol libraries
  implementation(libs.smbj)
  implementation(libs.commons.net)
  // Use local AAR instead of JitPack
  // implementation(libs.sardine.android) {
  //   exclude(group = "xpp3", module = "xpp3")
  // }
  implementation(files("libs/sardine-android-0.8.aar"))
  // Add Simple XML dependency required by sardine-android
  implementation("org.simpleframework:simple-xml:2.7.1") {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "stax", module = "stax-api")
    exclude(group = "stax", module = "stax")
  }
  implementation(libs.nanohttpd)
  // Use local AAR instead of JitPack
  // implementation(libs.lazycolumnscrollbar)
  implementation(files("libs/LazyColumnScrollbar-2.2.0.aar"))
  implementation(libs.reorderable)
}

/* ---------------- Git helpers (configuration-cache compatible) ---------------- */

fun getCommitCount(): String =
  providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
  }.standardOutput.asText.get().trim().ifEmpty { "0" }

fun getCommitSha(): String =
  providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
  }.standardOutput.asText.get().trim().ifEmpty { "unknown" }
