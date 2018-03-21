package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AWSetTest {

  lateinit var g: Global<AWSet<Char>>
  lateinit var set1: AntiEntropy<AWSet<Char>>
  lateinit var set2: AntiEntropy<AWSet<Char>>

  @BeforeEach
  fun setUp() {
    g = Global()
    set1 = g.create { AWSet() }
    set2 = g.create { AWSet() }
  }

  @Test
  fun testMe() {
    assertEquals(setOf<Int>(), set1.query(Elements()))
    assertEquals(setOf<Int>(), set2.query(Elements()))
    set1.operation(Add(), 'A')
    println(set1)
    println(set2)
    assertEquals(setOf('A'), set1.query(Elements()))
    assertEquals(setOf<Char>(), set2.query(Elements()))
    g.shipIntervalOrState(set1)
    assertEquals(setOf('A'), set1.query(Elements()))
    assertEquals(setOf('A'), set2.query(Elements()))
    set1.operation(Add(), 'B')
    println(set1)
    println(set2)
    assertEquals(setOf('A', 'B'), set1.query(Elements()))
    assertEquals(setOf('A'), set2.query(Elements()))
    set2.operation(Remove(), 'A')
    println(set1)
    println(set2)
    assertEquals(setOf('A', 'B'), set1.query(Elements()))
    assertEquals(setOf<Char>(), set2.query(Elements()))
    g.shipIntervalOrState(set1)
    g.shipIntervalOrState(set2)
    assertEquals(setOf('B'), set1.query(Elements()))
    assertEquals(setOf('B'), set2.query(Elements()))

    set1.operation(Remove(), 'C')
    set2.operation(Add(), 'C')
    g.shipIntervalOrState(set1)
    g.shipIntervalOrState(set2)
    assertEquals(setOf('B', 'C'), set1.query(Elements()))
    assertEquals(setOf('B', 'C'), set2.query(Elements()))
  }
}
