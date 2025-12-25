package net.japanesehunter.traits.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.japanesehunter.traits.Entity
import net.japanesehunter.traits.HashMapEntityRegistry
import net.japanesehunter.traits.TraitKey

private interface HealthView {
  val hp: Int
}

private data class Health(
  override var hp: Int,
) : HealthView {
  companion object : TraitKey<HealthView, Health> by TraitKey()
}

private data class DamageEvent(
  val target: Entity,
  val amount: Int,
)

class EventSinkBuilderTest :
  FunSpec({
    test("onEach executes for each event when no bindings declared") {
      var callCount = 0
      val sink =
        buildEventSink<String> {
          onEach { callCount++ }
        }

      sink.onEvent("first")
      sink.onEvent("second")

      callCount shouldBe 2
    }

    test("onEach receives the event instance") {
      val received = mutableListOf<String>()
      val sink =
        buildEventSink<String> {
          onEach { event -> received.add(event) }
        }

      sink.onEvent("hello")
      sink.onEvent("world")

      received shouldBe listOf("hello", "world")
    }

    test("read binding resolves trait from entity property") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(100))

      var observedHp = -1
      val sink =
        buildEventSink<DamageEvent> {
          val target = DamageEvent::target
          val health by target.read(Health)
          onEach { observedHp = health.hp }
        }

      sink.onEvent(DamageEvent(entity, 10))

      observedHp shouldBe 100
    }

    test("read binding skips onEach when trait is missing") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()

      var callCount = 0
      val sink =
        buildEventSink<DamageEvent> {
          val target = DamageEvent::target
          val health by target.read(Health)
          onEach { callCount++ }
        }

      sink.onEvent(DamageEvent(entity, 10))

      callCount shouldBe 0
    }

    test("read binding throws when accessed outside onEach") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(100))

      lateinit var capturedHealth: () -> HealthView
      buildEventSink<DamageEvent> {
        val target = DamageEvent::target
        val health by target.read(Health)
        capturedHealth = { health }
        onEach { }
      }

      shouldThrow<IllegalStateException> {
        capturedHealth()
      }
    }
  })
