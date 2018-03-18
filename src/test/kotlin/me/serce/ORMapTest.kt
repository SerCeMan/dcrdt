package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ORMapTest {

  lateinit var global: Global<ORMap<Int, DotSet, EWFlag>>
  lateinit var map1: AntiEntropy<ORMap<Int, DotSet, EWFlag>>
  lateinit var set2: AntiEntropy<ORMap<Int, DotSet, EWFlag>>

  @BeforeEach
  fun setUp() {
    global = Global()
    map1 = global.create { ORMap<Int, DotSet, EWFlag>() }
    set2 = global.create { ORMap<Int, DotSet, EWFlag>() }
  }

  @Test
  fun testMe() {
    map1.operation(Apply<Int, DotSet, Unit, Mutator<EWFlag, Unit>, EWFlag>({ DotSet() }),
      Triple(1, Enable, Unit))
    println(map1)


  }
}
