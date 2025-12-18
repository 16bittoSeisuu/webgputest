package net.japanesehunter.math

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Formats a Double value to a string with the specified number of decimal places.
 *
 * @param value The value to format.
 * @param decimals The number of decimal places to include.
 *
 *   range: decimals >= 0
 * @return A formatted string representation with exactly the specified decimal places.
 */
internal fun formatDecimals(
  value: Double,
  decimals: Int,
): String {
  if (decimals == 0) {
    return value.roundToLong().toString()
  }
  val factor = 10.0.pow(decimals)
  val rounded = (value * factor).roundToLong() / factor
  val str = rounded.toString()
  val dotIndex = str.indexOf('.')
  return if (dotIndex < 0) {
    str + "." + "0".repeat(decimals)
  } else {
    val currentDecimals = str.length - dotIndex - 1
    if (currentDecimals < decimals) {
      str + "0".repeat(decimals - currentDecimals)
    } else {
      str
    }
  }
}
