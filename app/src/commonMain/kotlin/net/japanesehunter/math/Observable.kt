package net.japanesehunter.math

/**
 * An entity that can be monitored for state changes.
 * Implementations provide a way to create an [ObserveTicket],
 * which allows clients to poll whether the underlying value has changed.
 *
 * Each call to [observe] returns an independent ticket, and
 * changes to the observable will be reported to all active tickets.
 *
 * @author Int16
 */
interface Observable {
  /**
   * Creates a new [ObserveTicket] bound to this observable.
   * The returned ticket will track future changes until this
   * [Observable] is discarded.
   *
   * @return A ticket that reports whether this observable has changed.
   */
  fun observe(): ObserveTicket
}

/**
 * Represents an object for checking whether a value has changed (Dirtiness).
 * To prevent memory leaks, the original object is weakly referenced,
 * and if it is discarded, [isDirty] will **always** return `false`.
 * Make sure to check [isActive] before relying on this ticket.
 *
 * @author Int16
 */
interface ObserveTicket {
  /**
   * Returns `true` if the [Observable]'s value has changed since
   * the last [reset], and `false` otherwise.
   * If the original [Observable] has been discarded, this will always return `false`.
   * Once this becomes `true`, it will remain `true` until [reset] is called.
   */
  val isDirty: Boolean

  /**
   * Returns `true` if the original [Observable] is still alive,
   * and `false` if it has been discarded.
   *
   * @return Whether the original [Observable] is still alive.
   */
  val isActive: Boolean

  /**
   * Set the value of `isDirty` to false.
   * This operation is performed atomically with respect to both
   * the original [Observable] and this [ObserveTicket].
   */
  fun reset()

  /**
   * Get the value of [isDirty], then set [isDirty] to `false`.
   * This operation is performed atomically with respect to both
   * the original [Observable] and this [ObserveTicket].
   *
   * @return Whether the value of [Observable] has changed since the last [reset].
   */
  fun fetchAndReset(): Boolean
}
