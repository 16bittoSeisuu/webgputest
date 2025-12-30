@file:Suppress("UnusedVariable", "unused", "ControlFlowWithEmptyBody")

package net.japanesehunter.traits.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import net.japanesehunter.traits.Entity
import net.japanesehunter.traits.HashMapEntityRegistry
import net.japanesehunter.traits.TraitKey

private data class Position(
  val x: Double,
  val y: Double,
) {
  companion object : TraitKey<Position, Position> by TraitKey()
}

private data class Name(val value: String) {
  companion object : TraitKey<Name, Name> by TraitKey()
}

private data class TickEvent(val dt: Double)

private data class ProximityEvent(
  val center: Entity,
  val radius: Double,
)

class QueryingEventSinkBuilderTest :
  FunSpec({
    test("query has returns entities with required trait") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      val e3 = registry.createEntity()
      e1.add(Position(1.0, 2.0))
      e2.add(Position(3.0, 4.0))
      e3.add(Name("no position"))

      val observed = mutableListOf<Position>()
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query().has(Position)
          onEach {
            for (entity in entities) {
              val pos = entity.get(Position.writableType)!!
              observed.add(pos)
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      observed shouldContainExactlyInAnyOrder
        listOf(
          Position(1.0, 2.0),
          Position(3.0, 4.0),
        )
    }

    test("query filter references event-bound value") {
      val registry = HashMapEntityRegistry()
      val origin = registry.createEntity()
      val near = registry.createEntity()
      val far = registry.createEntity()
      origin.add(Position(0.0, 0.0))
      near.add(Position(1.0, 1.0))
      far.add(Position(100.0, 100.0))

      val observed = mutableListOf<Position>()
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val entities =
            query().has(Position) { pos ->
              val dx = pos.x - centerPos.x
              val dy = pos.y - centerPos.y
              dx * dx + dy * dy <= 10.0 * 10.0
            }
          onEach {
            for (entity in entities) {
              val pos = entity.get(Position.writableType)!!
              observed.add(pos)
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(origin, 10.0))

      observed shouldContainExactlyInAnyOrder
        listOf(
          Position(0.0, 0.0),
          Position(1.0, 1.0),
        )
    }

    test("query read returns trait from query result entity") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      e1.add(Position(1.0, 2.0))
      e1.add(Name("first"))
      e2.add(Position(3.0, 4.0))
      e2.add(Name("second"))

      val observed = mutableListOf<Pair<Position, Name>>()
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities =
            query()
              .has(Position)
              .has(Name)
          val pos by entities.read(Position)
          val name by entities.read(Name)
          onEach {
            for (entity in entities) {
              observed.add(pos to name)
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      observed shouldContainExactlyInAnyOrder
        listOf(
          Position(1.0, 2.0) to Name("first"),
          Position(3.0, 4.0) to Name("second"),
        )
    }

    test("multiple queries with different conditions return correct results") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      val e3 = registry.createEntity()
      e1.add(Position(1.0, 0.0))
      e1.add(Name("has both"))
      e2.add(Position(2.0, 0.0))
      e3.add(Name("name only"))

      val positionOnly = mutableListOf<Position>()
      val nameOnly = mutableListOf<Name>()
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entitiesWithPosition = query().has(Position)
          val entitiesWithName = query().has(Name)
          val pos by entitiesWithPosition.read(Position)
          val name by entitiesWithName.read(Name)
          onEach {
            for (entity in entitiesWithPosition) {
              positionOnly.add(pos)
            }
            for (entity in entitiesWithName) {
              nameOnly.add(name)
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      positionOnly shouldContainExactlyInAnyOrder
        listOf(
          Position(1.0, 0.0),
          Position(2.0, 0.0),
        )
      nameOnly shouldContainExactlyInAnyOrder
        listOf(
          Name("has both"),
          Name("name only"),
        )
    }

    test(
      "query results are evaluated once per event even with multiple iterations",
    ) {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      e1.add(Position(1.0, 2.0))

      var queryExecutionCount = 0
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities =
            query().has(Position) { pos ->
              queryExecutionCount++
              true
            }
          onEach {
            // First iteration
            for (entity in entities) {
              // consume
            }
            // Second iteration - should not re-execute query filter
            for (entity in entities) {
              // consume
            }
            // Third iteration
            for (entity in entities) {
              // consume
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      queryExecutionCount shouldBe 1
    }

    test("query read throws when accessed outside iteration") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      e1.add(Position(1.0, 2.0))

      var caughtException: IllegalStateException? = null
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query()
          val pos by entities.read(Position)
          onEach {
            try {
              val unused = pos
            } catch (e: IllegalStateException) {
              caughtException = e
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      caughtException shouldNotBe null
      caughtException!!.message shouldContain "iteration"
    }

    test(
      "query readOptional returns value when trait exists and null when missing",
    ) {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      e1.add(Position(1.0, 2.0))
      e1.add(Name("has name"))
      e2.add(Position(3.0, 4.0))

      val observed = mutableListOf<Pair<Position, Name?>>()
      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query()
          val pos by entities.read(Position)
          val name by entities.readOptional(Name)
          onEach {
            for (entity in entities) {
              observed.add(pos to name)
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      observed shouldContainExactlyInAnyOrder
        listOf(
          Position(1.0, 2.0) to Name("has name"),
          Position(3.0, 4.0) to null,
        )
    }

    test("nested loop restores outer loop context") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      e1.add(Position(1.0, 0.0))
      e2.add(Position(2.0, 0.0))

      val outerPositions = mutableListOf<Position>()

      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query()
          val pos by entities.read(Position)
          onEach { event ->
            for (outer in entities) {
              // Inner loop iterates same query
              for (inner in entities) {
                // do nothing
              }
              // After the inner loop finishes, pos should refer to the outer entity
              outerPositions.add(pos)
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      outerPositions shouldContainExactlyInAnyOrder
        listOf(
          Position(1.0, 0.0),
          Position(2.0, 0.0),
        )
    }

    test("break from loop leaves context active but cleared on event end") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      e1.add(Position(1.0, 0.0))

      var posAfterBreak: Position? = null
      var exception: Exception? = null

      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query()
          val pos by entities.read(Position)
          onEach {
            for (entity in entities) {
              break // Break immediately
            }
            // Context remains active after the break (expected behavior)
            try {
              posAfterBreak = pos
            } catch (e: Exception) {
              exception = e
            }
          }
        }(registry)

      sink.onEvent(TickEvent(0.016))

      // After break, context is still active (expected behavior)
      exception shouldBe null
      posAfterBreak shouldBe Position(1.0, 0.0)
    }

    // event and query binding execution matrix

    test("event read missing skips onEach regardless of query") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      // center has NO Position
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))

      var onEachCount = 0
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val entities = query().has(Position)
          onEach {
            onEachCount++
            for (entity in entities) {
              // consume
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 0
    }

    test("event readOptional missing still executes onEach") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      // center has NO Name
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))

      var onEachCount = 0
      var observedName: Name? = Name("sentinel")
      val sink =
        buildQueryingEventSink {
          val name by ProximityEvent::center.readOptional(Name)
          val entities = query().has(Position)
          onEach {
            onEachCount++
            observedName = name
            for (entity in entities) {
              // consume
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 1
      observedName shouldBe null
    }

    test("event read present and query read missing throws") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Position(0.0, 0.0))
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))
      // other has NO Name

      var onEachCount = 0
      var caughtException: IllegalStateException? = null
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val entities = query().has(Position)
          val name by entities.read(Name)
          onEach {
            onEachCount++
            for (entity in entities) {
              try {
                val unused = name
              } catch (e: IllegalStateException) {
                caughtException = e
              }
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 1
      caughtException shouldNotBe null
      caughtException!!.message shouldContain "iteration"
    }

    test("event read present and query readOptional missing returns null") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Position(0.0, 0.0))
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))
      // other has NO Name

      var onEachCount = 0
      val observedNames = mutableListOf<Name?>()
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val entities = query().has(Position)
          val name by entities.readOptional(Name)
          onEach {
            onEachCount++
            for (entity in entities) {
              observedNames.add(name)
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 1
      observedNames shouldContainExactlyInAnyOrder listOf(null, null)
    }

    test("both event and query bindings present resolves all") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Position(0.0, 0.0))
      center.add(Name("center"))
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))
      other.add(Name("other"))

      var onEachCount = 0
      val observed = mutableListOf<Triple<Position, Name, Name>>()
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val centerName by ProximityEvent::center.read(Name)
          val entities = query().has(Position)
          val otherName by entities.read(Name)
          onEach {
            onEachCount++
            for (entity in entities) {
              observed.add(Triple(centerPos, centerName, otherName))
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 1
      observed shouldContainExactlyInAnyOrder
        listOf(
          Triple(Position(0.0, 0.0), Name("center"), Name("center")),
          Triple(Position(0.0, 0.0), Name("center"), Name("other")),
        )
    }

    test("event readOptional present and query read missing throws") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Name("center"))
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))
      // other has NO Name

      var onEachCount = 0
      var caughtException: IllegalStateException? = null
      val sink =
        buildQueryingEventSink {
          val centerName by ProximityEvent::center.readOptional(Name)
          val entities = query().has(Position)
          val name by entities.read(Name)
          onEach {
            onEachCount++
            for (entity in entities) {
              try {
                val unused = name
              } catch (e: IllegalStateException) {
                caughtException = e
              }
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 1
      caughtException shouldNotBe null
      caughtException!!.message shouldContain "iteration"
    }

    test("multiple event bindings with one missing skips onEach") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Position(0.0, 0.0))
      // center has NO Name
      val other = registry.createEntity()
      other.add(Position(1.0, 2.0))
      other.add(Name("other"))

      var onEachCount = 0

      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val centerName by ProximityEvent::center.read(Name) // Missing
          val entities = query().has(Position)
          val otherName by entities.read(Name)
          onEach {
            onEachCount++
            for (entity in entities) {
              // consume
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      onEachCount shouldBe 0
    }

    test("multiple query bindings with one missing throws") {
      val registry = HashMapEntityRegistry()
      val center = registry.createEntity()
      center.add(Position(0.0, 0.0))
      val withBoth = registry.createEntity()
      withBoth.add(Position(1.0, 2.0))
      withBoth.add(Name("withBoth"))
      val posOnly = registry.createEntity()
      posOnly.add(Position(3.0, 4.0))
      // posOnly has NO Name, center also has NO Name

      val observed = mutableListOf<Pair<Position, Name>>()
      var exceptionCount = 0
      val sink =
        buildQueryingEventSink {
          val centerPos by ProximityEvent::center.read(Position)
          val entities = query()
          val pos by entities.read(Position)
          val name by entities.read(Name)
          onEach {
            for (entity in entities) {
              try {
                observed.add(pos to name)
              } catch (e: IllegalStateException) {
                exceptionCount++
              }
            }
          }
        }(registry)

      sink.onEvent(ProximityEvent(center, 10.0))

      // Only withBoth has Name, so only that succeeds
      observed shouldContainExactlyInAnyOrder
        listOf(Position(1.0, 2.0) to Name("withBoth"))
      // Two entities (center and posOnly) throw exceptions
      exceptionCount shouldBe 2
    }
  })
