package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * Represents a handle to an entity within an [EntityRegistry].
 *
 * This interface provides high-level operations for managing traits attached to
 * the entity. Once the entity is destroyed, all operations on this handle will
 * throw [IllegalStateException].
 *
 * Implementations are not required to be thread-safe. Callers must provide
 * external synchronization when accessing from multiple threads.
 */
interface Entity {
  /**
   * Indicates whether this entity is still alive in the registry.
   */
  val isAlive: Boolean

  /**
   * Attaches a trait to this entity.
   *
   * If the entity already has a trait of the same type, it will be replaced.
   *
   * @param T the type of the trait.
   * @param trait the trait instance to attach.
   * @throws IllegalStateException if this entity has been destroyed.
   */
  fun <T : Any> add(trait: T)

  /**
   * Retrieves a trait of the specified type from this entity.
   *
   * @param T the type of the trait to retrieve.
   * @param type the KClass of the trait type.
   * @return the trait instance.
   *   null: when the entity does not have this trait
   * @throws IllegalStateException if this entity has been destroyed.
   */
  fun <T : Any> get(type: KClass<T>): T?

  /**
   * Removes a trait of the specified type from this entity.
   *
   * @param T the type of the trait to remove.
   * @param type the KClass of the trait type.
   * @return the removed trait instance.
   *   null: when the entity did not have this trait
   * @throws IllegalStateException if this entity has been destroyed.
   */
  fun <T : Any> remove(type: KClass<T>): T?

  /**
   * Checks whether this entity has a trait of the specified type.
   *
   * @param type the KClass of the trait type.
   * @return true if the entity has the trait, false otherwise.
   * @throws IllegalStateException if this entity has been destroyed.
   */
  fun has(type: KClass<*>): Boolean

  /**
   * Destroys this entity and removes all of its associated traits.
   *
   * After destruction, all subsequent operations on this handle will throw
   * [IllegalStateException].
   */
  fun destroy()
}

/**
 * Retrieves a trait of the specified type from this entity.
 *
 * @param T the type of the trait to retrieve.
 * @return the trait instance.
 *   null: when the entity does not have this trait
 * @throws IllegalStateException if this entity has been destroyed.
 */
inline fun <reified T : Any> Entity.get(): T? = get(T::class)

/**
 * Removes a trait of the specified type from this entity.
 *
 * @param T the type of the trait to remove.
 * @return the removed trait instance.
 *   null: when the entity did not have this trait
 * @throws IllegalStateException if this entity has been destroyed.
 */
inline fun <reified T : Any> Entity.remove(): T? = remove(T::class)

/**
 * Checks whether this entity has a trait of the specified type.
 *
 * @param T the type of the trait to check.
 * @return true if the entity has the trait, false otherwise.
 * @throws IllegalStateException if this entity has been destroyed.
 */
inline fun <reified T : Any> Entity.has(): Boolean = has(T::class)
