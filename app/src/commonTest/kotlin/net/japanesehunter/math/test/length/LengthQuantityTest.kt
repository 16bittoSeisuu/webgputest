package net.japanesehunter.math.test.length

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
          override fun toDouble(
            unit: net.japanesehunter.math.test.QuantityUnit<Length>,
          ): Double =
            Double.NaN

          override fun roundToLong(
            unit: net.japanesehunter.math.test.QuantityUnit<Length>,
          ): Long =
            0L

          override fun toLong(
            unit: net.japanesehunter.math.test.QuantityUnit<Length>,
          ): Long =
            0L

          override fun plus(
            other: net.japanesehunter.math.test.Quantity<Length>,
          ): LengthQuantity =
            this

          override fun times(
            scalar: Double,
          ): LengthQuantity =
            this

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

    // TODO: we need a test for different dimension quantities equality
  })
