package net.japanesehunter.math.test.length

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.japanesehunter.math.test.length.NanometerLength.Companion.meters
import net.japanesehunter.math.test.length.NanometerLength.Companion.micrometers
import net.japanesehunter.math.test.length.NanometerLength.Companion.millimeters
import net.japanesehunter.math.test.length.NanometerLength.Companion.nanometers

class NanometerLengthTest :
  FunSpec({
    test("Unit conversion equality is based on the physical amount") {
      1000L.millimeters shouldBe 1.0.meters
    }

    test("Equality implies hashCode equality") {
      val a = 1000L.millimeters
      val b = 1.0.meters
      a shouldBe b
      a.hashCode() shouldBe b.hashCode()
    }

    test("Equality works in hash-based collections") {
      val a = 1000L.millimeters
      val b = 1.0.meters

      val set = hashSetOf(a)
      set.contains(b) shouldBe true
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

    test("Float DSL rejects NaN") {
      shouldThrow<IllegalArgumentException> {
        Float.NaN.meters
      }
    }

    test("Float DSL rejects infinity") {
      shouldThrow<IllegalArgumentException> {
        Float.POSITIVE_INFINITY.meters
      }
    }

    test("Sign is preserved for the DSL") {
      (-1)
        .meters
        .toLong(meter) shouldBe -1L
    }

    test("meters to nanometers is exact for Long DSL") {
      1
        .meters
        .toLong(nanometer) shouldBe 1_000_000_000L
      (-1)
        .meters
        .toLong(nanometer) shouldBe -1_000_000_000L
    }

    test("Long DSL throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.meters
      }
    }

    test("Double DSL rounds to the nearest nanometer") {
      0.5
        .meters
        .toLong(nanometer) shouldBe 500_000_000L
    }

    test("toLong(nanometers) returns the underlying value") {
      123
        .nanometers
        .toLong(nanometer) shouldBe 123L
      (-123)
        .nanometers
        .toLong(nanometer) shouldBe -123L
    }

    test("toLong(inches) truncates toward zero") {
      3
        .millimeters
        .toLong(inch) shouldBe 0L
      (-3)
        .millimeters
        .toLong(inch) shouldBe 0L
    }

    test("roundToLong(inches) rounds ties away from zero") {
      12_700L
        .micrometers
        .roundToLong(inch) shouldBe 1L
      (-12_700L)
        .micrometers
        .roundToLong(inch) shouldBe -1L
    }

    test("plus throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers + 1.nanometers
      }
    }

    test("plus returns the exact sum") {
      (1.meters + 2.meters).toLong(meter) shouldBe 3L
      ((-1).meters + 2.meters).toLong(meter) shouldBe 1L
    }

    test("minus throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE.nanometers - 1.nanometers
      }
    }

    test("minus returns the exact difference") {
      (3.meters - 2.meters).toLong(meter) shouldBe 1L
      ((-3).meters - 2.meters).toLong(meter) shouldBe -5L
    }

    test("times rejects non-finite scalar") {
      shouldThrow<IllegalArgumentException> {
        1.nanometers * Double.NaN
      }
    }

    test("times throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers * 2.0
      }
    }

    test("times scales the physical amount") {
      (3.meters * 2.0).toLong(meter) shouldBe 6L
      (3.meters * -2.0).toLong(meter) shouldBe -6L
    }

    test("times(Long) throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers * 2L
      }
    }

    test("times(Long) scales the physical amount") {
      (3.meters * 2L).toLong(meter) shouldBe 6L
      (3.meters * -2L).toLong(meter) shouldBe -6L
    }

    test("times works with other numeric types") {
      (3.meters * 2).toLong(meter) shouldBe 6L // Int
      (3.meters * 2.toShort()).toLong(meter) shouldBe 6L // Short
      (3.meters * 2.toByte()).toLong(meter) shouldBe 6L // Byte
      (3.meters * 2.0f).toLong(meter) shouldBe 6L // Float
    }

    test("div rejects zero divisor") {
      shouldThrow<IllegalArgumentException> {
        1.nanometers / 0.0
      }
    }

    test("div rejects non-finite divisor") {
      shouldThrow<IllegalArgumentException> {
        1.nanometers / Double.NaN
      }
      shouldThrow<IllegalArgumentException> {
        1.nanometers / Double.POSITIVE_INFINITY
      }
      shouldThrow<IllegalArgumentException> {
        1.nanometers / Double.NEGATIVE_INFINITY
      }
    }

    test("div scales the physical amount") {
      (10.nanometers / 2.0).toLong(nanometer) shouldBe 5L
      (10.nanometers / -2.0).toLong(nanometer) shouldBe -5L
    }

    test("div(Long) throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE.nanometers / -1L
      }
    }

    test("div(Long) throws on division by zero") {
      shouldThrow<IllegalArgumentException> {
        1.nanometers / 0L
      }
    }

    test("div(Long) scales the physical amount") {
      (10.nanometers / 2L).toLong(nanometer) shouldBe 5L
      (10.nanometers / -2L).toLong(nanometer) shouldBe -5L
    }

    test("div works with other numeric types") {
      (10.nanometers / 2).toLong(nanometer) shouldBe 5L // Int
      (10.nanometers / 2.toShort()).toLong(nanometer) shouldBe 5L // Short
      (10.nanometers / 2.toByte()).toLong(nanometer) shouldBe 5L // Byte
      (10.nanometers / 2.0f).toLong(nanometer) shouldBe 5L // Float
    }

    test("toInt keeps the lower 32 bits") {
      0x1_0000_0001L
        .nanometers
        .toInt(nanometer) shouldBe 1
    }

    test("roundToInt delegates to roundToLong and keeps the lower 32 bits") {
      12_700L
        .micrometers
        .roundToInt(inch) shouldBe 1
    }

    test("toShort keeps the lower 16 bits") {
      0x1_0001L
        .nanometers
        .toShort(nanometer) shouldBe 1
    }

    test("toByte keeps the lower 8 bits") {
      0x101L
        .nanometers
        .toByte(nanometer) shouldBe 1
    }

    test("unaryPlus returns an equal value") {
      val a = 123.nanometers
      +a shouldBe a
    }

    test("unaryMinus negates the physical amount") {
      val a = 123.nanometers
      (-a).toLong(nanometer) shouldBe -123L
      (-a) shouldNotBe a
    }

    test("isPositive returns true if strictly greater than zero") {
      1
        .nanometers
        .isPositive() shouldBe true
      0
        .nanometers
        .isPositive() shouldBe false
      (-1)
        .nanometers
        .isPositive() shouldBe false
    }

    test("isNegative returns true if strictly less than zero") {
      (-1)
        .nanometers
        .isNegative() shouldBe true
      0
        .nanometers
        .isNegative() shouldBe false
      1
        .nanometers
        .isNegative() shouldBe false
    }

    test("isZero returns true if exactly zero") {
      0
        .nanometers
        .isZero() shouldBe true
      1
        .nanometers
        .isZero() shouldBe false
      (-1)
        .nanometers
        .isZero() shouldBe false
    }

    test(
      "absoluteValue returns a non-negative quantity with the same magnitude",
    ) {
      123
        .nanometers.absoluteValue
        .toLong(nanometer) shouldBe 123L
      (-123)
        .nanometers.absoluteValue
        .toLong(nanometer) shouldBe 123L
      0
        .nanometers.absoluteValue
        .toLong(nanometer) shouldBe 0L
    }

    test("absoluteValue of Min Value throws ArithmeticException") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE.nanometers.absoluteValue
      }
    }
  })
