plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
  }
}

android {
  namespace = "com.lyihub.archiveassistant"
  compileSdk {
    version = release(36)
  }

  val releaseStoreFile = providers.environmentVariable("RELEASE_STORE_FILE").orNull
  val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
  val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
  val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
  fun archiveConfig(name: String, default: String = ""): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).getOrElse(default)

  fun quotedBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

  signingConfigs {
    if (
      releaseStoreFile != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null
    ) {
      create("release") {
        storeFile = file(releaseStoreFile)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  defaultConfig {
    applicationId = "com.lyihub.archiveassistant"
    minSdk = 31
    targetSdk = 36
    versionCode = 2
    versionName = "1.1.0"

    buildConfigField(
      "String",
      "ARCHIVE_DATA_BACKEND",
      quotedBuildConfig(archiveConfig("ARCHIVE_DATA_BACKEND", "LOCAL")),
    )
    buildConfigField(
      "String",
      "ARCHIVE_CLOUD_BASE_URL",
      quotedBuildConfig(archiveConfig("ARCHIVE_CLOUD_BASE_URL")),
    )
    buildConfigField(
      "String",
      "ARCHIVE_CLOUD_WORKSPACE_ID",
      quotedBuildConfig(archiveConfig("ARCHIVE_CLOUD_WORKSPACE_ID", "default")),
    )
    buildConfigField(
      "String",
      "ARCHIVE_CLOUD_API_KEY",
      quotedBuildConfig(archiveConfig("ARCHIVE_CLOUD_API_KEY")),
    )

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug {
      manifestPlaceholders["debugHardwareAccelerated"] =
        archiveConfig("ARCHIVE_TEST_HARDWARE_ACCELERATED", "true")
    }
    release {
      isMinifyEnabled = true
      signingConfig = signingConfigs.findByName("release")
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    jniLibs {
      keepDebugSymbols +=
        setOf(
          "**/libLiteRt.so",
          "**/libLiteRtClGlAccelerator.so",
          "**/libandroidx.graphics.path.so",
          "**/libdatastore_shared_counter.so",
          "**/liblitertlm_jni.so",
        )
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.litertlm.android)
  implementation(libs.okhttp)
  implementation(libs.jsoup)
  implementation(libs.pdfbox.android)
  coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  testImplementation(libs.junit)
  testImplementation(libs.json)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
