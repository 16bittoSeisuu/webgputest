package net.japanesehunter.worldcreate.world

import kotlin.time.Duration

/**
 * Represents a source of tick events for game loop updates.
 *
 * Implementations must guarantee that subscribed handlers are called sequentially from a single thread.
 * The execution order between different handlers is undefined.
 *
 * Implementations are required to be thread-safe regarding subscription management.
 */
interface TickSource {
  /**
   * Subscribes to tick events.
   *
   * @param handler The callback to invoke on each tick.
   * @return A subscription handle to cancel the subscription.
   */
  fun subscribe(handler: TickHandler): TickSubscription
}

/**
 * Represents logic to be executed on every game tick.
 */
fun interface TickHandler {
  /**
   * Performs the logic for a single tick.
   *
   * @param dt The time delta to simulate for this tick.
   *   This represents the simulation step size, not necessarily the wall-clock time elapsed since the last frame.
   */
  fun onTick(dt: Duration)
}

/**
 * Represents a cancellable subscription to a tick source.
 */
fun interface TickSubscription : AutoCloseable {
  /**
   * Cancels the subscription.
   * After this method returns, the handler will no longer receive tick events.
   * Calling this multiple times has no effect.
   */
  override fun close()
}
