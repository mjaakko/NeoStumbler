plugins { id("convention.kotlin-library") }

dependencies {
    dependencies {
        api(project(":app:data:movement:api"))
        api(project(":app:data:location:api"))

        testImplementation(libs.junit)
        testImplementation(platform(libs.kotlinx.coroutinesBom))
        testImplementation(libs.kotlinx.coroutinesTest)
    }
}
