
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.TimeSource

fun application(
  coroutineContext: CoroutineContext = EmptyCoroutineContext,
  loggerLevel: Level = Level.INFO,
  timeout: Duration = Duration.INFINITE,
  timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
  action: suspend ResourceCoroutineScope.() -> Unit,
) {
  val appTime = timeSource.markNow()
  KotlinLoggingConfiguration.logLevel = loggerLevel
  logger.debug { "Hello, world!" }
  CoroutineScope(SupervisorJob() + coroutineContext).launch {
    try {
      resourceScope {
        val coroutine =
          CoroutineScope(
            coroutineContext + SupervisorJob(coroutineContext[Job]),
          )
        val scope =
          object :
            ResourceCoroutineScope,
            CoroutineScope by coroutine,
            ResourceScope by this@resourceScope {}
        withTimeoutOrNull(timeout) {
          scope.action()
        } ?: run {
          withContext(NonCancellable) {
            logger.error {
              "Application timeout $timeout exceeded, shutting down..."
            }
            val shutdownTime = timeSource.markNow()
            coroutine.coroutineContext[Job]
              ?.cancelAndJoin()
            logger.debug { "Shut down in ${shutdownTime.elapsedNow()}" }
          }
        }
      }
    } catch (e: Throwable) {
      logger.error(
        e,
      ) { "Application crashed with exception, message: ${e.message}" }
      throw e
    } finally {
      logger.debug { "Application lasted for ${appTime.elapsedNow()}" }
      logger.debug { "Goodbye!" }
    }
  }
}

interface ResourceCoroutineScope :
  CoroutineScope,
  ResourceScope

private val logger = KotlinLogging.logger("Application")
