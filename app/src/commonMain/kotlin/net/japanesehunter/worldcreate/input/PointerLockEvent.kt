package net.japanesehunter.worldcreate.input

/**
 * Represents a change in pointer lock state.
 *
 * @param locked true if the pointer is now locked, false if released.
 */
data class PointerLockEvent(
  val locked: Boolean,
)
