package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * Provides low-level entity storage operations using internal entity identifiers.
 *
 * This interface is intended for internal use within the entity system implementation.
 * External code should use [EntityRegistry] and [Entity] instead.
 *
 * Implementations are not required to be thread-safe. Callers must provide
 * external synchronization when accessing from multiple threads.
 */
internal interface IdBackedEntityStore {
  /**
   * Creates a new entity and returns its internal identifier.
   *
   * @return the internal identifier of the newly created entity.
   */
  fun createId(): EntityId

  /**
   * Destroys an entity by its internal identifier.
   *
   * @param id the internal identifier of the entity to destroy.
   */
  fun destroyById(id: EntityId)

  /**
   * Checks whether an entity with the given identifier exists.
   *
   * @param id the internal identifier to check.
   * @return true if the entity exists, false otherwise.
   */
  fun existsById(id: EntityId): Boolean

  /**
   * Attaches a trait to an entity by its internal identifier.
   *
   * @param T the type of the trait.
   * @param id the internal identifier of the target entity.
   * @param trait the trait instance to attach.
   */
  fun <T : Any> addById(
    id: EntityId,
    trait: T,
  )

  /**
   * Retrieves a trait from an entity by its internal identifier.
   *
   * @param T the type of the trait to retrieve.
   * @param id the internal identifier of the target entity.
   * @param type the KClass of the trait type.
   * @return the trait instance.
   *   null: when the entity does not have this trait
   */
  fun <T : Any> getById(
    id: EntityId,
    type: KClass<T>,
  ): T?

  /**
   * Removes a trait from an entity by its internal identifier.
   *
   * @param T the type of the trait to remove.
   * @param id the internal identifier of the target entity.
   * @param type the KClass of the trait type.
   * @return the removed trait instance.
   *   null: when the entity did not have this trait
   */
  fun <T : Any> removeById(
    id: EntityId,
    type: KClass<T>,
  ): T?

  /**
   * Checks whether an entity has a trait by its internal identifier.
   *
   * @param id the internal identifier of the target entity.
   * @param type the KClass of the trait type.
   * @return true if the entity has the trait, false otherwise.
   */
  fun hasById(
    id: EntityId,
    type: KClass<*>,
  ): Boolean

  /**
   * Returns all entity identifiers that have all of the specified trait types.
   *
   * @param types the trait types to match.
   * @return a sequence of internal identifiers that have all specified traits.
   */
  fun queryIds(vararg types: KClass<*>): Sequence<EntityId>
}

/**
 * Retrieves a trait from an entity by its internal identifier.
 *
 * @param T the type of the trait to retrieve.
 * @param id the internal identifier of the target entity.
 * @return the trait instance.
 *   null: when the entity does not have this trait
 */
internal inline fun <reified T : Any> IdBackedEntityStore.getById(id: EntityId): T? = getById(id, T::class)

/**
 * Removes a trait from an entity by its internal identifier.
 *
 * @param T the type of the trait to remove.
 * @param id the internal identifier of the target entity.
 * @return the removed trait instance.
 *   null: when the entity did not have this trait
 */
internal inline fun <reified T : Any> IdBackedEntityStore.removeById(id: EntityId): T? = removeById(id, T::class)

/**
 * Checks whether an entity has a trait by its internal identifier.
 *
 * @param T the type of the trait to check.
 * @param id the internal identifier of the target entity.
 * @return true if the entity has the trait, false otherwise.
 */
internal inline fun <reified T : Any> IdBackedEntityStore.hasById(id: EntityId): Boolean = hasById(id, T::class)
