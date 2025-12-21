package net.japanesehunter.traits

import net.japanesehunter.worldcreate.world.TickSink
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.time.Duration

/**
 * Builds a system that processes entities with declared trait requirements.
 *
 * Systems declare their trait access patterns using [read] and [write], then
 * define processing logic within [forEach]. The builder automatically generates
 * queries and resolves trait references for each matching entity during execution.
 */
class SystemBuilder internal constructor(
  private val registry: EntityRegistry,
) {
  private var forEachBlock: (EntityScope.(Duration) -> Unit)? = null
  private val requiredTraitTypes = mutableSetOf<KClass<*>>()
  private var currentEntity: EntityId? = null

  /**
   * Declares a read-only trait requirement.
   *
   * The returned property delegate resolves to the read-only view of the trait
   * for the current entity during [forEach] execution.
   *
   * @param key the trait key
   * @return the delegated property provider
   */
  fun <R : Any, W : Any> read(key: TraitKey<R, W>): ReadOnlyProperty<Any?, R> {
    requiredTraitTypes.add(key.writableType)
    return ReadOnlyProperty { _, _ ->
      val entity = currentEntity ?: error("read can only be accessed during forEach execution")

      @Suppress("UNCHECKED_CAST")
      val trait =
        registry.get(entity, key.writableType as KClass<W>)
          ?: error("Entity $entity missing required trait ${key.writableType}")
      key.provideReadonlyView(trait)
    }
  }

  /**
   * Declares a writable trait requirement.
   *
   * The returned property delegate resolves to the writable trait instance
   * for the current entity during [forEach] execution.
   *
   * @param key the trait key
   * @return the delegated property provider
   */
  fun <W : Any> write(key: TraitKey<*, W>): ReadWriteProperty<Any?, W> {
    requiredTraitTypes.add(key.writableType)
    return object : ReadWriteProperty<Any?, W> {
      override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
      ): W {
        val entity = currentEntity ?: error("write can only be accessed during forEach execution")
        @Suppress("UNCHECKED_CAST")
        return registry.get(entity, key.writableType as KClass<W>)
          ?: error("Entity $entity missing required trait ${key.writableType}")
      }

      override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: W,
      ) {
        val entity = currentEntity ?: error("write can only be accessed during forEach execution")
        @Suppress("UNCHECKED_CAST")
        registry.add(entity, value as Any)
      }
    }
  }

  /**
   * Defines the processing logic applied to each matching entity.
   *
   * The block receives the tick duration and executes with [EntityScope] as the receiver,
   * providing access to entity mutation operations. Trait properties are resolved to
   * the current entity. Entities lacking any declared trait are excluded from iteration.
   *
   * @param block the processing logic with [EntityScope] receiver.
   */
  fun forEach(block: EntityScope.(Duration) -> Unit) {
    forEachBlock = block
  }

  internal fun execute(dt: Duration) {
    val block = forEachBlock ?: return
    val entities = registry.query(*requiredTraitTypes.toTypedArray())
    for (entity in entities) {
      currentEntity = entity
      try {
        val scope = EntityScopeImpl(registry, entity)
        scope.block(dt)
      } finally {
        currentEntity = null
      }
    }
  }
}

/**
 * Builds a system that processes entities with declared trait requirements.
 *
 * The returned [TickSink] can be subscribed to a tick source to execute the
 * system's logic on each tick.
 *
 * @param registry the entity registry to query and access traits from.
 * @param block the system builder configuration
 * @return the tick sink. null: never returns null
 */
fun buildSystem(
  registry: EntityRegistry,
  block: SystemBuilder.() -> Unit,
): TickSink {
  val builder = SystemBuilder(registry)
  builder.block()
  return TickSink { dt ->
    builder.execute(dt)
  }
}
