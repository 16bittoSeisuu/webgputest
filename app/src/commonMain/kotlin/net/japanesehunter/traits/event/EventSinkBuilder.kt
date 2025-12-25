package net.japanesehunter.traits.event

import net.japanesehunter.worldcreate.world.EventSink

/**
 * Builds an [EventSink] with declarative trait bindings resolved per event.
 *
 * The builder allows declaring trait requirements from entities referenced in
 * events. Bindings are resolved when each event arrives, not at declaration time.
 * If all required bindings resolve successfully, [onEach] executes. Otherwise,
 * the handler silently skips without throwing exceptions.
 *
 * Accessing bound trait values outside [onEach] throws [IllegalStateException].
 *
 * Implementations are not thread-safe. Build the sink on a single thread, then
 * use the resulting [EventSink] according to its own thread-safety contract.
 *
 * @param Ev the event type this builder handles
 */
interface EventSinkBuilder<out Ev> {
  /**
   * Registers the handler invoked for each event when all bindings resolve.
   *
   * The handler receives the event instance. Bound trait values are accessible
   * only within this block.
   *
   * @param handler the event processing logic
   */
  fun onEach(handler: (Ev) -> Unit)
}

/**
 * Builds an [EventSink] with declarative trait binding resolution.
 *
 * Events flow through the returned sink. For each event, the builder resolves
 * all declared bindings. If every required binding succeeds, the [onEach]
 * handler executes. If any required binding fails, the event is silently skipped.
 *
 * @param Ev the event type
 * @param block the builder configuration
 * @return an event sink that processes events according to the configured bindings
 */
fun <Ev> buildEventSink(block: EventSinkBuilder<Ev>.() -> Unit): EventSink<Ev> {
  val builder = EventSinkBuilderImpl<Ev>()
  builder.block()
  return builder.build()
}

internal class EventSinkBuilderImpl<Ev> : EventSinkBuilder<Ev> {
  private var handler: ((Ev) -> Unit)? = null

  override fun onEach(handler: (Ev) -> Unit) {
    this.handler = handler
  }

  fun build(): EventSink<Ev> {
    val h = handler
    return EventSink { event ->
      h?.invoke(event)
    }
  }
}
