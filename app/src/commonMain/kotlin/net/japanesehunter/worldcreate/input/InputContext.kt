package net.japanesehunter.worldcreate.input

import net.japanesehunter.worldcreate.world.EventSource

/**
 * Provides access to user input events and current input state.
 *
 * Implementations emit keyboard and mouse events through an event source and expose the current
 * pressed key state for polling-based input handling. Event delivery order matches the order in
 * which the underlying platform delivers them.
 *
 * Implementations are not required to be thread-safe. Callers must access the context from the
 * same thread that processes platform input events.
 */
interface InputContext {
  /**
   * Emits keyboard and mouse input events.
   *
   * @return an event source that delivers input events to subscribed sinks.
   */
  fun events(): EventSource<InputEvent>

  /**
   * Checks whether the specified key is currently pressed.
   *
   * @param code the physical key code following the KeyboardEvent.code convention.
   * @return true if the key is pressed.
   */
  fun isKeyDown(code: String): Boolean

  /**
   * Returns a snapshot of all currently pressed key codes.
   *
   * The returned set is a copy and does not reflect subsequent state changes.
   *
   * @return an immutable set of pressed key codes.
   */
  fun pressedKeys(): Set<String>
}
