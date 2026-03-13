import com.android.build.gradle.internal.tasks.factory.dependsOn

private val readAndUnderstoodLicense = false

plugins {
    id("com.android.application")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "io.github.domi04151309.home"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "io.github.domi04151309.home"
        minSdk = 23
        targetSdk = 36
        versionCode = 1130
        versionName = "1.13.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        disable += "MissingTranslation"
    }
    project.tasks.preBuild.dependsOn("license")
}

detekt {
    config.setFrom(file("detekt-config.yml"))
    buildUponDefaultConfig = true
    basePath = rootProject.projectDir.absolutePath
}

tasks.register("license") {
    doFirst {
        val data =
            file("./src/main/res/xml/pref_about.xml")
                .readText()
                .contains("app:key=\"license\"")
        if (!data) {
            throw Exception(
                "Please note that removing the license from the about page is not allowed if you " +
                    "plan to publish your modified version of this app. " +
                    "Please read the project's LICENSE.",
            )
        }
        if (!(
                android.defaultConfig.applicationId?.contains("domi04151309") == true ||
                    readAndUnderstoodLicense
            )
        ) {
            throw Exception(
                "Please make sure you have read and understood the LICENSE!",
            )
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0")
    implementation("com.github.skydoves:colorpickerview:2.4.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
