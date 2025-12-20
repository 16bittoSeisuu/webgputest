package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutableVelocity3

/**
 * The rate of position change for an entity expressed as distance per second.
 *
 * Velocity is modified by physics simulation systems and directly affects the
 * entity's position in subsequent frames.
 */
data class Velocity(
  val value: MutableVelocity3,
)
