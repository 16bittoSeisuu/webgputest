package net.japanesehunter.traits.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
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
  })
