package convention

import dev.detekt.gradle.Detekt
import kotlin.math.roundToInt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.ncorti.ktfmt.gradle")
    id("dev.detekt")
}

/**
 * Configurations common for all Kotlin projects. Note that Kotlin plugin needs to be applied
 * separately to use the correct version ("jvm" or "android")
 */
val jvmTargetVersion = "17"

project.extra["jvmTarget"] = jvmTargetVersion

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = jvmTargetVersion
    targetCompatibility = jvmTargetVersion
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmTargetVersion)

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
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = jvmTargetVersion

    reports {
        checkstyle.required.set(false)
        html.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

ktfmt { kotlinLangStyle() }
