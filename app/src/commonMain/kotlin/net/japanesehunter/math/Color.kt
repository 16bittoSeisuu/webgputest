@file:OptIn(ExperimentalAtomicApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// region interfaces

/**
 * Represents an RGBA color using [Proportion] channels.
 * Channels are always clamped to [0.0, 1.0] by [Proportion].
 */
sealed interface Color {
  val r: Proportion
  val g: Proportion
  val b: Proportion
  val a: Proportion

  operator fun component1() =
    r

  operator fun component2() =
    g

  operator fun component3() =
    b

  operator fun component4() =
    a

  override fun toString(): String

  override fun equals(
    other: Any?,
  ): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable RGBA color. Users cannot implement this interface.
 */
sealed interface ImmutableColor : Color

/**
 * Mutable RGBA color. Changes can be monitored via [StateFlow] and [Observable.observe].
 */
interface MutableColor :
  Color,
  Observable {
  override var r: Proportion
  override var g: Proportion
  override var b: Proportion
  override var a: Proportion

  val rFlow: StateFlow<Proportion>
  val gFlow: StateFlow<Proportion>
  val bFlow: StateFlow<Proportion>
  val aFlow: StateFlow<Proportion>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(
    action: MutableColor.() -> Unit,
  ) =
    action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region extensions

inline val Color.isTransparent: Boolean
  get() = r.isZero && g.isZero && b.isZero && a.isZero

inline val Color.isBlack: Boolean
  get() = r.isZero && g.isZero && b.isZero && a.isFull

inline val Color.isWhite: Boolean
  get() = r.isFull && g.isFull && b.isFull && a.isFull

inline val Color.isRed: Boolean
  get() = r.isFull && g.isZero && b.isZero && a.isFull

inline val Color.isGreen: Boolean
  get() = r.isZero && g.isFull && b.isZero && a.isFull

inline val Color.isBlue: Boolean
  get() = r.isZero && g.isZero && b.isFull && a.isFull

inline val Color.isCyan: Boolean
  get() = r.isZero && g.isFull && b.isFull && a.isFull

inline val Color.isMagenta: Boolean
  get() = r.isFull && g.isZero && b.isFull && a.isFull

inline val Color.isYellow: Boolean
  get() = r.isFull && g.isFull && b.isZero && a.isFull

// endregion

// region constants

/**
 * The fully transparent color (0, 0, 0, 0).
 */
val Color.Companion.transparent: ImmutableColor get() = COLOR_TRANSPARENT

/**
 * The solid black color (0, 0, 0, 1).
 */
val Color.Companion.black: ImmutableColor get() = COLOR_BLACK

/**
 * The solid white color (1, 1, 1, 1).
 */
val Color.Companion.white: ImmutableColor get() = COLOR_WHITE

/**
 * The solid red color (1, 0, 0, 1).
 */
val Color.Companion.red: ImmutableColor get() = COLOR_RED

/**
 * The solid green color (0, 1, 0, 1).
 */
val Color.Companion.green: ImmutableColor get() = COLOR_GREEN

/**
 * The solid blue color (0, 0, 1, 1).
 */
val Color.Companion.blue: ImmutableColor get() = COLOR_BLUE

/**
 * The solid cyan color (0, 1, 1, 1).
 */
val Color.Companion.cyan: ImmutableColor get() = COLOR_CYAN

/**
 * The solid magenta color (1, 0, 1, 1).
 */
val Color.Companion.magenta: ImmutableColor get() = COLOR_MAGENTA

/**
 * The solid yellow color (1, 1, 0, 1).
 */
val Color.Companion.yellow: ImmutableColor get() = COLOR_YELLOW

// endregion

// region factory functions

/**
 * Creates a [Color] by specifying RGBA channels.
 * You can treat it as a [MutableColor] only at creation via [mutator],
 * after which it becomes an [ImmutableColor].
 *
 * @param r Red channel.
 * @param g Green channel.
 * @param b Blue channel.
 * @param a Alpha channel.
 * @param mutator A scope to mutate the color before freezing. If null, nothing is done.
 * @return The frozen [ImmutableColor].
 */
@Suppress("FunctionName")
fun Color(
  r: Proportion = Proportion.ZERO,
  g: Proportion = Proportion.ZERO,
  b: Proportion = Proportion.ZERO,
  a: Proportion = Proportion.ONE,
  mutator: (MutableColor.() -> Unit)? = null,
): ImmutableColor {
  val base =
    createColorImmutable(
      r = r,
      g = g,
      b = b,
      a = a,
    )
  if (mutator == null) {
    return base
  }
  val impl = ImmutableColorImpl(base.r, base.g, base.b, base.a)
  val mutableWrapper = ColorMutableWrapper(impl)
  mutator(mutableWrapper)
  return impl
}

/**
 * Creates an [ImmutableColor] by copying an existing one.
 * If the source is already immutable and [mutator] is null, it is returned as-is.
 */
inline fun Color.Companion.copyOf(
  copyFrom: Color,
  noinline mutator: (MutableColor.() -> Unit)? = null,
): ImmutableColor =
  if (copyFrom is ImmutableColor && mutator == null) {
    copyFrom
  } else {
    Color(
      r = copyFrom.r,
      g = copyFrom.g,
      b = copyFrom.b,
      a = copyFrom.a,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableColor] by specifying RGBA channels.
 */
fun MutableColor(
  r: Proportion = Proportion.ZERO,
  g: Proportion = Proportion.ZERO,
  b: Proportion = Proportion.ZERO,
  a: Proportion = Proportion.ONE,
): MutableColor =
  MutableColorImpl(r, g, b, a)

/**
 * Creates a [MutableColor] by copying an existing [Color].
 */
fun MutableColor.Companion.copyOf(
  copyFrom: Color,
): MutableColor =
  MutableColor(copyFrom.r, copyFrom.g, copyFrom.b, copyFrom.a)

// endregion

// region implementations

private val COLOR_TRANSPARENT: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ZERO,
  )
private val COLOR_BLACK: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ONE,
  )
private val COLOR_WHITE: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ONE,
    Proportion.ONE,
    Proportion.ONE,
    Proportion.ONE,
  )
private val COLOR_RED: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ONE,
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ONE,
  )
