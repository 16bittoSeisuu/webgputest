package net.japanesehunter.traits

/**
 * Represents a unique identifier for an entity within an [EntityRegistry].
 *
 * Entity IDs are opaque handles that remain valid only while the entity exists
 * in the registry. Once an entity is destroyed, its ID should not be reused
 * to access traits.
 *
 * This type is not thread-safe. External synchronization is required when
 * sharing instances across threads.
 *
 * @param value the underlying integer identifier.
 */
value class EntityId(
  val value: Int,
)
