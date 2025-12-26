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
interface EntityRegistry : EntityQuery {
  /**
   * Creates a new entity and returns a high-level handle to it.
   *
   * The returned [Entity] handle provides direct access to trait operations
   * without needing to reference the registry explicitly.
   *
   * @return a handle to the newly created entity.
   */
  fun createEntity(): Entity
}

fun interface EntityQuery {
  /**
   * Returns all entities that have all the specified trait types.
   *
   * The returned entity handles refer to entities that exist at the time of
   * enumeration. Each handle follows the equality contract defined by [Entity],
   * meaning that multiple calls to this method with the same query will return
   * handles that are equal to each other for the same underlying entity.
   *
   * @param types the trait types to match.
   * @return a sequence of entity handles that have all specified traits.
   */
  fun query(vararg types: KClass<*>): Sequence<Entity>
}
