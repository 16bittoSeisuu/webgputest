import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotest)
  alias(libs.plugins.ksp)
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
      testTask {
        useKarma {
          useChromeHeadless()
          useFirefoxHeadless()
        }
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
        implementation(libs.korge.foundation)
        implementation(libs.kotlinx.io.core)
        implementation(libs.kotlinx.io.bytestring)
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
    freeCompilerArgs.addAll(
      "-Xcontext-parameters",
      "-Xexpect-actual-classes",
      "-Xnested-type-aliases",
      "-Xreturn-value-checker=full",
    )
    allWarningsAsErrors = false
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
