package net.japanesehunter.traits.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
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
          onEach { event ->
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
  })
