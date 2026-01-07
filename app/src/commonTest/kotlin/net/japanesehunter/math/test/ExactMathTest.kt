package net.japanesehunter.math.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.japanesehunter.math.test.ExactMath.minusExact
import net.japanesehunter.math.test.ExactMath.plusExact
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.ExactMath.timesExact

class ExactMathTest :
  FunSpec({
    test("addExact throws on positive overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE plusExact 1L
      }
    }

    test("addExact throws on negative overflow") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE plusExact -1L
      }
    }

    test("addExact returns the exact sum") {
      10L plusExact -3L shouldBe 7L
    }

    test("minusExact throws on negative overflow") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE minusExact 1L
      }
    }

    test("minusExact throws on positive overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE minusExact -1L
      }
    }

    test("minusExact returns the exact difference") {
      (-13L) minusExact 3L shouldBe -16L
    }

    test("multiplyExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE timesExact 2L
      }
    }

    test("multiplyExact returns the exact product") {
      6L timesExact -7L shouldBe -42L
    }

    test("scaleExact returns the exact product") {
      123_456_789_123_456_789L scaleExact 10.0 shouldBe
        1_234_567_891_234_567_890L
    }

    test("scaleExact returns the exact quotient and truncates towards zero") {
      123_456_789_123_456_789L scaleExact 0.1 shouldBe
        12_345_678_912_345_678L
    }

    test(
      "scaleExact returns the exact quotient with negative scale and " +
        "truncates towards zero",
    ) {
      (-123_456_789_123_456_789L) scaleExact 0.1 shouldBe
        -12_345_678_912_345_678L
    }

    test("scaleExact throws on non-finite scale") {
      shouldThrow<IllegalArgumentException> {
        123L scaleExact Double.NaN
      }
      shouldThrow<IllegalArgumentException> {
        123L scaleExact Double.POSITIVE_INFINITY
      }
      shouldThrow<IllegalArgumentException> {
        123L scaleExact Double.NEGATIVE_INFINITY
      }
    }

    test("scaleExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE scaleExact 2.0
      }
    }
  })


