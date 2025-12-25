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

private data class Name(
  val value: String,
) {
  companion object : TraitKey<Name, Name> by TraitKey()
}

private data class TickEvent(
  val dt: Double,
)

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
          onEach { event ->
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
        buildQueryingEventSink<ProximityEvent> {
          val centerPos by ProximityEvent::center.read(Position)
          val entities =
            query().has(Position) { pos ->
              val dx = pos.x - centerPos.x
              val dy = pos.y - centerPos.y
              dx * dx + dy * dy <= 10.0 * 10.0
            }
          onEach { event ->
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
          val entities = query().has(Position).has(Name)
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

    test("query results are evaluated once per event even with multiple iterations") {
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
          onEach { event ->
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
          val entities = query().has(Position)
          val pos by entities.read(Position)
          onEach {
            try {
              @Suppress("unused", "UNUSED_VARIABLE")
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

    test("nested loop restores outer loop context") {
      val registry = HashMapEntityRegistry()
      val e1 = registry.createEntity()
      val e2 = registry.createEntity()
      e1.add(Position(1.0, 0.0))
      e2.add(Position(2.0, 0.0))

      val outerPositions = mutableListOf<Position>()

      val sink =
        buildQueryingEventSink<TickEvent> {
          val entities = query().has(Position)
          val pos by entities.read(Position)
          onEach { event ->
            for (outer in entities) {
              // Inner loop iterates same query
              for (inner in entities) {
                // do nothing
              }
              // After inner loop finishes, pos should refer to outer entity
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
          val entities = query().has(Position)
          val pos by entities.read(Position)
          onEach {
            for (entity in entities) {
              break // Break immediately
            }
            // Context remains active after break (expected behavior)
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
  })
