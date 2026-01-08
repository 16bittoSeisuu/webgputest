package net.japanesehunter.math.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import korlibs.time.seconds
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import net.japanesehunter.math.test.ExactMath.descaleExact
import net.japanesehunter.math.test.ExactMath.minusExact
import net.japanesehunter.math.test.ExactMath.negateExact
import net.japanesehunter.math.test.ExactMath.plusExact
import net.japanesehunter.math.test.ExactMath.reciprocalExact
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.ExactMath.timesExact

class ExactMathTest :
  FunSpec({
    // region addExact
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
    // endregion
    // region negateExact
    test("negateExact returns the exact negation") {
      1L.negateExact() shouldBe -1L
      (-1L).negateExact() shouldBe 1L
    }

    test("negateExact should throw on Long.MIN_VALUE") {
      shouldThrow<ArithmeticException> {
        Long.MIN_VALUE
          .negateExact()
      }
    }
    // endregion
    // region minusExact
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

    test("minusExact throws on overflow with Long.MIN_VALUE subtrahend") {
      shouldThrow<ArithmeticException> {
        0L minusExact Long.MIN_VALUE
      }
    }

    test(
      "minusExact returns the correct difference with " +
        "Long.MIN_VALUE subtrahend",
    ) {
      (-1L) minusExact Long.MIN_VALUE shouldBe Long.MAX_VALUE
    }
    // endregion
    // region timesExact
    test("timesExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE timesExact 2L
      }
    }

    test("timesExact returns the exact product") {
      6L timesExact -7L shouldBe -42L
    }
    // endregion
    // region scaleExact
    test("scaleExact returns the exact product") {
      123_456_789_123_456_789L scaleExact 10.0 shouldBe
        1_234_567_891_234_567_890L
    }

    test("scaleExact returns the exact quotient and truncates towards zero") {
      123_456_789_123_456_789L scaleExact 0.2 shouldBe
        24_691_357_824_691_357L
    }

    test(
      "scaleExact returns the exact quotient with negative scale and " +
        "truncates towards zero",
    ) {
      (-123_456_789_123_456_789L) scaleExact 0.2 shouldBe
        -24_691_357_824_691_357L
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

    test("scaleExact returns correctly for negative left operand") {
      (-123L) scaleExact (10.0) shouldBe -1230L
    }

    test("scaleExact returns correctly for negative right operand") {
      (123L) scaleExact (-10.0) shouldBe -1230L
    }

    test("scaleExact returns correctly for negative operands") {
      (-123L) scaleExact (-10.0) shouldBe 1230L
    }

    test("scaleExact doesn't take too long") {
      withTimeout(1.seconds) {
        for (_ in 0 until 1_000) {
          for (_ in 0 until 1_000) {
            val _ = 1_000_000_000_000_000L scaleExact 99.0
          }
          yield()
        }
      }
      withTimeout(1.seconds) {
        for (_ in 0 until 1_000) {
          for (_ in 0 until 100) {
            val _ = 1_000_000_000_000_000L scaleExact 99.9
          }
        }
      }
    }
    // endregion
    // region descaleExact
    test("descaleExact returns the exact quotient") {
      123_456_789_123_456_789L descaleExact 5.0 shouldBe
        24_691_357_824_691_357L
    }

    test(
      "descaleExact returns the exact quotient and " +
        "truncates towards zero",
    ) {
      123_456_789_123_456_789L descaleExact 5.5 shouldBe
        22_446_688_931_537_598L
    }

    test(
      "descaleExact returns the exact quotient with negative divisor and " +
        "truncates towards zero",
    ) {
      (-123_456_789_123_456_789L) descaleExact 5.0 shouldBe
        -24_691_357_824_691_357L
    }

    test("descaleExact returns the exact quotient with negative dividend") {
      (-123_456_789_123_456_789L) descaleExact (-5.0) shouldBe
        24_691_357_824_691_357L
    }

    test("descaleExact throws on NaN divisor") {
      shouldThrow<IllegalArgumentException> {
        123L descaleExact Double.NaN
      }
    }

    test("descaleExact throws on positive infinity divisor") {
      shouldThrow<IllegalArgumentException> {
        123L descaleExact Double.POSITIVE_INFINITY
      }
    }

    test("descaleExact throws on negative infinity divisor") {
      shouldThrow<IllegalArgumentException> {
        123L descaleExact Double.NEGATIVE_INFINITY
      }
    }

    test("descaleExact throws on zero divisor") {
      shouldThrow<IllegalArgumentException> {
        123L descaleExact 0.0
      }
    }

    test("descaleExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE descaleExact 0.5
      }
    }

    test("descaleExact throws on overflow with small divisor") {
      shouldThrow<ArithmeticException> {
        Long.MAX_VALUE descaleExact Double.MIN_VALUE
      }
    }

    test("descaleExact returns zero when dividend is zero") {
      0L descaleExact 5.0 shouldBe 0L
    }

    test("descaleExact returns correctly for negative dividend") {
      (-123L) descaleExact (5.0) shouldBe -24L
    }

    test("descaleExact returns correctly for negative divisor") {
      (123L) descaleExact (-5.0) shouldBe -24L
    }

    test("descaleExact returns correctly for negative operands") {
      (-123L) descaleExact (-5.0) shouldBe 24L
    }
    // endregion
    // region reciprocalExact
    test("reciprocalExact returns the exact reciprocal") {
      5.0.reciprocalExact() shouldBe 0.2
    }

    test("reciprocalExact throws on zero") {
      shouldThrow<IllegalArgumentException> {
        0.0.reciprocalExact()
      }
    }

    test("reciprocalExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        Double.MIN_VALUE
          .reciprocalExact()
      }
    }

    test("reciprocalExact returns the exact reciprocal of negative number") {
      (-5.0).reciprocalExact() shouldBe -0.2
    }
    // endregion
  })
