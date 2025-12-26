package net.japanesehunter.traits

import net.japanesehunter.worldcreate.world.EventSink
import kotlin.time.Duration

/**
 * Represents a trait system tick event.
 *
 * This event carries the delta time for the current tick cycle. Systems
 * subscribed to this event type process entities and update their traits
 * based on the elapsed time.
 */
interface TraitTickEvent {
  /**
   * The delta time since the last tick.
   *
   * range: 0 <= dt < Infinity
   */
  val dt: Duration
}

/**
 * Mutable variant of [TraitTickEvent] that allows interceptors to modify the delta time.
 *
 * Interceptors in the event pipeline can adjust [dt] before the event reaches
 * subscribers. The modified delta time must remain non-negative and finite.
 * Setting [dt] to an invalid value throws [IllegalArgumentException].
 *
 * @param dt the initial delta time.
 *   range: 0 <= dt < Infinity
 * @throws IllegalArgumentException if dt is negative or not finite
 */
class MutableTraitTickEvent(
  dt: Duration,
) : TraitTickEvent {
  override var dt: Duration = dt
    set(value) {
      require(!value.isNegative()) { "dt must be non-negative: $value" }
      require(value.isFinite()) { "dt must be finite: $value" }
      field = value
    }

  init {
    require(!dt.isNegative()) { "dt must be non-negative: $dt" }
    require(dt.isFinite()) { "dt must be finite: $dt" }
  }

  override fun toString(): String = "MutableTraitTickEvent(dt=$dt)"
}

/**
 * Represents a consumer of trait tick events.
 */
typealias TraitTickSink = EventSink<TraitTickEvent>
