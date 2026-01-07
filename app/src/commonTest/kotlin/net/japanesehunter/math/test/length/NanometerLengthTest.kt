package net.japanesehunter.math.test.length

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.japanesehunter.math.test.length.NanometerLength.Companion.meters
import net.japanesehunter.math.test.length.NanometerLength.Companion.millimeters
import net.japanesehunter.math.test.length.NanometerLength.Companion.nanometers

class NanometerLengthTest :
  FunSpec({
    test("Unit conversion equality is based on the physical amount") {
      1000L.millimeters shouldBe 1.0.meters
    }

    test("Double DSL rejects NaN") {
      shouldThrow<IllegalArgumentException> {
        Double.NaN.meters
      }
    }

    test("Double DSL rejects infinity") {
      shouldThrow<IllegalArgumentException> {
        Double.POSITIVE_INFINITY.meters
      }
    }

    test("Sign is preserved for the DSL") {
      (-1)
        .meters
        .toLong(meters) shouldBe -1L
    }

    test("meters to nanometers is exact for Long DSL") {
      1
        .meters
        .toLong(nanometers) shouldBe 1_000_000_000L
      (-1)
        .meters
        .toLong(nanometers) shouldBe -1_000_000_000L
    }

    test("toLong(nanometers) returns the underlying value") {
      123
        .nanometers
        .toLong(nanometers) shouldBe 123L
      (-123)
        .nanometers
        .toLong(nanometers) shouldBe -123L
    }

    test("toLong(inches) truncates toward zero") {
      3
        .millimeters
        .toLong(inches) shouldBe 0L
      (-3)
        .millimeters
        .toLong(inches) shouldBe 0L
    }

    test("roundToLong(inches) rounds ties away from zero") {
      13
        .millimeters
        .roundToLong(inches) shouldBe 1L
      (-13)
        .millimeters
        .roundToLong(inches) shouldBe -1L
    }

    test("plus throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers + 1.nanometers
      }
    }

    test("times rejects non-finite scalar") {
      shouldThrow<IllegalArgumentException> {
        1.nanometers * Double.NaN
      }
    }
  })


