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

  override fun createEntity(): Entity {
    val id = create()
    return EntityImpl(id, this)
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

/**
 * Default implementation of [Entity] backed by an [EntityRegistry].
 *
 * Holds a reference to the registry and its internal entity ID. All operations
 * delegate to the registry and verify that the entity is still alive before
 * proceeding.
 *
 * This class is not thread-safe. External synchronization is required when
 * accessing from multiple threads.
 *
 * @param id the internal entity identifier.
 * @param registry the registry that owns this entity.
 */
private class EntityImpl(
  private val id: EntityId,
  private val registry: EntityRegistry,
) : Entity {
  override val isAlive: Boolean
    get() = registry.exists(id)

  override fun <T : Any> add(trait: T) {
    checkAlive()
    registry.add(id, trait)
  }

  override fun <T : Any> get(type: KClass<T>): T? {
    checkAlive()
    return registry.get(id, type)
  }

  override fun <T : Any> remove(type: KClass<T>): T? {
    checkAlive()
    return registry.remove(id, type)
  }

  override fun has(type: KClass<*>): Boolean {
    checkAlive()
    return registry.has(id, type)
  }

  override fun destroy() {
    checkAlive()
    registry.destroy(id)
  }

  private fun checkAlive() {
    check(isAlive) { "Entity has been destroyed" }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is EntityImpl) return false
    return id == other.id && registry === other.registry
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "Entity($id)"
}
