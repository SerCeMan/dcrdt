package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ORMapTest {

  lateinit var global: Global<ORMap<Int, DotSet, AWSet<String>>>
  lateinit var map1: AntiEntropy<ORMap<Int, DotSet, AWSet<String>>>
  lateinit var set2: AntiEntropy<ORMap<Int, DotSet, AWSet<String>>>

  @BeforeEach
  fun setUp() {
    global = Global()
    map1 = global.create { ORMap<Int, DotSet, AWSet<String>>() }
    set2 = global.create { ORMap<Int, DotSet, AWSet<String>>() }
  }

  @Test
  fun testMe() {
//    map1.operation(Apply<Int, DotSet, Unit, Mutator<AWSet<String>, Unit>, AWSet<String>>({ DotSet() }),
//      Triple(1, object: Mutator<AWSet<String>, Unit> {
//        override fun apply(id: ID, state: AWSet<String>, args: Unit): AWSet<String> {
//          return null!!
//        }
//      }, Unit))
    assertEquals(setOf<Int>(), map1.query(Elements()))
    assertEquals(setOf<Int>(), set2.query(Elements()))

  }
}
