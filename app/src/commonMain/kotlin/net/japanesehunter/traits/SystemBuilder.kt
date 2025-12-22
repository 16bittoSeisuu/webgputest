package net.japanesehunter.traits

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
class SystemBuilder internal constructor() {
  private var forEachBlock: (EntityScope.(Duration) -> Unit)? = null
  private val requiredTraitTypes = mutableSetOf<KClass<*>>()
  private var currentEntity: Entity? = null

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

      val trait =
        entity.get(key.writableType)
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
        return entity.get(key.writableType)
          ?: error("Entity $entity missing required trait ${key.writableType}")
      }

      override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: W,
      ) {
        val entity = currentEntity ?: error("write can only be accessed during forEach execution")
        entity.add(value)
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

  internal fun execute(
    registry: EntityRegistry,
    dt: Duration,
  ) {
    val block = forEachBlock ?: return
    val entities = registry.query(*requiredTraitTypes.toTypedArray())
    for (entity in entities) {
      currentEntity = entity
      try {
        val scope = EntityScopeImpl(entity)
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
 * The returned [TraitUpdateSink] can be subscribed to a tick source using the
 * context-aware extension to execute the system's logic on each update.
 *
 * @param block the system builder configuration
 * @return the trait update sink. null: never returns null
 */
fun buildSystem(block: SystemBuilder.() -> Unit): TraitUpdateSink {
  val builder = SystemBuilder()
  builder.block()
  return TraitUpdateSink { event ->
    builder.execute(event.registry, event.dt)
  }
}
