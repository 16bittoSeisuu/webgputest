package net.japanesehunter.math.test.speed

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.japanesehunter.math.test.length.NanometerLength.Companion.meters
import net.japanesehunter.math.test.length.NanometerLength.Companion.millimeters
import net.japanesehunter.math.test.length.NanometerLength.Companion.nanometers
import net.japanesehunter.math.test.length.meter
import net.japanesehunter.math.test.length.nanometer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class SpeedTest :
  FunSpec({
    // region Equality and hashCode

    test("Unit conversion equality is based on the physical amount") {
      val a = 3600.meters / 1.hours
      val b = 1.meters / 1.seconds
      a shouldBe b
    }

    test("Equality implies hashCode equality") {
      val a = 3600.meters / 1.hours
      val b = 1.meters / 1.seconds
      a shouldBe b
      a.hashCode() shouldBe b.hashCode()
    }

    test("Equality works in hash-based collections") {
      val a = 3600.meters / 1.hours
      val b = 1.meters / 1.seconds
      val set = hashSetOf(a)
      set.contains(b) shouldBe true
    }

    // endregion

    // region Arithmetic with non-canonical units

    test("plus returns the exact sum with non-canonical units") {
      val a = 1.millimeters / 1.seconds
      val b = 2.millimeters / 1.seconds
      val result = a + b
      result.toDouble(metersPerSecond) shouldBe 0.003
    }

    test("minus returns the exact difference with non-canonical units") {
      val a = 3.millimeters / 1.seconds
      val b = 1.millimeters / 1.seconds
      val result = a - b
      result.toDouble(metersPerSecond) shouldBe 0.002
    }

    // endregion

    // region Scalar multiplication and division

    test("times scales the physical amount with Double") {
      val speed = 1.millimeters / 1.seconds
      val result = speed * 2.0
      result.toDouble(metersPerSecond) shouldBe 0.002
    }

    test("times scales the physical amount with Long") {
      val speed = 1.millimeters / 1.seconds
      val result = speed * 2L
      result.toDouble(metersPerSecond) shouldBe 0.002
    }

    test("times scales the physical amount with Int") {
      val speed = 1.millimeters / 1.seconds
      val result = speed * 2
      result.toDouble(metersPerSecond) shouldBe 0.002
    }

    test("div scales the physical amount with Double") {
      val speed = 2.millimeters / 1.seconds
      val result = speed / 2.0
      result.toDouble(metersPerSecond) shouldBe 0.001
    }

    test("div scales the physical amount with Long") {
      val speed = 2.millimeters / 1.seconds
      val result = speed / 2L
      result.toDouble(metersPerSecond) shouldBe 0.001
    }

    test("div scales the physical amount with Int") {
      val speed = 2.millimeters / 1.seconds
      val result = speed / 2
      result.toDouble(metersPerSecond) shouldBe 0.001
    }

    // endregion

    // region Sign and absoluteValue

    test("isPositive returns true if strictly greater than zero") {
      val positive = 1.millimeters / 1.seconds
      val zero = 0.meters / 1.seconds
      val negative = (-1).millimeters / 1.seconds

      positive.isPositive() shouldBe true
      zero.isPositive() shouldBe false
      negative.isPositive() shouldBe false
    }

    test("isNegative returns true if strictly less than zero") {
      val positive = 1.millimeters / 1.seconds
      val zero = 0.meters / 1.seconds
      val negative = (-1).millimeters / 1.seconds

      negative.isNegative() shouldBe true
      zero.isNegative() shouldBe false
      positive.isNegative() shouldBe false
    }

    test("isZero returns true if exactly zero") {
      val positive = 1.millimeters / 1.seconds
      val zero = 0.meters / 1.seconds
      val negative = (-1).millimeters / 1.seconds

      zero.isZero() shouldBe true
      positive.isZero() shouldBe false
      negative.isZero() shouldBe false
    }

    test(
      "absoluteValue returns a non-negative quantity with the same magnitude",
    ) {
      val positive = 1.millimeters / 1.seconds
      val negative = (-1).millimeters / 1.seconds
      val zero = 0.meters / 1.seconds

      positive.absoluteValue
        .toDouble(metersPerSecond) shouldBe 0.001
      negative.absoluteValue
        .toDouble(metersPerSecond) shouldBe 0.001
      zero.absoluteValue
        .toDouble(metersPerSecond) shouldBe 0.0
    }

    test("unaryPlus returns an equal value") {
      val speed = 1.millimeters / 1.seconds
      +speed shouldBe speed
    }

    test("unaryMinus negates the physical amount") {
      val speed = 1.millimeters / 1.seconds
      val negated = -speed
      negated.toDouble(metersPerSecond) shouldBe -0.001
      negated shouldNotBe speed
    }

    // endregion

    // region Cross-dimension operations

    test("Speed times Duration returns the expected Length") {
      val speed = 1.meters / 1.seconds
      val duration = 5.seconds
      val distance = speed * duration
      distance.toLong(meter) shouldBe 5L
    }

    test(
      "Speed times Duration with non-canonical units returns the expected Length",
    ) {
      val speed = 1.millimeters / 1.seconds
      val duration = 1000.seconds
      val distance = speed * duration
      distance.toLong(nanometer) shouldBe 1_000_000_000L
    }

    test("Speed divided by Duration returns the expected Acceleration") {
      val speed = 10.meters / 1.seconds
      val duration = 2.seconds
      val acceleration = speed / duration
      acceleration.toDouble(
        net.japanesehunter.math.test.acceleration.metersPerSecondSquared,
      ) shouldBe 5.0
    }

    test("Speed per Duration returns the expected Acceleration") {
      val speed = 10.meters / 1.seconds
      val duration = 2.seconds
      val acceleration = speed per duration
      acceleration.toDouble(
        net.japanesehunter.math.test.acceleration.metersPerSecondSquared,
      ) shouldBe 5.0
    }

    // endregion

    // region Overflow

    test("plus throws on overflow") {
      val large = Long.MAX_VALUE.nanometers / 1.seconds
      shouldThrow<ArithmeticException> {
        large + large
      }
    }

    test("times throws on overflow") {
      val large = Long.MAX_VALUE.nanometers / 1.seconds
      shouldThrow<ArithmeticException> {
        large * 2L
      }
    }

    // endregion
  })
