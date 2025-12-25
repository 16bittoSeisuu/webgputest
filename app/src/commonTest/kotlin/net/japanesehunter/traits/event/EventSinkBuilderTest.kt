package net.japanesehunter.traits.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

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
  })
