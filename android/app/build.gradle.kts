import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// API adresi koda sabitlenmez. Öncelik sırası:
//   1) -Psosyal.apiBaseUrl=... komut satırı / ~/.gradle/gradle.properties
//   2) android/local.properties içindeki sosyal.apiBaseUrl (Git'e girmez)
//   3) debug için emülatör varsayılanı (10.0.2.2 = geliştirme makinesinin localhost'u)
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun configValue(key: String): String? =
    ((project.findProperty(key) as String?) ?: localProperties.getProperty(key))?.takeIf { it.isNotBlank() }

val debugApiBaseUrl = configValue("sosyal.apiBaseUrl") ?: "http://10.0.2.2:8080/"
val releaseApiBaseUrl = configValue("sosyal.releaseApiBaseUrl") ?: "https://api.example.invalid/"

// Uygulama içi güncelleme: en son sürüm bilgisinin (version.json) adresi. Boşsa güncelleme kontrolü kapalıdır.
val updateManifestUrl = configValue("sosyal.updateManifestUrl").orEmpty()

// CI her derlemede artan bir sürüm numarası verir; yerel derlemeler 1 kullanır.
val appVersionCode = configValue("sosyal.versionCode")?.toInt() ?: 1
val appVersionName = configValue("sosyal.versionName") ?: "0.1.0-yerel"

// Yayın imzası yalnızca ortam değişkenlerinden okunur (CI'da GitHub Secrets). Anahtar depoya girmez.
// Aynı anahtarla imzalanan sürümler birbirinin üzerine kurulabilir; anahtar kaybolursa kullanıcılar
// uygulamayı silip yeniden kurmak zorunda kalır.
val releaseKeystoreFile: String? = System.getenv("SOSYAL_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.proje.sosyal.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.proje.sosyal.android"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifestUrl\"")
    }

    signingConfigs {
        if (releaseKeystoreFile != null) {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = System.getenv("SOSYAL_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SOSYAL_KEY_ALIAS") ?: "sosyal"
                keyPassword = System.getenv("SOSYAL_KEY_PASSWORD") ?: System.getenv("SOSYAL_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            // Küçültme (R8) şimdilik kapalı: kural seti gerçek cihazda test edildikten sonra açılacak.
            // Release derlemesi debug değildir (debuggable=false), log basmaz ve yalnızca HTTPS kullanır.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    // Alt çubuğun arkasındaki içeriği bulanıklaştırmak için (Android 12+ gerçek bulanıklık, eskilerde yarı saydam)
    implementation(libs.haze)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
