package net.japanesehunter.worldcreate.input

import net.japanesehunter.worldcreate.world.EventSource

/**
 * Provides pointer lock state management and event delivery.
 *
 * Implementations emit pointer lock state changes through an event source and expose the current
 * lock state for polling. The pointer lock feature captures the pointer for exclusive use,
 * typically to enable first-person camera control.
 *
 * Implementations are not required to be thread-safe. Callers must access the instance from the
 * same thread that processes platform events.
 */
interface PointerLock {
  /**
   * Current pointer lock state.
   *
   * Returns true when the pointer is locked to this context.
   */
  val isPointerLocked: Boolean

  /**
   * Emits pointer lock state changes.
   *
   * @return an event source that delivers pointer lock events to subscribed sinks.
   */
  fun pointerLockEvents(): EventSource<PointerLockEvent>

  /**
   * Requests pointer lock acquisition.
   *
   * The request may fail silently if the platform denies it or if user interaction requirements
   * are not satisfied.
   */
  fun requestPointerLock()

  /**
   * Releases pointer lock if currently held.
   *
   * Has no effect when the pointer is not locked.
   */
  fun exitPointerLock()
}
