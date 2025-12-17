package net.japanesehunter.worldcreate.world

/**
 * Represents a producer of events delivered to subscribed sinks.
 *
 * Implementations guarantee that subscribed sinks receive events sequentially from a single thread.
 * The invocation order among different sinks is unspecified.
 *
 * Implementations are required to be thread-safe for subscription management.
 */
interface EventSource<out T> {
  /**
   * Subscribes a sink to this source.
   *
   * @param sink The consumer invoked for each emitted event.
   * @return A subscription handle to cancel the subscription.
   */
  fun subscribe(sink: EventSink<T>): EventSubscription
}

/**
 * Represents a consumer of events emitted by an [EventSource].
 */
fun interface EventSink<in T> {
  /**
   * Processes a single event.
   *
   * @param event The event payload.
   */
  fun onEvent(event: T)
}

/**
 * Represents a cancellable subscription between an [EventSource] and an [EventSink].
 */
fun interface EventSubscription : AutoCloseable {
  /**
   * Cancels the subscription.
   * After this method returns, the sink will no longer receive events.
   * Calling this multiple times has no effect.
   */
  override fun close()
}
