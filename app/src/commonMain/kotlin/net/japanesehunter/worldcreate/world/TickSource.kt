package net.japanesehunter.worldcreate.world

import kotlin.time.Duration

/**
 * Represents a source of periodic tick events for simulation updates.
 *
 * Each emitted event carries the duration of the tick step.
 * Implementations guarantee that sinks receive events sequentially from a single thread.
 *
 * Implementations are required to be thread-safe for subscription management.
 */
typealias TickSource = EventSource<Duration>

/**
 * Represents a consumer of tick events from a [TickSource].
 */
typealias TickSink = EventSink<Duration>
