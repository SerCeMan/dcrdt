package me.serce

import kotlin.math.max

// GCounter

class GCounterMutable(
  private val i: Int,
  private val data: HashMap<Int, Int> = hashMapOf()
) {
  fun inc(): Pair<Int, Int> {
    val delta = Pair(i, (data[i] ?: 0).inc())
    data += delta
    return delta
  }

  fun value(): Int {
    return data.values.sum()
  }

  fun join(m: GCounterMutable) {
    for ((k, v) in m.data) {
      data[k] = Math.max(data[k] ?: 0, v)
    }
  }
}

data class GCounterImmutable(
  private val i: Int,
  private val data: Map<Int, Int> = hashMapOf()
) {
  fun inc(): HashMap<Int, Int> {
    return hashMapOf(Pair(i, (data[i] ?: 0).inc()))
  }

  fun value(): Int {
    return data.values.sum()
  }

  fun join(m: GCounterImmutable): GCounterImmutable {
    val newMap = hashMapOf<Int, Int>()
    newMap.putAll(data)
    for ((k, v) in m.data) {
      newMap[k] = Math.max(newMap[k] ?: 0, v)
    }
    return GCounterImmutable(i, newMap)
  }
}

// deltas

interface State<S : State<S>> {
  fun join(m: S): S
}

interface Mutator<S : State<S>> {
  fun apply(i: Int, state: S): S
}

interface Query<S : State<S>, out T> {
  fun apply(state: S): T
}

typealias Nat = Int

class Global<S : State<S>>(
  private val nodes: HashMap<Int, AntiEntropy<S>> = hashMapOf()
) {
  private fun neighbour(id: Int): AntiEntropy<S> = nodes[id]!!

  fun create(factory: () -> S): AntiEntropy<S> {
    val state = factory()
    val n = nodes.size
    val node = AntiEntropy(this, n, state)
    nodes[n] = node
    return node
  }

  fun sendAck(j: Int, n: Nat) {
    neighbour(j).receiveAck(j, n)
  }

  fun send(j: Int, delta: S, c: Nat) {
    neighbour(j).receive(j, delta, c)
  }

  fun size(): Int = nodes.size
}

data class AntiEntropy<S : State<S>>(
  val g: Global<S>,
  val i: Int,
  var X: S,
  var c: Nat = 0,
  var D: Map<Nat, S> = hashMapOf(), // deltas
  var A: Map<Int, Nat> = hashMapOf() // ack map
) {
  fun receive(j: Int, delta: S, n: Nat) {
    val newState: S = X.join(delta)
    if (newState != X) {
      X = newState
      D = D.plus(c to delta)
      c += 1
    }
    g.sendAck(j, n)
  }

  fun receiveAck(j: Int, n: Nat) {
    A = A.plus(j to max(A[j] ?: 0, n))
  }

  fun operation(mutator: Mutator<S>) {
    val delta: S = mutator.apply(i, X)
    D = D.plus(c to delta)
    X = X.join(delta)
    c += 1
  }

  fun <T> query(q: Query<S, T>): T {
    return q.apply(X)
  }

  fun shipIntervalOrState() {
    for (j in 0 until g.size()) {
      if (j == i) {
        continue
      }
      val ackNumber = A[j] ?: 0
      val delta: S = when {
        D.isEmpty() || (D.keys.min() ?: 0) > ackNumber -> X
        else ->
          D.filter { (l, _) -> l in ackNumber..(c - 1) }
            .map { (_, s) -> s }
            .reduce { s1, s2 -> s1.join(s2) }
      }
      if (ackNumber < c) {
        g.send(j, delta, c)
      }
    }
  }

  fun gc() {
    val l = A.values.min() ?: 0
    D = D.filter { (n, _) -> n >= l }
  }
}

// counter

data class GCCounterState(
  val data: HashMap<Int, Int> = hashMapOf()
) : State<GCCounterState> {
  override fun join(m: GCCounterState): GCCounterState {
    val newMap = hashMapOf<Int, Int>()
    newMap.putAll(data)
    for ((k, v) in m.data) {
      newMap[k] = Math.max(newMap[k] ?: 0, v)
    }
    return GCCounterState(newMap)
  }
}

object Inc : Mutator<GCCounterState> {
  override fun apply(i: Int, state: GCCounterState): GCCounterState {
    val data = state.data
    return GCCounterState(hashMapOf(i to (data[i] ?: 0).inc()))
  }
}

object Value : Query<GCCounterState, Int> {
  override fun apply(state: GCCounterState): Int {
    return state.data.values.sum()
  }
}

// context

class CasualContext(
  private val context: Map<Int, Nat> // compact version vector
) {
  fun max(i: Int): Nat = context[i] ?: 0
  fun next(i: Int): Pair<Int, Nat> = i to max(i) + 1
}


//

fun main(args: Array<String>) {
  val counterGlobal = Global<GCCounterState>()
  val counter1: AntiEntropy<GCCounterState> = counterGlobal.create { GCCounterState() }
  val counter2: AntiEntropy<GCCounterState> = counterGlobal.create { GCCounterState() }

  println(counter1)
  counter1.operation(Inc)
  println(counter1)
  println(counter2)
  counter2.operation(Inc)
  counter1.shipIntervalOrState()
  println(counter2)
  counter2.shipIntervalOrState()

  println("Result " + counter1.query(Value))
  println("Result " + counter2.query(Value))

  counter1.gc()
  counter2.gc()

  println(counter1)

  println("Result " + counter1.query(Value))
  println("Result " + counter2.query(Value))
}


//







































