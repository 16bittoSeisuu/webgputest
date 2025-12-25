package net.japanesehunter.traits.event

import net.japanesehunter.traits.Entity
import net.japanesehunter.traits.TraitKey
import net.japanesehunter.worldcreate.world.EventSink
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty1

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

  /**
   * Declares a required read-only trait binding from an entity property.
   *
   * The binding resolves when each event arrives. If the entity lacks the trait,
   * the event handler silently skips. Accessing the bound value outside [onEach]
   * throws [IllegalStateException].
   *
   * @param R the read-only view type
   * @param W the writable trait type
   * @param key the trait key
   * @return a property delegate providing the read-only view
   */
  fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R>
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
inline fun <Ev> buildEventSink(block: EventSinkBuilder<Ev>.() -> Unit): EventSink<Ev> {
  val builder = EventSinkBuilderImpl<Ev>()
  builder.block()
  return builder.build()
}

@PublishedApi
internal class EventSinkBuilderImpl<Ev> : EventSinkBuilder<Ev> {
  private var handler: ((Ev) -> Unit)? = null
  private val requiredBindings = mutableListOf<Binding<Ev>>()

  override fun onEach(handler: (Ev) -> Unit) {
    this.handler = handler
  }

  override fun <E : Entity, R : Any, W : Any> KProperty1<in Ev, E>.read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R> {
    val binding = ReadBinding(this, key)
    requiredBindings.add(binding)
    return ReadOnlyProperty { _, _ ->
      binding.resolvedReadView ?: error("Trait binding can only be accessed during onEach execution")
    }
  }

  fun build(): EventSink<Ev> {
    val h = handler
    val bindings = requiredBindings.toList()
    return EventSink { event ->
      if (!bindings.all { it.tryResolve(event) }) {
        return@EventSink
      }
      try {
        h?.invoke(event)
      } finally {
        bindings.forEach { it.clear() }
      }
    }
  }

  private interface Binding<Ev> {
    fun tryResolve(event: Ev): Boolean

    fun clear()
  }

  private class ReadBinding<Ev, R : Any, W : Any>(
    val property: KProperty1<in Ev, Entity>,
    val key: TraitKey<R, W>,
  ) : Binding<Ev> {
    var resolvedReadView: R? = null
      private set

    override fun tryResolve(event: Ev): Boolean {
      val entity = property.get(event)
      val trait = entity.get(key.writableType) ?: return false
      resolvedReadView = key.provideReadonlyView(trait)
      return true
    }

    override fun clear() {
      resolvedReadView = null
    }
  }
}
