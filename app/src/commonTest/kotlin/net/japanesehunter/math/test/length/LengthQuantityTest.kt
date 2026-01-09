package net.japanesehunter.math.test.length

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.length.NanometerLength.Companion.meters
import net.japanesehunter.math.test.length.NanometerLength.Companion.nanometers

class LengthQuantityTest :
  FunSpec({
    test("0.0 meters and -0.0 meters are equal") {
      val posZero = 0.0.meters
      val negZero = (-0.0).meters
      posZero shouldBe negZero
      posZero.hashCode() shouldBe negZero.hashCode()
    }

    test("Different units representing same physical amount are equal") {
      1.0.meters shouldBe 1_000_000_000L.nanometers
      1.0
        .meters
        .hashCode() shouldBe
        1_000_000_000L
          .nanometers
          .hashCode()
    }

    test("NaN values are equal to each other") {
      val nanQuantity =
        object : LengthQuantity() {
          override val resolution: QuantityUnit<Length> = nanometers

          override fun toDouble(
            unit: QuantityUnit<Length>,
          ): Double =
            Double.NaN

          override fun roundToLong(
            unit: QuantityUnit<Length>,
          ): Long =
            0L

          override fun toLong(
            unit: QuantityUnit<Length>,
          ): Long =
            0L

          override fun plus(
            other: Quantity<Length>,
          ): LengthQuantity =
            this

          override fun times(
            scalar: Double,
          ): LengthQuantity =
            this

          override val absoluteValue: LengthQuantity
            get() = this

          override fun isPositive(): Boolean = false

          override fun isNegative(): Boolean = false

          override fun isZero(): Boolean = false

          override fun times(
            scalar: Long,
          ): LengthQuantity =
            this

          override fun toString(): String =
            "NaN"
        }
      nanQuantity shouldBe nanQuantity
      nanQuantity.hashCode() shouldBe nanQuantity.hashCode()
    }

    test("Different values are not equal") {
      1.0.meters shouldNotBe 2.0.meters
      1.0
        .meters
        .hashCode() shouldNotBe
        2.0
          .meters
          .hashCode()
    }

    test("Different dimension quantities are not equal") {
      val length = 1.0.meters
      val otherQuantity = OtherQuantity() // always represents 1
      length shouldNotBe otherQuantity
    }
  })

private class OtherDimension : Dimension<OtherDimension> {
  override val canonicalUnit:
    QuantityUnit<OtherDimension> by lazy {
      QuantityUnit
        .base(this, "Other", "o")
    }
}

private class OtherQuantity : Quantity<OtherDimension> {
  override val resolution:
    QuantityUnit<OtherDimension> =
    OtherDimension()
      .canonicalUnit

  override fun toDouble(
    unit: QuantityUnit<OtherDimension>,
  ): Double =
    1.0

  override fun toLong(
    unit: QuantityUnit<OtherDimension>,
  ): Long =
    1L

  override fun roundToLong(
    unit: QuantityUnit<OtherDimension>,
  ): Long =
    1L

  override fun plus(
    other: Quantity<OtherDimension>,
  ): Quantity<OtherDimension> =
    this

  override fun times(
    scalar: Double,
  ): Quantity<OtherDimension> =
    this

  override fun times(
    scalar: Long,
  ): Quantity<OtherDimension> =
    this

  override fun isPositive(): Boolean = true

  override fun isNegative(): Boolean = false

  override fun isZero(): Boolean = false

  override val absoluteValue: Quantity<OtherDimension>
    get() = this

  override fun toString(): String =
    "Other"

  override fun equals(
    other: Any?,
  ): Boolean =
    false

  override fun hashCode(): Int =
    0
}
