package net.japanesehunter.traits

import net.japanesehunter.worldcreate.world.EventSubscription
import net.japanesehunter.worldcreate.world.TickSource

/**
 * Subscribes a trait update sink to this tick source.
 *
 * Each tick event from the source is transformed into a [TraitUpdateEvent] that includes
 * the provided registry and the tick duration. The trait update sink receives these events
 * and can process entities within the registry.
 *
 * @param registry the entity registry to include in each trait update event.
 * @param sink the trait update sink to receive events.
 * @return the subscription handle.
 *   null: never returns null
 */
context(registry: EntityRegistry)
fun TickSource.subscribe(sink: TraitUpdateSink): EventSubscription =
  subscribe { dt ->
    sink.onEvent(TraitUpdateEvent(registry, dt))
  }
