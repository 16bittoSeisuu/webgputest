package net.japanesehunter.math

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AabbTest :
  FunSpec({
    fun p(
      xMeters: Int,
      yMeters: Int,
      zMeters: Int,
    ): ImmutablePoint3 =
      Point3(
        x = xMeters.meters,
        y = yMeters.meters,
        z = zMeters.meters,
      )

    fun d(
      xMeters: Int,
      yMeters: Int,
      zMeters: Int,
    ): ImmutableLength3 =
      Length3(
        dx = xMeters.meters,
        dy = yMeters.meters,
        dz = zMeters.meters,
      )

    test("Aabb normalizes min/max component-wise") {
      val a =
        Aabb(
          min = p(1, 2, 3),
          max = p(-1, 0, 5),
        )

      a.min shouldBe p(-1, 0, 3)
      a.max shouldBe p(1, 2, 5)
    }

    test("MutableAabb normalizes on construction and assignment") {
      val a =
        MutableAabb(
          min = p(2, 0, 0),
          max = p(1, 0, 0),
        )

      a.min shouldBe p(1, 0, 0)
      a.max shouldBe p(2, 0, 0)

      a.min = p(5, 0, 0)

      a.min shouldBe p(2, 0, 0)
      a.max shouldBe p(5, 0, 0)

      a.max = p(4, 0, 0)

      a.min shouldBe p(2, 0, 0)
      a.max shouldBe p(4, 0, 0)
    }

    test("Aabb.intersects is symmetric") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val b = Aabb(min = p(1, 0, 0), max = p(2, 1, 1))

      a.intersects(b) shouldBe true
      b.intersects(a) shouldBe true
    }

    test("Aabb.intersects treats touching faces as intersection") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val b = Aabb(min = p(1, 0, 0), max = p(2, 1, 1))

      a.intersects(b) shouldBe true
    }

    test("Aabb.intersects returns false when separated") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val b = Aabb(min = p(2, 0, 0), max = p(3, 1, 1))

      a.intersects(b) shouldBe false
    }

    test("Aabb.overlaps returns false when only touching") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val b = Aabb(min = p(1, 0, 0), max = p(2, 1, 1))

      a.overlaps(b) shouldBe false
      b.overlaps(a) shouldBe false
    }

    test("Aabb.overlaps returns true when volumes intersect") {
      val a = Aabb(min = p(0, 0, 0), max = p(2, 2, 2))
      val b = Aabb(min = p(1, 1, 1), max = p(3, 3, 3))

      a.overlaps(b) shouldBe true
      b.overlaps(a) shouldBe true
    }

    test("Aabb.overlaps returns false when separated") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val b = Aabb(min = p(2, 0, 0), max = p(3, 1, 1))

      a.overlaps(b) shouldBe false
    }

    test("Aabb.size is non-negative and equals max - min") {
      val a = Aabb(min = p(5, 2, 1), max = p(1, 4, 3))
      a.size shouldBe d(4, 2, 2)
    }

    test("Aabb.translatedBy preserves size") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 2, 3))
      val moved = a.translatedBy(d(10, -5, 2))
      moved.size shouldBe a.size
    }

    test("Aabb.translatedBy composes") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val d1 = d(1, 2, 3)
      val d2 = d(-4, 0, 7)

      a
        .translatedBy(d1)
        .translatedBy(d2) shouldBe a.translatedBy(d1 + d2)
    }

    test("MutableAabb.translateBy matches Aabb.translatedBy") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 2, 3))
      val moved = a.translatedBy(d(3, 4, 5))

      val mutable = MutableAabb.copyOf(a)
      mutable.translateBy(d(3, 4, 5))
      Aabb.copyOf(mutable) shouldBe moved
    }

    test("Aabb.expandedBy expands in every direction") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      val expanded = a.expandedBy(2.meters)

      expanded.min shouldBe p(-2, -2, -2)
      expanded.max shouldBe p(3, 3, 3)
    }

    test("Aabb.expandedBy with zero padding is identity") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 2, 3))
      a.expandedBy(Length.ZERO) shouldBe a
    }

    test("Aabb.expandedBy rejects negative padding") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 1, 1))
      shouldThrow<IllegalArgumentException> {
        a.expandedBy((-1).meters)
      }

      shouldThrow<IllegalArgumentException> {
        a.expandedBy(
          Length3(dx = (-1).meters, dy = Length.ZERO, dz = Length.ZERO),
        )
      }
    }

    test("Aabb.sweptBy contains both start and end AABBs") {
      val a = Aabb(min = p(0, 0, 0), max = p(1, 2, 3))
      val delta = d(10, -5, 2)
      val end = a.translatedBy(delta)
      val swept = a.sweptBy(delta)

      (swept.min.x <= a.min.x) shouldBe true
      (swept.min.y <= a.min.y) shouldBe true
      (swept.min.z <= a.min.z) shouldBe true

      (swept.max.x >= a.max.x) shouldBe true
      (swept.max.y >= a.max.y) shouldBe true
      (swept.max.z >= a.max.z) shouldBe true

      (swept.min.x <= end.min.x) shouldBe true
      (swept.min.y <= end.min.y) shouldBe true
      (swept.min.z <= end.min.z) shouldBe true

      (swept.max.x >= end.max.x) shouldBe true
      (swept.max.y >= end.max.y) shouldBe true
      (swept.max.z >= end.max.z) shouldBe true
    }
  })
