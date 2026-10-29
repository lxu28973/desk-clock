plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.deskclock"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.deskclock"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // 固定签名：每次 CI 构建签名一致，才能覆盖安装升级。
    // 只是个人侧载用，密码公开无妨。
    signingConfigs {
        create("fixed") {
            storeFile = file("deskclock.keystore")
            storePassword = "deskclock"
            keyAlias = "deskclock"
            keyPassword = "deskclock"
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