private val COLOR_GREEN: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ZERO,
    Proportion.ONE,
    Proportion.ZERO,
    Proportion.ONE,
  )
private val COLOR_BLUE: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ZERO,
    Proportion.ZERO,
    Proportion.ONE,
    Proportion.ONE,
  )
private val COLOR_CYAN: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ZERO,
    Proportion.ONE,
    Proportion.ONE,
    Proportion.ONE,
  )
private val COLOR_MAGENTA: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ONE,
    Proportion.ZERO,
    Proportion.ONE,
    Proportion.ONE,
  )
private val COLOR_YELLOW: ImmutableColor =
  ImmutableColorImpl(
    Proportion.ONE,
    Proportion.ONE,
    Proportion.ZERO,
    Proportion.ONE,
  )

private data class ImmutableColorImpl(
  override var r: Proportion,
  override var g: Proportion,
  override var b: Proportion,
  override var a: Proportion,
) : ImmutableColor {
  override fun toString(): String =
    "Color(r=$r, g=$g, b=$b, a=$a)"

  override fun equals(
    other: Any?,
  ): Boolean =
    when {
      this === other -> true
      other !is Color -> false
      else -> channelsEqual(this, other)
    }

  override fun hashCode(): Int =
    channelsHash(r, g, b, a)
}

private value class ColorMutableWrapper(
  private val impl: ImmutableColorImpl,
) : MutableColor {
  override var r: Proportion
    get() = impl.r
    set(value) {
      impl.r = value
    }
  override var g: Proportion
    get() = impl.g
    set(value) {
      impl.g = value
    }
  override var b: Proportion
    get() = impl.b
    set(value) {
      impl.b = value
    }
  override var a: Proportion
    get() = impl.a
    set(value) {
      impl.a = value
    }

  override val rFlow: StateFlow<Proportion>
    get() = throw UnsupportedOperationException()
  override val gFlow: StateFlow<Proportion>
    get() = throw UnsupportedOperationException()
  override val bFlow: StateFlow<Proportion>
    get() = throw UnsupportedOperationException()
  override val aFlow: StateFlow<Proportion>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket =
    throw UnsupportedOperationException()

  override fun toString(): String =
    "Color(r=$r, g=$g, b=$b, a=$a)"
}

