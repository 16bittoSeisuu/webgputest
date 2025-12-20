package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * A simple in-memory implementation of [EntityRegistry].
 *
 * Stores entities and their traits using hash maps. Entity IDs are assigned
 * sequentially starting from 1. Destroyed entity IDs are not reused.
 *
 * This implementation is not thread-safe. External synchronization is required
 * when accessing from multiple threads.
 */
class SimpleEntityRegistry : EntityRegistry {
  private var nextId: Int = 1
  private val aliveEntities: MutableSet<EntityId> = mutableSetOf()
  private val traitStores: MutableMap<KClass<*>, MutableMap<EntityId, Any>> = mutableMapOf()

  override fun create(): EntityId {
    val id = EntityId(nextId++)
    aliveEntities.add(id)
    return id
  }

  override fun destroy(entity: EntityId) {
    if (aliveEntities.remove(entity)) {
      traitStores.values.forEach { it.remove(entity) }
    }
  }

  override fun exists(entity: EntityId): Boolean = entity in aliveEntities

  override fun <T : Any> add(
    entity: EntityId,
    trait: T,
  ) {
    require(entity in aliveEntities) { "Entity $entity does not exist" }
    val store = traitStores.getOrPut(trait::class) { mutableMapOf() }
    store[entity] = trait
  }

  override fun <T : Any> get(
    entity: EntityId,
    type: KClass<T>,
  ): T? {
    @Suppress("UNCHECKED_CAST")
    return traitStores[type]?.get(entity) as T?
  }

  override fun <T : Any> remove(
    entity: EntityId,
    type: KClass<T>,
  ): T? {
    @Suppress("UNCHECKED_CAST")
    return traitStores[type]?.remove(entity) as T?
  }

  override fun has(
    entity: EntityId,
    type: KClass<*>,
  ): Boolean = traitStores[type]?.containsKey(entity) == true

  override fun query(vararg types: KClass<*>): Sequence<EntityId> {
    if (types.isEmpty()) {
      return aliveEntities.asSequence()
    }

    val stores = types.mapNotNull { traitStores[it] }
    if (stores.size != types.size) {
      return emptySequence()
    }

    val smallest = stores.minBy { it.size }
    return smallest.keys.asSequence().filter { entity ->
      stores.all { entity in it }
    }
  }
}
