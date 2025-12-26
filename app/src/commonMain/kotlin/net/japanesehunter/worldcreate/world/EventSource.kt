package net.japanesehunter.worldcreate.world

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.japanesehunter.traits.EntityRegistry

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

/**
 * Represents a factory for creating event sinks with entity registry access.
 *
 * This factory receives an entity registry at invocation time and produces
 * an [EventSink] that can query entities during event processing.
 *
 * @param Ev the event type the produced sink handles
 */
fun interface QueryingEventSink<in Ev> {
  /**
   * Creates an event sink with access to the given registry.
   *
   * @param registry the entity registry to query during event processing.
   * @return the event sink. null: never returns null
   */
  operator fun invoke(registry: EntityRegistry): EventSink<Ev>
}

/**
 * Represents a pending subscription that requires an entity registry to complete.
 *
 * This handle is returned when subscribing a querying event sink to a source.
 * Call [invoke] with a registry to finalize the subscription and begin
 * receiving events.
 */
fun interface PendingEventSubscription {
  /**
   * Completes the subscription using the given registry.
   *
   * @param registry the entity registry for query execution.
   * @return the active subscription. null: never returns null
   */
  operator fun invoke(registry: EntityRegistry): EventSubscription
}

/**
 * Subscribes a querying sink to this source.
 *
 * The returned handle requires an entity registry to complete the subscription.
 * Invoke the handle with a registry to begin receiving events.
 *
 * @param sink the querying event sink factory.
 * @return a pending subscription awaiting registry. null: never returns null
 */
fun <T> EventSource<T>.subscribe(sink: QueryingEventSink<T>): PendingEventSubscription =
  PendingEventSubscription { registry ->
    subscribe(sink(registry))
  }

/**
 * Converts this event source into a cold [Flow].
 *
 * The returned flow subscribes on collection start and cancels the subscription when the collector stops.
 * Events emitted after cancellation are ignored.
 *
 * @return A flow that emits every event from this source until collection is cancelled.
 */
fun <T> EventSource<T>.asFlow(): Flow<T> =
  callbackFlow {
    val subscription = subscribe { event -> trySend(event) }
    awaitClose { subscription.close() }
  }
