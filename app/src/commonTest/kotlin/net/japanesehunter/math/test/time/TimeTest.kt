package net.japanesehunter.math.test.time

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MICROSECONDS
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.NANOSECONDS
import kotlin.time.DurationUnit.SECONDS

class TimeTest :
  FunSpec({
    test("canonicalUnit is second") {
      Time.canonicalUnit shouldBe second
    }

    test("second is base unit") {
      second.thisToCanonicalFactor shouldBe 1.0
    }

    test("minute conversion") {
      val min = 1.minutes
      second per minute shouldBe min.toDouble(SECONDS)
    }

    test("hour conversion") {
      val hr = 1.hours
      minute per hour shouldBe hr.toDouble(MINUTES)
    }

    test("day conversion") {
      val d = 1.days
      hour per day shouldBe d.toDouble(HOURS)
    }

    test("millisecond conversion") {
      val sec = 1.seconds
      millisecond per second shouldBe sec.toDouble(MILLISECONDS)
    }

    test("microsecond conversion") {
      val ms = 1.milliseconds
      microsecond per millisecond shouldBe ms.toDouble(MICROSECONDS)
    }

    test("nanosecond conversion") {
      val us = 1.microseconds
      nanosecond per microsecond shouldBe us.toDouble(NANOSECONDS)
    }
  })
