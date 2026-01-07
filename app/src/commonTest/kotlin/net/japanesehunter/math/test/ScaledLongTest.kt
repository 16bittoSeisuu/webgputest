package net.japanesehunter.math.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScaledLongTest :
  FunSpec({
    test("scaleToLong rejects NaN") {
      shouldThrow<IllegalArgumentException> {
        ScaledLong.scaleToLong(1L, Double.NaN)
      }
    }

    test("scaleToLong rejects infinity") {
      shouldThrow<IllegalArgumentException> {
        ScaledLong.scaleToLong(1L, Double.POSITIVE_INFINITY)
      }
    }

    test("scaleToLong truncates toward zero") {
      ScaledLong.scaleToLong(3L, 0.5) shouldBe 1L
      ScaledLong.scaleToLong(-3L, 0.5) shouldBe -1L
      ScaledLong.scaleToLong(3L, -0.5) shouldBe -1L
      ScaledLong.scaleToLong(-3L, -0.5) shouldBe 1L
    }

    test("scaleToLong throws on overflow") {
      shouldThrow<ArithmeticException> {
        ScaledLong.scaleToLong(Long.MAX_VALUE, 2.0)
      }
    }

    test("scaleToLong supports Long.MIN_VALUE when the result fits") {
      ScaledLong.scaleToLong(Long.MIN_VALUE, 1.0) shouldBe Long.MIN_VALUE
    }
  })


