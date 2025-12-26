package net.japanesehunter.worldcreate.world

import net.japanesehunter.traits.EntityRegistry

/**
 * Represents a source that allows interception of events before they reach final subscribers.
 *
 * Interception forms a chain where each interceptor can observe, modify, or cancel events.
 * The order of interceptor invocation within a single source is unspecified, but all
 * interceptors at a given depth are invoked before any child interceptors.
 *
 * @param R the read-only event type for final subscribers
 * @param W the mutable event type for interception
 */
interface InterceptableEventSource<out R, out W> : EventSource<R> {
  /**
   * Registers an interceptor to observe or modify events.
   *
   * The interceptor receives events before downstream subscribers. Multiple interceptors
   * at the same level are invoked in an unspecified order. Interceptors can cancel
   * events to prevent downstream processing.
   *
   * @param interceptor the interceptor to register.
   * @return an interception handle that acts as a child source. null: never returns null
   */
  fun intercept(interceptor: EventInterceptor<W>): InterceptableEventInterception<W>
}

/**
 * Represents a consumer that intercepts events with cancellation support.
 *
 * When invoked, the interceptor receives a [CancellableScope] that allows calling
 * [CancellableScope.cancel] to prevent downstream processing of the current event.
 *
 * @param Ev the event type to intercept
 */
fun interface EventInterceptor<in Ev> {
  /**
   * Processes an intercepted event with cancellation capability.
   *
   * @param event the event to process.
   */
  fun CancellableScope.onIntercept(event: Ev)
}

/**
 * Represents the result of a cancellable operation.
 *
 * Instances report whether the operation was canceled by any participant.
 */
fun interface Cancellation {
  /**
   * Returns true if the operation was canceled.
   *
   * @return true if canceled.
   */
  fun isCanceled(): Boolean
}

/**
 * Represents a scope that allows canceling the current event processing.
 *
 * Call [cancel] to prevent downstream subscribers from receiving the event.
 * Cancellation affects only the current event, not the subscription itself.
 */
fun interface CancellableScope {
  /**
   * Cancels the current event processing.
   *
   * After calling this method, downstream subscribers will not receive this event.
   * Interceptors at the same or higher level may still be invoked.
   */
  fun cancel()
}

/**
 * Represents an active interception that can receive further interceptions and subscriptions.
 *
 * Closing this interception removes it from the parent source. Child interceptions and
 * subscriptions remain active but will no longer receive events from this interception.
 *
 * @param Ev the event type for further interceptions
 */
interface InterceptableEventInterception<out Ev> :
  InterceptableEventSource<Ev, Ev>,
  AutoCloseable {
  override fun close()
}

/**
 * Represents a consumer of events from an [InterceptableEventSource] that can cancel delivery.
 *
 * @param Ev the event type to consume
 */
fun interface CancellableEventSink<in Ev> {
  /**
   * Processes an event and returns the cancellation result.
   *
   * @param event the event to process.
   * @return the cancellation result. null: never returns null
   */
  fun onEvent(event: Ev): Cancellation
}

/**
 * Represents a factory for creating event interceptors with entity registry access.
 *
 * @param Ev the event type the produced interceptor handles
 */
fun interface QueryingEventInterceptor<in Ev> {
  /**
   * Creates an event interceptor with access to the given registry.
   *
   * @param registry the entity registry to query during event processing.
   * @return the event interceptor. null: never returns null
   */
  operator fun invoke(registry: EntityRegistry): EventInterceptor<Ev>
}

/**
 * Represents a pending interception that requires an entity registry to complete.
 */
fun interface PendingEventInterception {
  /**
   * Completes the interception using the given registry.
   *
   * @param registry the entity registry for query execution.
   * @return the active interception. null: never returns null
   */
  operator fun invoke(registry: EntityRegistry): InterceptableEventInterception<*>
}

/**
 * Intercepts events from this source using a querying interceptor.
 *
 * The returned handle requires an entity registry to complete the interception.
 *
 * @param interceptor the querying event interceptor factory.
 * @return a pending interception awaiting registry. null: never returns null
 */
fun <R, W> InterceptableEventSource<R, W>.intercept(interceptor: QueryingEventInterceptor<W>): PendingEventInterception =
  PendingEventInterception { registry ->
    intercept(interceptor(registry))
  }

// region Factory

/**
 * Creates an interceptable event source with a writable-to-readonly transformation.
 *
 * The returned pair contains the source for subscribers and interceptors, and a sink
 * for emitting events. Events emitted through the sink flow through the interception
 * chain, and final subscribers receive the read-only view.
 *
 * @param R the read-only event type for final subscribers
 * @param W the mutable event type for interception
 * @param toReadonlyView converts the writable event to a read-only view for final subscribers.
 * @return a pair of the interceptable source and the sink for emitting events.
 */
fun <R, W> createInterceptableEventSource(toReadonlyView: (W) -> R): Pair<InterceptableEventSource<R, W>, CancellableEventSink<W>> {
  class Node<T> : InterceptableEventSource<T, W> {
    val children = mutableListOf<Node<W>>()
    val interceptors = mutableListOf<EventInterceptor<W>>()
    val sinks = mutableListOf<EventSink<T>>()

    override fun intercept(interceptor: EventInterceptor<W>): InterceptableEventInterception<W> {
      interceptors += interceptor
      val child = Node<W>()
      children += child
      return object : InterceptableEventInterception<W>, InterceptableEventSource<W, W> by child {
        override fun close() {
          interceptors -= interceptor
          children -= child
        }
      }
    }

    override fun subscribe(sink: EventSink<T>): EventSubscription {
      sinks += sink
      return EventSubscription { sinks -= sink }
    }
  }

  val root = Node<R>()

  val emitter =
    CancellableEventSink<W> { event ->
      fun <T> invokeRecursive(node: Node<T>): Cancellation {
        var canceledInChildren = false
        for (child in node.children) {
          val result = invokeRecursive(child)
          if (result.isCanceled()) {
            canceledInChildren = true
          }
        }

        var canceled = false
        if (!canceledInChildren) {
          val scope = CancellableScope { canceled = true }
          for (interceptor in node.interceptors) {
            with(interceptor) { scope.onIntercept(event) }
          }
        }

        for (child in node.children) {
          for (sink in child.sinks) {
            sink.onEvent(event)
          }
        }

        return Cancellation { canceled }
      }

      val canceled = invokeRecursive(root).isCanceled()
      if (!canceled) {
        val readonlyEvent = toReadonlyView(event)
        for (sink in root.sinks) {
          sink.onEvent(readonlyEvent)
        }
      }
      Cancellation { canceled }
    }

  return root to emitter
}

/**
 * Creates an interceptable event source where the writable and read-only types are the same.
 *
 * @param R the event type for both interception and subscription
 * @return a pair of the interceptable source and the sink for emitting events.
 */
@Suppress("ktlint:standard:function-naming")
fun <R> createInterceptableEventSource(): Pair<InterceptableEventSource<R, R>, CancellableEventSink<R>> =
  createInterceptableEventSource { it }

// endregion
