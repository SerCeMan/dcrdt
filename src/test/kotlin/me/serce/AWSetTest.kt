package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AWSetTest {

    lateinit var global: Global<AWSet<Char>>
    lateinit var set1: AntiEntropy<AWSet<Char>>
    lateinit var set2: AntiEntropy<AWSet<Char>>

    @BeforeEach
    fun setUp() {
        global = Global()
        set1 = global.create { AWSet() }
        set2 = global.create { AWSet() }
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
        set1.shipIntervalOrState()
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
        set1.shipIntervalOrState()
        set2.shipIntervalOrState()
        assertEquals(setOf('B'), set1.query(Elements()))
        assertEquals(setOf('B'), set2.query(Elements()))

        set1.operation(Remove(), 'C')
        set2.operation(Add(), 'C')
        set1.shipIntervalOrState()
        set2.shipIntervalOrState()
        assertEquals(setOf('B', 'C'), set1.query(Elements()))
        assertEquals(setOf('B', 'C'), set2.query(Elements()))
    }
}
