package net.japanesehunter.traits

import kotlin.reflect.KClass

/**
 * Provides scoped access to the current entity during system iteration.
 *
 * Operations within this scope apply to the entity currently being processed.
 * The scope is only valid during the execution of a [SystemBuilder.forEach] block.
 */
interface EntityScope {
  /**
   * Adds a trait to the current entity.
   *
   * If the entity already has a trait of the same type, it is replaced.
   *
   * @param trait the trait instance to add.
   */
  fun add(trait: Any)

  /**
   * Removes a trait from the current entity.
   *
   * @param key the trait key identifying the trait to remove.
   * @return the removed trait instance.
   *   null: returned when the entity does not have the specified trait
   */
  fun <W : Any> remove(key: TraitKey<*, W>): W?

  /**
   * Checks whether the current entity has a trait of the specified type.
   *
   * @param key the trait key identifying the trait to check.
   * @return true if the entity has the trait, false otherwise.
   */
  fun has(key: TraitKey<*, *>): Boolean
}

internal class EntityScopeImpl(
  private val registry: EntityRegistry,
  private val entity: EntityId,
) : EntityScope {
  override fun add(trait: Any) {
    registry.add(entity, trait)
  }

  override fun <W : Any> remove(key: TraitKey<*, W>): W? = registry.remove(entity, key.writableType)

  override fun has(key: TraitKey<*, *>): Boolean = registry.has(entity, key.writableType)
}
