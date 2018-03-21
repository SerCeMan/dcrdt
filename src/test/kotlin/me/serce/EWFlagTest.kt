package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EWFlagTest {

  lateinit var g: Global<EWFlag>
  lateinit var flag1: AntiEntropy<EWFlag>
  lateinit var flag2: AntiEntropy<EWFlag>

  @BeforeEach
  fun setUp() {
    g = Global()
    flag1 = g.create { EWFlag() }
    flag2 = g.create { EWFlag() }
  }

  @Test
  fun testIt() {
    flag1.operation(Enable)
    assertEquals(true, flag1.query(Read))
    assertEquals(false, flag2.query(Read))

    println(flag1)

    g.shipIntervalOrState(flag1)
    assertEquals(true, flag1.query(Read))
    assertEquals(true, flag2.query(Read))

    flag2.operation(Disable)
    g.shipIntervalOrState(flag1)
    g.shipIntervalOrState(flag2)
    assertEquals(false, flag1.query(Read))
    assertEquals(false, flag2.query(Read))
  }
}
