plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
}

android {
  namespace = "com.dsdajabar.simit"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.dsdajabar.simit"
    minSdk = 24            // Android versi menengah ke atas (Bab 8.1, survei perangkat Fase 0)
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0" // mengikuti produk SIMIT (Lampiran C.1: 0.x = pra-pilot)
    // Endpoint API via BuildConfig (bukan System.getenv, yang tak tersedia di device)
    val simitApiBase = (project.findProperty("SIMIT_API_BASE") as String?) ?: "http://10.0.2.2:3000"
    buildConfigField("String", "SIMIT_API_BASE", "\"$simitApiBase\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }

  buildFeatures { compose = true; buildConfig = true }
  packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
  implementation(composeBom)
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

  // Room (basis data lokal terenkripsi: produksi -> SQLCipher, Lampiran B.4)
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  ksp("androidx.room:room-compiler:2.6.1")

  // WorkManager (sinkron offline, Lampiran B.4)
  implementation("androidx.work:work-runtime-ktx:2.9.1")

  testImplementation("junit:junit:4.13.2")
}
