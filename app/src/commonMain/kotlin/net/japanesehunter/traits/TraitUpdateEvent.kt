package net.japanesehunter.traits

import net.japanesehunter.worldcreate.world.EventSink
import kotlin.time.Duration

/**
 * Represents a trait system update event.
 *
 * This event carries the entity registry and the delta time for the current update cycle.
 * Systems subscribed to this event type process entities and update their traits.
 *
 * @param registry the entity registry containing all entities and their traits.
 * @param dt the delta time since the last update.
 */
data class TraitUpdateEvent(
  val registry: EntityRegistry,
  val dt: Duration,
)

/**
 * Represents a consumer of trait update events.
 */
typealias TraitUpdateSink = EventSink<TraitUpdateEvent>
