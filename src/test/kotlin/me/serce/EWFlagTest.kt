package me.serce

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EWFlagTest {

  lateinit var global: Global<EWFlag>
  lateinit var flag1: AntiEntropy<EWFlag>
  lateinit var flag2: AntiEntropy<EWFlag>

  @BeforeEach
  fun setUp() {
    global = Global()
    flag1 = global.create { EWFlag() }
    flag2 = global.create { EWFlag() }
  }

  @Test
  fun testIt() {
    flag1.operation(Enable)
    assertEquals(true, flag1.query(Read))
    assertEquals(false, flag2.query(Read))

    println(flag1)

    flag1.shipIntervalOrState()
    assertEquals(true, flag1.query(Read))
    assertEquals(true, flag2.query(Read))

    flag2.operation(Disable)
    flag1.shipIntervalOrState()
    flag2.shipIntervalOrState()
    assertEquals(false, flag1.query(Read))
    assertEquals(false, flag2.query(Read))
  }
}
