package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * Manages the lifecycle of entities and their associated traits.
 *
 * An entity is a lightweight identifier that can have zero or more traits
 * attached to it. Traits are arbitrary data objects that describe aspects
 * of an entity such as position, velocity, or appearance.
 *
 * Implementations are not required to be thread-safe. Callers must provide
 * external synchronization when accessing from multiple threads.
 */
interface EntityRegistry {
  /**
   * Creates a new entity and returns its unique identifier.
   *
   * @return the identifier of the newly created entity.
   */
  fun create(): EntityId

  /**
   * Destroys an entity and removes all of its associated traits.
   *
   * After destruction, the given [entity] ID becomes invalid and should
   * not be used to access traits.
   *
   * @param entity the identifier of the entity to destroy.
   */
  fun destroy(entity: EntityId)

  /**
   * Checks whether the given entity currently exists in this registry.
   *
   * @param entity the identifier to check.
   * @return true if the entity exists, false otherwise.
   */
  fun exists(entity: EntityId): Boolean

  /**
   * Attaches a trait to the specified entity.
   *
   * If the entity already has a trait of the same type, it will be replaced.
   *
   * @param T the type of the trait.
   * @param entity the target entity.
   * @param trait the trait instance to attach.
   */
  fun <T : Any> add(
    entity: EntityId,
    trait: T,
  )

  /**
   * Retrieves a trait of the specified type from an entity.
   *
   * @param T the type of the trait to retrieve.
   * @param entity the target entity.
   * @param type the KClass of the trait type.
   * @return the trait instance, or null if the entity does not have this trait.
   */
  fun <T : Any> get(
    entity: EntityId,
    type: KClass<T>,
  ): T?

  /**
   * Removes a trait of the specified type from an entity.
   *
   * @param T the type of the trait to remove.
   * @param entity the target entity.
   * @param type the KClass of the trait type.
   * @return the removed trait instance, or null if the entity did not have this trait.
   */
  fun <T : Any> remove(
    entity: EntityId,
    type: KClass<T>,
  ): T?

  /**
   * Checks whether an entity has a trait of the specified type.
   *
   * @param entity the target entity.
   * @param type the KClass of the trait type.
   * @return true if the entity has the trait, false otherwise.
   */
  fun has(
    entity: EntityId,
    type: KClass<*>,
  ): Boolean

  /**
   * Returns all entity IDs that have all of the specified trait types.
   *
   * @param types the trait types to match.
   * @return a sequence of entity IDs that have all specified traits.
   */
  fun query(vararg types: KClass<*>): Sequence<EntityId>
}

/**
 * Retrieves a trait of the specified type from an entity.
 *
 * @param T the type of the trait to retrieve.
 * @param entity the target entity.
 * @return the trait instance, or null if the entity does not have this trait.
 */
inline fun <reified T : Any> EntityRegistry.get(entity: EntityId): T? = get(entity, T::class)

/**
 * Removes a trait of the specified type from an entity.
 *
 * @param T the type of the trait to remove.
 * @param entity the target entity.
 * @return the removed trait instance, or null if the entity did not have this trait.
 */
inline fun <reified T : Any> EntityRegistry.remove(entity: EntityId): T? = remove(entity, T::class)

/**
 * Checks whether an entity has a trait of the specified type.
 *
 * @param T the type of the trait to check.
 * @param entity the target entity.
 * @return true if the entity has the trait, false otherwise.
 */
inline fun <reified T : Any> EntityRegistry.has(entity: EntityId): Boolean = has(entity, T::class)
