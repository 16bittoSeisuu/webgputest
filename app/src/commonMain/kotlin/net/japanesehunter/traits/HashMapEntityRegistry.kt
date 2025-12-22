package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * A hash map-based in-memory implementation of [EntityRegistry].
 *
 * Stores entities and their traits using hash maps. Entity IDs are assigned
 * sequentially starting from 1. Destroyed entity IDs are not reused.
 *
 * This implementation is not thread-safe. External synchronization is required
 * when accessing from multiple threads.
 */
class HashMapEntityRegistry :
  EntityRegistry,
  IdBackedEntityStore {
  private var nextId: Int = 1
  private val aliveEntities: MutableSet<EntityId> = mutableSetOf()
  private val traitStores: MutableMap<KClass<*>, MutableMap<EntityId, Any>> = mutableMapOf()

  // Internal alias for backward compatibility
  internal fun create(): EntityId = createId()

  override fun createId(): EntityId {
    val id = EntityId(nextId++)
    aliveEntities.add(id)
    return id
  }

  override fun createEntity(): Entity {
    val id = create()
    return EntityHandle(id)
  }

  // Internal alias for backward compatibility
  internal fun destroy(entity: EntityId) {
    destroyById(entity)
  }

  override fun destroyById(id: EntityId) {
    if (aliveEntities.remove(id)) {
      traitStores.values.forEach { it.remove(id) }
    }
  }

  // Internal alias for backward compatibility
  internal fun exists(entity: EntityId): Boolean = existsById(entity)

  override fun existsById(id: EntityId): Boolean = id in aliveEntities

  // Internal alias for backward compatibility
  internal fun <T : Any> add(
    entity: EntityId,
    trait: T,
  ) {
    addById(entity, trait)
  }

  override fun <T : Any> addById(
    id: EntityId,
    trait: T,
  ) {
    require(id in aliveEntities) { "Entity $id does not exist" }
    val store = traitStores.getOrPut(trait::class) { mutableMapOf() }
    store[id] = trait
  }

  // Internal alias for backward compatibility
  internal fun <T : Any> get(
    entity: EntityId,
    type: KClass<T>,
  ): T? = getById(entity, type)

  override fun <T : Any> getById(
    id: EntityId,
    type: KClass<T>,
  ): T? {
    @Suppress("UNCHECKED_CAST")
    return traitStores[type]?.get(id) as T?
  }

  // Internal alias for backward compatibility
  internal fun <T : Any> remove(
    entity: EntityId,
    type: KClass<T>,
  ): T? = removeById(entity, type)

  override fun <T : Any> removeById(
    id: EntityId,
    type: KClass<T>,
  ): T? {
    @Suppress("UNCHECKED_CAST")
    return traitStores[type]?.remove(id) as T?
  }

  // Internal alias for backward compatibility
  internal fun has(
    entity: EntityId,
    type: KClass<*>,
  ): Boolean = hasById(entity, type)

  override fun hasById(
    id: EntityId,
    type: KClass<*>,
  ): Boolean = traitStores[type]?.containsKey(id) == true

  // Internal alias for backward compatibility
  internal fun query(vararg types: KClass<*>): Sequence<EntityId> = queryIds(*types)

  override fun queryIds(vararg types: KClass<*>): Sequence<EntityId> {
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

  override fun queryEntities(vararg types: KClass<*>): Sequence<Entity> = query(*types).map { id -> EntityHandle(id) }

  /**
   * Entity handle implementation specific to [HashMapEntityRegistry].
   *
   * Holds a reference to the internal entity ID. All operations delegate to
   * the enclosing registry and verify that the entity is still alive before
   * proceeding.
   *
   * Two instances are considered equal if they refer to the same entity within
   * the same registry instance. The hash code is derived from the internal
   * entity identifier to ensure consistency with equals.
   *
   * This class is not thread-safe. External synchronization is required when
   * accessing from multiple threads.
   *
   * @param id the internal entity identifier.
   */
  private inner class EntityHandle(
    private val id: EntityId,
  ) : Entity {
    override val isAlive: Boolean
      get() = exists(id)

    override fun <T : Any> add(trait: T) {
      checkAlive()
      this@HashMapEntityRegistry.add(id, trait)
    }

    override fun <T : Any> get(type: KClass<T>): T? {
      checkAlive()
      return this@HashMapEntityRegistry.get(id, type)
    }

    override fun <T : Any> remove(type: KClass<T>): T? {
      checkAlive()
      return this@HashMapEntityRegistry.remove(id, type)
    }

    override fun has(type: KClass<*>): Boolean {
      checkAlive()
      return this@HashMapEntityRegistry.has(id, type)
    }

    override fun destroy() {
      checkAlive()
      this@HashMapEntityRegistry.destroy(id)
    }

    private fun checkAlive() {
      check(isAlive) { "Entity has been destroyed" }
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is EntityHandle) return false
      if (this@HashMapEntityRegistry !== other.outer) return false
      return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Entity($id)"

    private val EntityHandle.outer: HashMapEntityRegistry
      get() = this@HashMapEntityRegistry
  }
}
