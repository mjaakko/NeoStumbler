package convention

plugins {
    `java-library`
    id("convention.kotlin")
    id("org.jetbrains.kotlin.jvm")
}

tasks.register("assembleAll") { dependsOn(tasks.named("assemble"), tasks.named("testClasses")) }

tasks.register("unitTest") { dependsOn(tasks.named("test")) }
