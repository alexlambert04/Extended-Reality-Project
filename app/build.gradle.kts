import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun resolveConfigValue(project: Project, key: String): String {
    val localValue = localProperties.getProperty(key)
    if (!localValue.isNullOrBlank()) {
        return localValue
    }
    return (project.findProperty(key) as String?) ?: ""
}

fun toBuildConfigString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "be.kuleuven.gt.extendedrealityproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "be.kuleuven.gt.extendedrealityproject"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val supabaseUrl = resolveConfigValue(project, "SUPABASE_URL")
        val supabaseAnonKey = resolveConfigValue(project, "SUPABASE_ANON_KEY")
        buildConfigField("String", "SUPABASE_URL", toBuildConfigString(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", toBuildConfigString(supabaseAnonKey))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.arcore)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.okhttp)
    implementation(libs.glide)
    implementation(libs.webkit)
    implementation(libs.exifinterface)
    implementation(libs.androidx.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}