private class MutableColorImpl(
  r: Proportion,
  g: Proportion,
  b: Proportion,
  a: Proportion,
) : MutableColor {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _rFlow: MutableStateFlow<Proportion> = MutableStateFlow(r)
  private val _gFlow: MutableStateFlow<Proportion> = MutableStateFlow(g)
  private val _bFlow: MutableStateFlow<Proportion> = MutableStateFlow(b)
  private val _aFlow: MutableStateFlow<Proportion> = MutableStateFlow(a)

  override var r: Proportion
    get() = lock.withLock { _rFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _rFlow.value = value
      }
    }
  override var g: Proportion
    get() = lock.withLock { _gFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _gFlow.value = value
      }
    }
  override var b: Proportion
    get() = lock.withLock { _bFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _bFlow.value = value
      }
    }
  override var a: Proportion
    get() = lock.withLock { _aFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _aFlow.value = value
      }
    }

  override val rFlow: StateFlow<Proportion> get() = _rFlow.asStateFlow()
  override val gFlow: StateFlow<Proportion> get() = _gFlow.asStateFlow()
  override val bFlow: StateFlow<Proportion> get() = _bFlow.asStateFlow()
  override val aFlow: StateFlow<Proportion> get() = _aFlow.asStateFlow()

  override fun mutate(
    action: MutableColor.() -> Unit,
  ) {
    lock.withLock { action(this) }
  }

  override fun toString(): String =
    "Color(r=$r, g=$g, b=$b, a=$a)"

  override fun equals(
    other: Any?,
  ): Boolean =
    when {
      this === other -> true
      other !is Color -> false
      else -> channelsEqual(this, other)
    }

  override fun hashCode(): Int =
    channelsHash(r, g, b, a)

  override fun observe(): ObserveTicket =
    Ticket(this)

  private class Ticket(original: MutableColorImpl) : ObserveTicket {
    private val weakOriginal by WeakProperty(original)
    private val knownGeneration: Int =
      original.lock.withLock {
        original.generation
      }

    override val isDirty: Boolean
      get() =
        weakOriginal?.let {
          it.lock
            .withLock { it.generation != knownGeneration }
        } ?: false

    override val isActive: Boolean
      get() = weakOriginal != null

    override fun reset() {
      weakOriginal?.let {
        it.lock.withLock {
          it.generation = knownGeneration
        }
      }
    }

    override fun fetchAndReset(): Boolean =
      weakOriginal?.let {
        it.lock.withLock {
          val dirty = it.generation != knownGeneration
          it.generation = knownGeneration
          dirty
        }
      } ?: false
  }
}

private fun channelsEqual(
  a: Color,
  b: Color,
): Boolean =
  a.r == b.r &&
    a.g == b.g &&
    a.b == b.b &&
    a.a == b.a

private fun channelsHash(
  r: Proportion,
  g: Proportion,
  b: Proportion,
  a: Proportion,
): Int {
  var result = 17
  result = 31 *
    result +
    r
      .toDouble()
      .hashCode()
  result = 31 *
    result +
    g
      .toDouble()
      .hashCode()
  result = 31 *
    result +
    b
      .toDouble()
      .hashCode()
  result = 31 *
    result +
    a
      .toDouble()
      .hashCode()
  return result
}

private fun createColorImmutable(
  r: Proportion,
  g: Proportion,
  b: Proportion,
  a: Proportion,
): ImmutableColor =
  when {
    r.isZero && g.isZero && b.isZero && a.isZero -> COLOR_TRANSPARENT
    r.isZero && g.isZero && b.isZero && a.isFull -> COLOR_BLACK
    r.isFull && g.isFull && b.isFull && a.isFull -> COLOR_WHITE
    r.isFull && g.isZero && b.isZero && a.isFull -> COLOR_RED
    r.isZero && g.isFull && b.isZero && a.isFull -> COLOR_GREEN
    r.isZero && g.isZero && b.isFull && a.isFull -> COLOR_BLUE
    r.isZero && g.isFull && b.isFull && a.isFull -> COLOR_CYAN
    r.isFull && g.isZero && b.isFull && a.isFull -> COLOR_MAGENTA
    r.isFull && g.isFull && b.isZero && a.isFull -> COLOR_YELLOW
    else -> ImmutableColorImpl(r, g, b, a)
  }

// endregion
