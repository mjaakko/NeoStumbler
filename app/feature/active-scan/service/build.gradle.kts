plugins { id("convention.kotlin-library") }

dependencies {
    api(project(":app:core:domain"))
    api(project(":app:core:scan-common"))

    api(project(":app:data:reports:api"))
    api(project(":app:data:airpressure:api"))
    api(project(":app:data:battery:api"))
    api(project(":app:data:thermal:api"))
    api(project(":app:data:emitter:api"))
    api(project(":app:data:location:api"))
    api(project(":app:data:movement:api"))
    api(project(":app:data:settings:api"))

    implementation(libs.androidx.collection)

    testImplementation(platform(libs.kotlinx.coroutinesBom))
    testImplementation(libs.kotlinx.coroutinesTest)

    testImplementation(libs.junit)

    testImplementation(libs.mockitoKotlin)
}
