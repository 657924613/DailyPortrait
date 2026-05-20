/*
 * App 模块 Gradle 配置
 * 应用所有插件并按 architecture-android.md §2 引入完整依赖树
 */
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.missyun.dailyportrait"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.missyun.dailyportrait"
        minSdk = 26 // 兼容 Android 8.0+,覆盖 95%+ 设备
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vector drawable 支持
        vectorDrawables {
            useSupportLibrary = true
        }

        // Sentry DSN：从 sentry.properties 读取（可选）
        // 文件格式：dsn=https://xxx@xxx.ingest.sentry.io/xxx
        // 文件不存在 → DSN 为空字符串 → Sentry 不启用
        val sentryProps = rootProject.file("sentry.properties")
        val sentryDsn = if (sentryProps.exists()) {
            val props = Properties().apply {
                load(sentryProps.inputStream())
            }
            props.getProperty("dsn", "")
        } else ""
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    /**
     * 签名配置
     *
     * Release 签名密钥从 keystore.properties 读取（不进版本控制）
     * 文件格式：
     *     storeFile=../keystore/release.jks
     *     storePassword=xxx
     *     keyAlias=daily_portrait
     *     keyPassword=xxx
     *
     * 生成密钥命令：
     *     keytool -genkey -v -keystore keystore/release.jks \
     *         -keyalg RSA -keysize 2048 -validity 10000 \
     *         -alias daily_portrait
     */
    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore.properties")
            if (keystoreFile.exists()) {
                val props = Properties()
                props.load(keystoreFile.inputStream())
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 如果 keystore.properties 不存在,signingConfig 会保持 null,
            // gradle 会回退到 debug 签名（不能上架但能本地测试）
            val keystoreFile = rootProject.file("keystore.properties")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
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

    // Room schema 导出位置(用于 CI 检查与 Migration 测试)
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM 统一版本
    implementation(platform(libs.androidx.compose.bom))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit (静态版,严禁 Play Services 动态版本)
    implementation(libs.mlkit.face.detection)

    // Media3
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Coil
    implementation(libs.coil.compose)

    // DataStore (isFirstLaunch 首选项)
    implementation(libs.androidx.datastore.preferences)

    // ExifInterface (修复 CameraX 拍照 EXIF 旋转 bug)
    implementation(libs.androidx.exifinterface)

    // Sentry 崩溃监控 —— 暂时禁用排查启动崩溃
    // implementation(libs.sentry.android)

    // LeakCanary 内存泄漏检测 —— 暂时禁用排查启动崩溃
    // debugImplementation(libs.leakcanary.android)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
