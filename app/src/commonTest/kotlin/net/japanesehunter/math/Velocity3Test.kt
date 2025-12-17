package net.japanesehunter.math

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Velocity3Test :
  FunSpec({
    test("Velocity3.zero is the zero vector") {
      Velocity3.zero shouldBe Velocity3(vx = Length.ZERO, vy = Length.ZERO, vz = Length.ZERO)
    }

    test("Velocity3 plus and minus are component-wise") {
      val a = Velocity3(vx = 1.meters, vy = 2.meters, vz = 3.meters)
      val b = Velocity3(vx = (-4).meters, vy = 5.meters, vz = 0.meters)

      a + b shouldBe Velocity3(vx = (-3).meters, vy = 7.meters, vz = 3.meters)
      a - b shouldBe Velocity3(vx = 5.meters, vy = (-3).meters, vz = 3.meters)
    }

    test("Velocity3 times Duration yields displacement") {
      val v = Velocity3(vx = 2.meters, vy = (-1).meters, vz = 0.meters)

      v * 500.milliseconds shouldBe Length3(dx = 1.meters, dy = (-0.5).meters, dz = 0.meters)
    }

    test("Velocity3 times Duration composes") {
      val v = Velocity3(vx = 3.meters, vy = 0.meters, vz = (-2).meters)
      val t1 = 0.25.seconds
      val t2 = 0.75.seconds

      v * (t1 + t2) shouldBe (v * t1) + (v * t2)
    }
  })
