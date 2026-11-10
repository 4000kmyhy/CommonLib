plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yhy.commonlib"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.yhy.commonlib"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 13
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        multiDexEnabled = true

//        val localProperties = Properties().apply {
//            load(rootProject.file("local.properties").inputStream())
//        }
//        buildConfigField(
//            "String",
//            "GITEE_API_KEY",
//            localProperties.getProperty("GITEE_API_KEY")
//        )
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = false
        disable += setOf("MissingTranslation")
        checkReleaseBuilds = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.accompanist.permissions)

    implementation(libs.multidex)

    implementation(libs.glide)
    implementation(libs.glide.transformations)
    implementation(libs.jsoup)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.gson)

//    implementation(project(":lib_auto_mix"))
//    implementation(project(":lib_2048"))
    implementation(project(":lib_bass"))
//    implementation(project(":lib_eq_preset"))
//    implementation(project(":lib_lyric"))
    implementation(project(":lib_retrofit"))
    implementation(project(":lib_web_pic"))
    implementation(project(":lib_piano"))

//    implementation("com.github.4000kmyhy.CommonLib:lib_lyric:1.1.2")
    implementation(project(":lib_lyric"))
    implementation("com.github.4000kmyhy.CommonLib:lib_auto_mix:1.1.2")
//    implementation("com.github.4000kmyhy.CommonLib:lib_bass:1.1.2")
    implementation("com.github.4000kmyhy.CommonLib:lib_2048:1.1.2")
//    implementation("com.github.4000kmyhy.CommonLib:lib_compose:1.1.2")
    implementation(project(":lib_compose"))
}