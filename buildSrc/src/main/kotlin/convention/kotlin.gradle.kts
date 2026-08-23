package convention

import constants.JvmVersion
import dev.detekt.gradle.Detekt
import kotlin.math.roundToInt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.ncorti.ktfmt.gradle")
    id("dev.detekt")
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JvmVersion.JVM_TARGET_VERSION.toString()
    targetCompatibility = JvmVersion.JVM_TARGET_VERSION.toString()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(JvmVersion.JVM_TARGET_VERSION.toString())

        freeCompilerArgs.add("-opt-in=kotlin.io.path.ExperimentalPathApi")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.FlowPreview")
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }

    maxParallelForks =
        (Runtime.getRuntime().availableProcessors() / 2.0).roundToInt().coerceAtLeast(1)

    // Don't generate test reports, because currently we don't use them for anything
    reports.html.required = false
    reports.junitXml.required = false
}

configurations.configureEach {
    resolutionStrategy {
        force("org.hamcrest:hamcrest-core:3.0")
        force("org.hamcrest:hamcrest-library:3.0")
    }
}

detekt {
    config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml"))

    ignoredBuildTypes.add("release")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmVersion.JVM_TARGET_VERSION.toString()

    reports {
        checkstyle.required.set(false)
        html.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

tasks.register("detektAll") {
    description = "Runs Detekt for all sources"

    dependsOn(tasks.named("detektMain"), tasks.named("detektTest"))
}

ktfmt { kotlinLangStyle() }
