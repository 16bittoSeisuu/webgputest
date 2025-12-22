package net.japanesehunter.traits

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

private interface CounterView {
  val count: Int
}

private data class Counter(
  override var count: Int,
) : CounterView {
  companion object : TraitKey<CounterView, Counter> by TraitKey()
}

private interface LabelView {
  val label: String
}

private data class Label(
  override val label: String,
) : LabelView {
  companion object : TraitKey<LabelView, Label> by TraitKey()
}

private data class WrappedInt(
  val value: Int,
)

private object WrappedIntKey : TraitKey<Int, WrappedInt> by TraitKey({ it.value })

class SystemBuilderTest :
  FunSpec({
    test("forEach executes once per matching entity") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.create()
      val e2 = registry.create()
      val e3 = registry.create()
      registry.add(e1, Counter(1))
      registry.add(e2, Counter(2))
      registry.add(e3, Label("no counter"))

      val observed = mutableListOf<Int>()
      val sink =
        buildSystem {
          val counter by read(Counter)
          forEach { _ ->
            observed.add(counter.count)
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      observed.toSet() shouldBe setOf(1, 2)
    }

    test("forEach skips entities missing required traits") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.create()
      val e2 = registry.create()
      registry.add(e1, Counter(1))
      registry.add(e1, Label("a"))
      registry.add(e2, Counter(2))

      var callCount = 0
      val sink =
        buildSystem {
          val counter by read(Counter)
          val label by read(Label)
          forEach { _ ->
            callCount++
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      callCount shouldBe 1
    }

    test("read returns correct value for each entity") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.create()
      val e2 = registry.create()
      registry.add(e1, Counter(10))
      registry.add(e2, Counter(20))

      val observed = mutableListOf<Int>()
      val sink =
        buildSystem {
          val counter by read(Counter)
          forEach { _ ->
            observed.add(counter.count)
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      observed.toSet() shouldBe setOf(10, 20)
    }

    test("read applies provideReadonlyView transformation") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, WrappedInt(42))

      var observed: Int? = null
      val sink =
        buildSystem {
          val value by read(WrappedIntKey)
          forEach { _ ->
            observed = value
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      observed shouldBe 42
    }

    test("write getValue returns writable instance") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(5))

      var writtenValue: Counter? = null
      val sink =
        buildSystem {
          var counter by write(Counter)
          forEach { _ ->
            writtenValue = counter
            counter.count = 99
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      writtenValue?.count shouldBe 99
      registry.get(entity, Counter::class)?.count shouldBe 99
    }

    test("write setValue replaces trait in registry") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(1))

      val sink =
        buildSystem {
          var counter by write(Counter)
          forEach { _ ->
            counter = Counter(100)
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      registry.get(entity, Counter::class)?.count shouldBe 100
    }

    test("dt is passed to forEach block") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(0))

      var receivedDt = 0.milliseconds
      val sink =
        buildSystem {
          val counter by read(Counter)
          forEach { dt ->
            receivedDt = dt
          }
        }

      sink.onEvent(TraitUpdateEvent(registry, 33.milliseconds))

      receivedDt shouldBe 33.milliseconds
    }

    test("onEvent does nothing when no forEach is defined") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(0))

      var forEachExecuted = false
      val sink =
        buildSystem {
          val counter by read(Counter)
        }

      sink.onEvent(TraitUpdateEvent(registry, 16.milliseconds))

      forEachExecuted shouldBe false
    }

    test("read outside forEach throws error") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(0))

      val sink =
        buildSystem {
          val counter by read(Counter)
          shouldThrow<IllegalStateException> {
            @Suppress("UNUSED_EXPRESSION")
            counter
          }
        }
    }

    test("write outside forEach throws error") {
      val registry = HashMapEntityRegistry()
      val entity = registry.create()
      registry.add(entity, Counter(0))

      val sink =
        buildSystem {
          var counter by write(Counter)
          shouldThrow<IllegalStateException> {
            @Suppress("UNUSED_EXPRESSION")
            counter
          }
        }
    }
  })
