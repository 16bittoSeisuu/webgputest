package net.japanesehunter.math.test.time

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.QuantityUnit

/**
 * Defines the time dimension.
 *
 * ## Description
 *
 * The dimension responsible for time quantities.
 * The canonical unit of this dimension is [second].
 *
 * Comparisons between [Time] objects are based on identity.
 */
data object Time : Dimension<Time> {
  override val canonicalUnit: QuantityUnit<Time> by lazy {
    second
  }
}
