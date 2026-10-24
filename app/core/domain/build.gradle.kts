plugins {
    id("convention.kotlin-library")

    `java-test-fixtures`
}

dependencies {
    api(project(":libs:geography"))

    testImplementation(libs.junit)
}
