package net.japanesehunter.math

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Velocity3Test :
  FunSpec({
    test("Velocity3.zero is the zero vector") {
      Velocity3.zero shouldBe Velocity3(vx = Speed.ZERO, vy = Speed.ZERO, vz = Speed.ZERO)
    }

    test("Velocity3 plus and minus are component-wise") {
      val a = Velocity3(vx = 1.metersPerSecond, vy = 2.metersPerSecond, vz = 3.metersPerSecond)
      val b = Velocity3(vx = (-4).metersPerSecond, vy = 5.metersPerSecond, vz = 0.metersPerSecond)

      a + b shouldBe Velocity3(vx = (-3).metersPerSecond, vy = 7.metersPerSecond, vz = 3.metersPerSecond)
      a - b shouldBe Velocity3(vx = 5.metersPerSecond, vy = (-3).metersPerSecond, vz = 3.metersPerSecond)
    }

    test("Velocity3 times Duration yields displacement") {
      val v = Velocity3(vx = 2.metersPerSecond, vy = (-1).metersPerSecond, vz = 0.metersPerSecond)

      v * 500.milliseconds shouldBe Length3(dx = 1.meters, dy = (-0.5).meters, dz = 0.meters)
    }

    test("Velocity3 times Duration composes") {
      val v = Velocity3(vx = 3.metersPerSecond, vy = 0.metersPerSecond, vz = (-2).metersPerSecond)
      val t1 = 0.25.seconds
      val t2 = 0.75.seconds

      v * (t1 + t2) shouldBe (v * t1) + (v * t2)
    }
  })
