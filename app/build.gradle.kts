plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
// Debug always uses Google's test inventory, even when production properties are supplied.
val productionAppId = providers.gradleProperty("ADMOB_APP_ID").orNull
val productionBannerId = providers.gradleProperty("ADMOB_BANNER_ID").orNull
val productionAdsEnabled = providers.gradleProperty("ADS_ENABLED").orNull == "true"
val sampleAppId = "ca-app-pub-3940256099942544~3347511713"
val sampleBannerId = "ca-app-pub-3940256099942544/9214589741"
if (productionAdsEnabled) {
    require(productionAppId?.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")) == true) { "Set a valid ADMOB_APP_ID" }
    require(productionBannerId?.matches(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}")) == true) { "Set a valid ADMOB_BANNER_ID" }
    require(!productionAppId!!.startsWith("ca-app-pub-3940256099942544") && !productionBannerId!!.startsWith("ca-app-pub-3940256099942544")) { "Production ads require your own IDs" }
}
android {
    namespace = "ru.pdd.academy"
    compileSdk = 35
    defaultConfig {
        applicationId = "ru.pdd.academy"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release_config") {
            keyAlias = "pddacademy"
            keyPassword = "POLkjm123"
            storeFile = file("../accademy-keystore.keystore")
            storePassword = "POLkjm123"
        }
    }
    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = sampleAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$sampleBannerId\"")
        }
        release {
            manifestPlaceholders["admobAppId"] = if (productionAdsEnabled) productionAppId!! else sampleAppId
            buildConfigField("boolean", "ADS_ENABLED", productionAdsEnabled.toString())
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${if (productionAdsEnabled) productionBannerId!! else sampleBannerId}\"")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            listOf("https.proxyHost", "https.proxyPort", "http.proxyHost", "http.proxyPort", "javax.net.ssl.trustStore").forEach { name ->
                System.getProperty(name)?.let { value -> it.systemProperty(name, value) }
            }
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
