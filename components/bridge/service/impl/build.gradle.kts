plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.squareup.anvil")
    id("kotlin-kapt")
}
apply<com.flipperdevices.gradle.ConfigurationPlugin>()

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":components:core:di"))
    implementation(project(":components:core:log"))
    implementation(project(":components:core:preference"))

    implementation(project(":components:bridge:api"))
    implementation(project(":components:bridge:provider"))
    implementation(project(":components:bridge:protobuf"))
    implementation(project(":components:bridge:service:api"))
    implementation(project(":components:bridge:impl"))

    implementation(Libs.ANNOTATIONS)
    implementation(Libs.APPCOMPAT)

    implementation(Libs.KOTLIN_COROUTINES)
    implementation(Libs.LIFECYCLE_RUNTIME_KTX)
    implementation(Libs.LIFECYCLE_VIEWMODEL_KTX)
    implementation(Libs.LIFECYCLE_SERVICE)
    kapt(Libs.LIFECYCLE_KAPT)

    implementation(Libs.NORDIC_BLE)
    implementation(Libs.NORDIC_BLE_KTX)
    implementation(Libs.NORDIC_BLE_COMMON)
    implementation(Libs.NORDIC_BLE_SCAN)

    implementation(Libs.DAGGER)
    kapt(Libs.DAGGER_COMPILER)

    testImplementation(project(":components:core:test"))
    testImplementation(TestingLib.JUNIT)
    testImplementation(TestingLib.MOCKITO)
    testImplementation(TestingLib.ANDROIDX_TEST_EXT_JUNIT)
    testImplementation(TestingLib.ROBOELECTRIC)
    testImplementation(TestingLib.LIFECYCLE)
}
