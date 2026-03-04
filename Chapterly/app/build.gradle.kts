import groovy.json.JsonSlurper
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    jacoco
}

val googleServicesConfig = file("google-services.json")
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val booksApiKey =
    (
        providers.environmentVariable("BOOKS_API_KEY").orNull
            ?: localProperties.getProperty("BOOKS_API_KEY")
    ).orEmpty().trim()
val backendBaseUrl =
    (
        providers.environmentVariable("BACKEND_BASE_URL").orNull
            ?: localProperties.getProperty("BACKEND_BASE_URL")
            ?: "http://10.0.2.2:8080/"
    ).trim().let { configured ->
        if (configured.endsWith("/")) configured else "$configured/"
    }

val hasValidGoogleServicesConfig =
    googleServicesConfig.exists() &&
        googleServicesConfig.length() > 0L &&
        runCatching {
            val root = JsonSlurper().parse(googleServicesConfig)
            root is Map<*, *> && root["project_info"] != null && root["client"] is Collection<*>
        }.getOrDefault(false)

if (googleServicesConfig.exists() && !hasValidGoogleServicesConfig) {
    logger.warn(
        "Skipping Google Services plugin because app/google-services.json is empty or malformed. " +
            "Replace it with the real Firebase config file to enable Firebase Auth.",
    )
}

if (hasValidGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.chapterly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.chapterly"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "BOOKS_API_KEY",
            "\"${booksApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${backendBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"",
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    sourceSets {
        getByName("main").java.setSrcDirs(listOf("src/main/java/com/example/chapterly"))
        getByName("test").java.setSrcDirs(listOf("src/test/java/com/example/chapterly"))
        getByName("androidTest").java.setSrcDirs(listOf("src/androidTest/java/com/example/chapterly"))
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi.converter)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material.components)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    buildUponDefaultConfig = true
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

val jacocoExcludes =
    listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*_Factory.*",
        "**/*_HiltModules*.*",
        "**/*_MembersInjector*.*",
        "**/*_Impl*.*",
        "**/Hilt_*.*",
    )

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val projectBuildDir = layout.buildDirectory.asFile.get()
    val kotlinClasses =
        fileTree("$projectBuildDir/tmp/kotlin-classes/debug") {
            exclude(jacocoExcludes)
        }
    val javaClasses =
        fileTree("$projectBuildDir/intermediates/javac/debug/classes") {
            exclude(jacocoExcludes)
        }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(projectBuildDir) {
            include("jacoco/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    val projectBuildDir = layout.buildDirectory.asFile.get()
    val kotlinClasses =
        fileTree("$projectBuildDir/tmp/kotlin-classes/debug") {
            exclude(jacocoExcludes)
        }
    val javaClasses =
        fileTree("$projectBuildDir/intermediates/javac/debug/classes") {
            exclude(jacocoExcludes)
        }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(projectBuildDir) {
            include("jacoco/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        },
    )

    violationRules {
        rule {
            limit {
                minimum = "0.015".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn("detekt")
    dependsOn("ktlintCheck")
    dependsOn("jacocoTestCoverageVerification")
}
