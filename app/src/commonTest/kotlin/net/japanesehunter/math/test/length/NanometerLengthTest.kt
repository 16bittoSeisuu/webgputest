package net.japanesehunter.math.test.length

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NanometerLengthTest :
  FunSpec({
    test("Unit conversion equality is based on the physical amount") {
      with(NanometerLength) {
        1000L.millimeters shouldBe 1.0.meters
      }
    }

    test("Double DSL rejects NaN") {
      with(NanometerLength) {
        shouldThrow<IllegalArgumentException> {
          Double.NaN.meters
        }
      }
    }

    test("Double DSL rejects infinity") {
      with(NanometerLength) {
        shouldThrow<IllegalArgumentException> {
          Double.POSITIVE_INFINITY.meters
        }
      }
    }

    test("Sign is preserved for the DSL") {
      with(NanometerLength) {
        (-1)
          .meters
          .toLong(meters) shouldBe -1L
      }
    }
  })


