package net.japanesehunter.math.test.length

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.japanesehunter.math.test.length.NanometerLength.Companion.meters
import net.japanesehunter.math.test.length.NanometerLength.Companion.millimeters

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
  })


