package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CounterTest {

  lateinit var g: Global<GCCounterState>
  lateinit var counter1: AntiEntropy<GCCounterState>
  lateinit var counter2: AntiEntropy<GCCounterState>

  @BeforeEach
  internal fun setUp() {
    g = Global()
    counter1 = g.create { GCCounterState() }
    counter2 = g.create { GCCounterState() }
  }

  @Test
  internal fun testIt() {
    assertEquals(0, counter1.query(Value))
    assertEquals(0, counter2.query(Value))
    counter1.operation(Inc)
    assertEquals(1, counter1.query(Value))
    assertEquals(0, counter2.query(Value))
    counter2.operation(Inc)
    assertEquals(1, counter1.query(Value))
    assertEquals(1, counter2.query(Value))
    g.shipIntervalOrState(counter1)
    assertEquals(1, counter1.query(Value))
    assertEquals(2, counter2.query(Value))
    g.shipIntervalOrState(counter2)
    assertEquals(2, counter1.query(Value))
    assertEquals(2, counter2.query(Value))
  }
}
