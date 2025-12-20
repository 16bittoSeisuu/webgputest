package net.japanesehunter.worldcreate.trait

/**
 * Marks an entity as currently in contact with solid ground.
 *
 * The presence of this trait indicates the entity is grounded. The absence
 * indicates the entity is airborne. This state is determined by the physics
 * simulation system after resolving collisions during movement.
 *
 * An entity is grounded when its downward motion is blocked by collision
 * geometry below it.
 */
data object Grounded
