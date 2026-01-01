package net.japanesehunter.traits

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private data class Health(var value: Int)

private data class Name(val text: String)

class EntityTest :
  FunSpec({
    test("createEntity returns a living entity") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.isAlive shouldBe true
    }

    test("add and get return the trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity
        .get(Health::class)
        ?.value shouldBe 1
    }

    test("add and get with reified extension") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(42))
      entity
        .get<Health>()
        ?.value shouldBe 42
    }

    test("add replaces existing trait of the same type") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity.add(Health(2))
      entity
        .get<Health>()
        ?.value shouldBe 2
    }

    test("has returns false before adding trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.has(Health::class) shouldBe false
    }

    test("has returns true after adding trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity.has(Health::class) shouldBe true
    }

    test("has returns false after removing trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity.remove(Health::class)
      entity.has(Health::class) shouldBe false
    }

    test("has with reified extension") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.has<Health>() shouldBe false
      entity.add(Health(1))
      entity.has<Health>() shouldBe true
    }

    test("remove returns the removed trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(99))
      val removed = entity.remove(Health::class)
      removed?.value shouldBe 99
    }

    test("remove returns null when trait not present") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.remove(Health::class) shouldBe null
    }

    test("destroy makes isAlive false") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      entity.isAlive shouldBe false
    }

    test("add throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      val exception =
        runCatching { entity.add(Health(1)) }
          .exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }

    test("get throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity.destroy()
      val exception =
        runCatching { entity.get(Health::class) }
          .exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }

    test("remove throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      entity.destroy()
      val exception =
        runCatching { entity.remove(Health::class) }
          .exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }

    test("has throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      val exception =
        runCatching { entity.has(Health::class) }
          .exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }

    test("destroy throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      val exception =
        runCatching { entity.destroy() }
          .exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }

    test("exception message contains destroyed") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      val exception =
        runCatching { entity.add(Health(1)) }
          .exceptionOrNull()
      (
        exception
          ?.message
          ?.contains("destroyed") == true
      ) shouldBe true
    }

    test("same entity obtained from query equals itself") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      val queried =
        registry
          .query(Health::class)
          .first()
      (entity == queried) shouldBe true
    }

    test("hashCode is consistent with equals") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.add(Health(1))
      val queried =
        registry
          .query(Health::class)
          .first()
      entity.hashCode() shouldBe queried.hashCode()
    }

    test("entities from different registries are not equal") {
      val registry1 = HashMapEntityRegistry()
      val registry2 = HashMapEntityRegistry()
      val entity1 = registry1.createEntity()
      val entity2 = registry2.createEntity()
      (entity1 == entity2) shouldBe false
    }

    test("query without types returns all entities") {
      val registry = HashMapEntityRegistry()
      val _ = registry.createEntity()
      val _ = registry.createEntity()
      val _ = registry.createEntity()
      val _ =
        registry
          .query()
          .count() shouldBe 3
    }

    test("query with multiple types requires all") {
      val registry = HashMapEntityRegistry()
      val entity1 = registry.createEntity()
      entity1.add(Health(1))

      val entity2 = registry.createEntity()
      entity2.add(Name("test"))

      val entity3 = registry.createEntity()
      entity3.add(Health(2))
      entity3.add(Name("both"))

      registry
        .query(Health::class)
        .count() shouldBe 2
      registry
        .query(Name::class)
        .count() shouldBe 2
      registry
        .query(Health::class, Name::class)
        .count() shouldBe 1
    }

    test("destroyed entity is removed from query results") {
      val registry = HashMapEntityRegistry()
      val entity1 = registry.createEntity()
      entity1.add(Health(1))
      val entity2 = registry.createEntity()
      entity2.add(Health(2))

      registry
        .query(Health::class)
        .count() shouldBe 2
      entity1.destroy()
      registry
        .query(Health::class)
        .count() shouldBe 1
    }

    test("unaryPlus operator adds trait") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      with(entity) {
        +Health(42)
      }
      entity
        .get<Health>()
        ?.value shouldBe 42
    }

    test("unaryPlus throws after destroy") {
      val registry = HashMapEntityRegistry()
      val entity = registry.createEntity()
      entity.destroy()
      val exception =
        runCatching {
          with(entity) {
            +Health(1)
          }
        }.exceptionOrNull()
      exception.shouldBeInstanceOf<IllegalStateException>()
    }
  })
