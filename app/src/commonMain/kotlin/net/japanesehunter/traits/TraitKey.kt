package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * Represents a type-safe key for accessing entity traits with read and write views.
 *
 * Each key associates a read-only view type [R] with a writable type [W].
 * Systems declare trait access using [R] for observation and [W] for mutation.
 *
 * Implementations must ensure that [R] does not expose state mutation operations.
 *
 * @param R the read-only view type
 * @param W the writable type
 */
interface TraitKey<out R : Any, W : Any> {
  /**
   * The runtime type of the writable trait.
   */
  val writableType: KClass<W>

  /**
   * Converts the writable instance to a read-only view.
   *
   * @param write the writable instance
   * @return the read-only view. null: never returns null
   */
  fun provideReadonlyView(write: W): R
}

/**
 * Creates a [TraitKey] with a custom conversion function from [W] to [R].
 *
 * Use this factory when the writable type requires explicit transformation
 * to provide a read-only view.
 *
 * @param toReadonlyView the conversion function from writable to read-only view.
 * @param R the read-only view type
 * @param W the writable type
 * @return the trait key. null: never returns null
 */
inline fun <R : Any, reified W : Any> TraitKey(noinline toReadonlyView: (W) -> R): TraitKey<R, W> =
  object : TraitKey<R, W> {
    override val writableType: KClass<W> = W::class

    override fun provideReadonlyView(write: W): R = toReadonlyView(write)
  }

/**
 * Creates a [TraitKey] with identity conversion from [W] to [R].
 *
 * Returns the writable instance directly as the read-only view without transformation.
 * Use this factory only when [R] provides no state mutation operations.
 *
 * Traits holding mutable references should provide explicit read-only wrappers
 * through custom [TraitKey] implementations.
 *
 * @param R the read-only view type
 * @param W the writable type
 * @return the trait key. null: never returns null
 */
inline fun <R : Any, reified W : R> TraitKey(): TraitKey<R, W> = TraitKey { it }
