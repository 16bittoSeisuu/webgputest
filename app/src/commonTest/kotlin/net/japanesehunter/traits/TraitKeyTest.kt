package net.japanesehunter.traits

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TraitKeyTest :
  FunSpec({
    test("TraitKey with custom conversion returns correct writableType") {
      val key = TraitKey<String, Int> { it.toString() }
      key.writableType shouldBe Int::class
    }

    test("TraitKey with custom conversion applies transformation") {
      val key = TraitKey<String, Int> { "value:$it" }
      key.provideReadonlyView(42) shouldBe "value:42"
    }

    test("TraitKey with identity conversion returns correct writableType") {
      val key = TraitKey<CharSequence, String>()
      key.writableType shouldBe String::class
    }

    test("TraitKey with identity conversion returns same instance") {
      val key = TraitKey<CharSequence, String>()
      val input = "test"
      key.provideReadonlyView(input) shouldBe input
    }
  })
