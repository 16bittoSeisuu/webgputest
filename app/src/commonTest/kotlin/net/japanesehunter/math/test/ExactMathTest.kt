package net.japanesehunter.math.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExactMathTest :
  FunSpec({
    test("addExact throws on positive overflow") {
      shouldThrow<ArithmeticException> {
        ExactMath.addExact(Long.MAX_VALUE, 1L)
      }
    }

    test("addExact throws on negative overflow") {
      shouldThrow<ArithmeticException> {
        ExactMath.addExact(Long.MIN_VALUE, -1L)
      }
    }

    test("addExact returns the exact sum") {
      ExactMath.addExact(10L, -3L) shouldBe 7L
    }

    test("multiplyExact throws on overflow") {
      shouldThrow<ArithmeticException> {
        ExactMath.multiplyExact(Long.MAX_VALUE, 2L)
      }
    }

    test("multiplyExact returns the exact product") {
      ExactMath.multiplyExact(6L, -7L) shouldBe -42L
    }
  })


