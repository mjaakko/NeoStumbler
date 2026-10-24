plugins {
    id("convention.kotlin-library")

    `java-test-fixtures`
}

dependencies {
    api(platform(libs.kotlinx.coroutinesBom))
    api(libs.kotlinx.coroutinesCore)
}
