@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

repositories {
  mavenCentral()
}

kotlin {
  js {
    browser {
      commonWebpackConfig {
        outputFileName = "main.js"
      }
    }
    binaries.executable()
  }
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlin.logging)
        api(libs.kotlinx.coroutines.core)
        api(libs.arrow.core)
        api(libs.arrow.fx.coroutines)
        api(libs.arrow.resilience)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.kotest.framework.engine)
        implementation(libs.kotest.assertions)
      }
    }
  }

  compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
  }
}

tasks.withType<Test>().configureEach {
  testLogging {
    showStandardStreams = true

    events =
      setOf(
        TestLogEvent.PASSED,
        TestLogEvent.SKIPPED,
        TestLogEvent.FAILED,
        TestLogEvent.STANDARD_OUT,
        TestLogEvent.STANDARD_ERROR,
      )

    exceptionFormat = TestExceptionFormat.FULL
    showExceptions = true
    showCauses = false
    showStackTraces = true
  }
}
