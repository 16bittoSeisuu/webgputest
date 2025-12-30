package net.japanesehunter.math

/**
 * Represents a strategy for formatting the sign of numeric values.
 *
 * Implementations determine how positive, negative, and zero values are prefixed
 * when converted to strings. This allows consistent sign formatting across
 * different display contexts.
 */
interface SignMode {
  /**
   * Returns the prefix string to display for the given sign.
   *
   * @param isNegative Whether the value is negative.
   * @return The prefix string to prepend to the formatted value.
   */
  fun prefix(
    isNegative: Boolean,
  ): String

  /**
   * Always displays an explicit sign.
   *
   * Negative values are prefixed with `-`, positive and zero values with `+`.
   */
  data object Always : SignMode {
    override fun prefix(
      isNegative: Boolean,
    ): String =
      if (isNegative) "-" else "+"
  }

  /**
   * Displays a space for non-negative values to align with negative values.
   *
   * Negative values are prefixed with `-`, positive and zero values with a space.
   */
  data object Space : SignMode {
    override fun prefix(
      isNegative: Boolean,
    ): String =
      if (isNegative) "-" else " "
  }

  /**
   * Only displays the sign for negative values.
   *
   * Negative values are prefixed with `-`, positive and zero values have no prefix.
   */
  data object Negative : SignMode {
    override fun prefix(
      isNegative: Boolean,
    ): String =
      if (isNegative) "-" else ""
  }
}
