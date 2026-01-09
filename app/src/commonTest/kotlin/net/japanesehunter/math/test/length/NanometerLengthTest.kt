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

    test("Long DSL throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.meters
      }
    }

    test("Double DSL rounds to the nearest nanometer") {
      0.5
        .meters
        .toLong(nanometers) shouldBe 500_000_000L
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
      12_700L
        .micrometers
        .roundToLong(inches) shouldBe 1L
      (-12_700L)
        .micrometers
        .roundToLong(inches) shouldBe -1L
    }

    test("plus throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers + 1.nanometers
      }
    }

    test("plus returns the exact sum") {
      (1.meters + 2.meters).toLong(meters) shouldBe 3L
      ((-1).meters + 2.meters).toLong(meters) shouldBe 1L
    }

    test("minus throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE.nanometers - 1.nanometers
      }
    }

    test("minus returns the exact difference") {
      (3.meters - 2.meters).toLong(meters) shouldBe 1L
      ((-3).meters - 2.meters).toLong(meters) shouldBe -5L
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
      (3.meters * 2.0).toLong(meters) shouldBe 6L
      (3.meters * -2.0).toLong(meters) shouldBe -6L
    }

    test("times(Long) throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE.nanometers * 2L
      }
    }

    test("times(Long) scales the physical amount") {
      (3.meters * 2L).toLong(meters) shouldBe 6L
      (3.meters * -2L).toLong(meters) shouldBe -6L
    }

    test("times works with other numeric types") {
      (3.meters * 2).toLong(meters) shouldBe 6L // Int
      (3.meters * 2.toShort()).toLong(meters) shouldBe 6L // Short
      (3.meters * 2.toByte()).toLong(meters) shouldBe 6L // Byte
      (3.meters * 2.0f).toLong(meters) shouldBe 6L // Float
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
      (10.nanometers / 2.0).toLong(nanometers) shouldBe 5L
      (10.nanometers / -2.0).toLong(nanometers) shouldBe -5L
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
      (10.nanometers / 2L).toLong(nanometers) shouldBe 5L
      (10.nanometers / -2L).toLong(nanometers) shouldBe -5L
    }

    test("div works with other numeric types") {
      (10.nanometers / 2).toLong(nanometers) shouldBe 5L // Int
      (10.nanometers / 2.toShort()).toLong(nanometers) shouldBe 5L // Short
      (10.nanometers / 2.toByte()).toLong(nanometers) shouldBe 5L // Byte
      (10.nanometers / 2.0f).toLong(nanometers) shouldBe 5L // Float
    }

    test("toInt keeps the lower 32 bits") {
      0x1_0000_0001L
        .nanometers
        .toInt(nanometers) shouldBe 1
    }

    test("roundToInt delegates to roundToLong and keeps the lower 32 bits") {
      12_700L
        .micrometers
        .roundToInt(inches) shouldBe 1
    }

    test("toShort keeps the lower 16 bits") {
      0x1_0001L
        .nanometers
        .toShort(nanometers) shouldBe 1
    }

    test("toByte keeps the lower 8 bits") {
      0x101L
        .nanometers
        .toByte(nanometers) shouldBe 1
    }

    test("unaryPlus returns an equal value") {
      val a = 123.nanometers
      +a shouldBe a
    }

    test("unaryMinus negates the physical amount") {
      val a = 123.nanometers
      (-a).toLong(nanometers) shouldBe -123L
      (-a) shouldNotBe a
    }
  })
