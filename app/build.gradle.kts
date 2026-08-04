import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    // AGP 9 compiles Kotlin itself (built-in Kotlin), so org.jetbrains.kotlin.android
    // is intentionally not applied: it is incompatible with the new DSL.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// The release workflow commits these values before tagging so F-Droid can read and
// reproduce them from source. Gradle properties and environment variables remain
// available as explicit local/CI overrides.
val versionProperties =
    Properties().apply {
        rootProject.file("version.properties").inputStream().use(::load)
    }

fun versionProp(
    name: String,
): String =
    (project.findProperty(name) as String?)
        ?: System.getenv(name)
        ?: versionProperties.getProperty(name)
        ?: error("Missing $name in version.properties")

val appVersionName = versionProp("VERSION_NAME")
val appVersionCode = versionProp("VERSION_CODE").toInt()

android {
    namespace = "live.pageless.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "live.pageless.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            // Local dev default (emulator alias for the host's localhost);
            // overridable on the login screen. Debug builds also permit
            // cleartext HTTP via app/src/debug/res/xml/network_security_config.
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://10.0.2.2:5050\"")
        }
        release {
            // No baked-in default: users enter their own HTTPS server. Release
            // builds forbid cleartext (see app/src/main/res/xml/...).
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"https://\"")

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    lint {
        // Media3 @UnstableApi usage is opted in explicitly at each call site's
        // class (@OptIn(UnstableApi::class)), so the default checks apply.
        warningsAsErrors = false
        abortOnError = true
    }
}

// Built-in Kotlin (AGP 9) replaces the old android.kotlinOptions block.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    ignoreFailures.set(false)
    filter {
        // Never lint generated sources (KSP/Room/Hilt/BuildConfig, etc.).
        exclude { it.file.path.contains("/build/") }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Media (playback in Phase 5)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Background work (downloads)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)

    // Images
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
