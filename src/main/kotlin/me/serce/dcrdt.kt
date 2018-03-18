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

interface Lattice<S : Lattice<S>> {
  fun join(other: S): S
}

interface Mutator<S : Lattice<S>, in Args> {
  fun apply(id: ID, state: S, args: Args): S
}

interface Query<S : Lattice<S>, out T> {
  fun apply(state: S): T
}

typealias Nat = Int

class Global<S : Lattice<S>>(
  private val nodes: HashMap<ID, AntiEntropy<S>> = hashMapOf()
) {
  private fun neighbour(id: ID): AntiEntropy<S> = nodes[id]!!

  fun create(factory: () -> S): AntiEntropy<S> {
    val state = factory()
    val n = ID(nodes.size)
    val node = AntiEntropy(this, n, state)
    nodes[n] = node
    return node
  }

  fun sendAck(j: ID, n: Nat) {
    neighbour(j).receiveAck(j, n)
  }

  fun send(j: ID, delta: S, c: Nat) {
    neighbour(j).receive(j, delta, c)
  }

  fun size(): Int = nodes.size
}

data class AntiEntropy<S : Lattice<S>>(
  val g: Global<S>,
  private val i: ID,
  var X: S,
  var c: Nat = 0,
  var D: Map<Nat, S> = hashMapOf(), // deltas
  var A: Map<ID, Nat> = hashMapOf() // ack map
) {
  fun receive(j: ID, delta: S, n: Nat) {
    val newState: S = X.join(delta)
    if (newState != X) {
      X = newState
      D = D.plus(c to delta)
      c += 1
    }
    g.sendAck(j, n)
  }

  fun receiveAck(j: ID, n: Nat) {
    A = A.plus(j to max(A[j] ?: 0, n))
  }

  fun operation(mutator: Mutator<S, Unit>) {
    operation(mutator, Unit)
  }

  fun <A> operation(mutator: Mutator<S, A>, args: A) {
    val delta: S = mutator.apply(i, X, args)
    D = D.plus(c to delta)
    X = X.join(delta)
    c += 1
  }

  fun <T> query(q: Query<S, T>): T {
    return q.apply(X)
  }

  fun shipIntervalOrState() {
    for (jid in 0 until g.size()) {
      val j = ID(jid)
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

  override fun toString(): String {
    return X.toString()
  }
}

// counter

data class GCCounterState(
  val data: HashMap<ID, Int> = hashMapOf()
) : Lattice<GCCounterState> {
  override fun join(other: GCCounterState): GCCounterState {
    val newMap = hashMapOf<ID, Int>()
    newMap.putAll(data)
    for ((k, v) in other.data) {
      newMap[k] = Math.max(newMap[k] ?: 0, v)
    }
    return GCCounterState(newMap)
  }
}

object Inc : Mutator<GCCounterState, Unit> {
  override fun apply(i: ID, state: GCCounterState, args: Unit): GCCounterState {
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

data class ID(val id: Int) {
  override fun toString(): String {
    return "I$id)"
  }
}

class CasualContext(
  val context: Set<Pair<ID, Nat>> = emptySet() // compact version vector
) : Lattice<CasualContext> {
  fun max(i: ID): Nat = context.filter { it.first == i }.maxBy { it.second }?.second ?: 0
  fun next(i: ID): Pair<ID, Nat> = i to max(i) + 1

  override fun join(other: CasualContext): CasualContext {
    return CasualContext(context.union(other.context))
  }

  fun contains(d: Pair<ID, Nat>): Boolean {
    return context.contains(d)
  }

  override fun toString(): String {
    return context.toString()
  }
}

interface DotStore {
  fun dots(): Set<Pair<ID, Nat>>
}

data class DotSet(
  val dots: Set<Pair<ID, Nat>> = emptySet()
) : DotStore {
  override fun dots(): Set<Pair<ID, Nat>> = dots
  override fun toString(): String {
    return dots.joinToString(",", "[", "]") { "{${it.first}=${it.second}}" }
  }
}

data class DotFun<V : Lattice<V>>(
  val m: Map<Pair<ID, Nat>, V>
) : DotStore {
  override fun dots(): Set<Pair<ID, Nat>> = m.keys
}

data class DotMap<K, V : DotStore>(
  val m: Map<K, V> = emptyMap()
) : DotStore, Map<K, V> by m {
  override fun dots(): Set<Pair<ID, Nat>> = m.values
    .map { it.dots() }
    .reduce { a, b -> a.union(b) }

  override fun toString(): String {
    return "$m"
  }
}


interface Casual<Store : DotStore, This : Casual<Store, This>> : Lattice<This> {
  val store: Store
  val cs: CasualContext
}

data class CasualDotSet(
  override val store: DotSet = DotSet(),
  override val cs: CasualContext = CasualContext()
) : Casual<DotSet, CasualDotSet> {
  override fun join(other: CasualDotSet): CasualDotSet {
    val store2 = other.store
    return CasualDotSet(
      DotSet(store.dots.intersect(store2.dots)
        .union(leaveNotIn(store, other.cs))
        .union(leaveNotIn(store2, cs))),
      cs.join(other.cs)
    )
  }

  override fun toString(): String {
    return "{m=$store,c=$cs}"
  }

  companion object {
    private fun leaveNotIn(store: DotSet, cs: CasualContext): Set<Pair<ID, Nat>> {
      return store.dots
        .filter { d -> !cs.contains(d) }
        .toSet()
    }
  }
}

class CasualDotFun<V : Lattice<V>>(
  override val store: DotFun<V>,
  override val cs: CasualContext
) : Casual<DotFun<V>, CasualDotFun<V>> {
  override fun join(other: CasualDotFun<V>): CasualDotFun<V> {
    return CasualDotFun(
      DotFun(
        mergeInBoth(this, other)
          .plus(mergeNotInSecond(this, other))
          .plus(mergeNotInSecond(other, other))
      ),
      cs.join(other.cs)
    )
  }

  companion object {
    private fun <V : Lattice<V>> mergeInBoth(a: CasualDotFun<V>, b: CasualDotFun<V>): Map<Pair<ID, Nat>, V> {
      val m1 = a.store.m
      val m2 = b.store.m
      val result = HashMap<Pair<ID, Nat>, V>()
      for ((k1, v1) in m1) {
        val v2 = m2[k1]
        if (v2 != null) {
          result[k1] = v1.join(v2)
        }
      }
      return result
    }

    private fun <V : Lattice<V>> mergeNotInSecond(a: CasualDotFun<V>, b: CasualDotFun<V>): Map<Pair<ID, Nat>, V> {
      val m1 = a.store.m
      val c2 = b.cs
      return m1.filter { (k, _) -> c2.contains(k) }
    }
  }
}

class CasualDotMap<K, S : DotStore>(
  override val store: DotMap<K, S> = DotMap(),
  override val cs: CasualContext = CasualContext()
) : Casual<DotMap<K, S>, CasualDotMap<K, S>> {
  override fun join(other: CasualDotMap<K, S>): CasualDotMap<K, S> {
    return CasualDotMap(
      DotMap(
        combineMaps<K, S, Nothing>(this, other)
      ),
      cs.join(other.cs)
    )
  }

  override fun toString(): String {
    return "{m=$store,c=$cs}"
  }

  companion object {
    private fun <K, S : DotStore, CS : Casual<S, CS>> combineMaps(a: CasualDotMap<K, S>, b: CasualDotMap<K, S>): Map<K, S> {
      val m1 = a.store.m
      val m2 = b.store.m
      return m1.keys.union(m2.keys)
        .map { k ->
          val v1: S? = m1[k]
          val v2: S? = m2[k]
          k to v<K, S, CS>(k, v1, v2, a.cs, b.cs)
        }
        .toMap()
        .filterValuesNotNull()
    }

    private fun <K, T : DotStore, CS : Casual<T, CS>> v(k: K, v1: T?, v2: T?, cs1: CasualContext, cs2: CasualContext): T? {
      val casual1: CS
      val casual2: CS
      if (v1 is DotSet || v2 is DotSet) {
        val v11 = v1 as? DotSet ?: DotSet()
        val v22 = v2 as? DotSet ?: DotSet()
        casual1 = CasualDotSet(v11, cs1) as CS
        casual2 = CasualDotSet(v22, cs2) as CS
      } else TODO()

      val pair = casual1.join(casual2)
      return when {
        pair.store.dots().isEmpty() -> null
        else -> pair.store
      }
    }
  }
}

class PairLattice<A : Lattice<A>, B : Lattice<B>>(
  val a: A?,
  val b: B?
) : Lattice<PairLattice<A, B>> {
  override fun join(other: PairLattice<A, B>): PairLattice<A, B> {
    return PairLattice(
      join(a, other.a),
      join(b, other.b)
    )
  }

  companion object {
    fun <A : Lattice<A>> join(a: A?, b: A?): A? {
      return when {
        a == null && b == null -> null
        a == null -> b
        b == null -> a
        else -> a.join(b)
      }
    }
  }
}

class LexPair<A : Comparable<A>, B>(
  val a: A?,
  val b: B?
) : Lattice<LexPair<A, B>> {
  override fun join(other: LexPair<A, B>): LexPair<A, B> {
    TODO()
  }
}

private fun <K, V> Map<K, V?>.filterValuesNotNull(): Map<K, V> {
  val result = HashMap<K, V>()
  for ((k, v) in this) {
    if (v != null) {
      result[k] = v
    }
  }
  return result
}

// impls

typealias EWFlag = CasualDotSet

object Enable : Mutator<EWFlag, Unit> {
  override fun apply(id: ID, state: EWFlag, args: Unit): EWFlag {
    val s = state.store
    val d: Set<Pair<ID, Nat>> = setOf(state.cs.next(id))
    return EWFlag(
      DotSet(d),
      CasualContext(s.dots.union(d))
    )
  }
}

object Disable : Mutator<EWFlag, Unit> {
  override fun apply(i: ID, state: EWFlag, args: Unit): EWFlag {
    return EWFlag(
      DotSet(setOf()),
      CasualContext(state.store.dots())
    )
  }
}

object Read : Query<EWFlag, Boolean> {
  override fun apply(state: EWFlag): Boolean {
    return state.store.dots().isNotEmpty()
  }
}


typealias AWSet<E> = CasualDotMap<E, DotSet>

class Add<E> : Mutator<AWSet<E>, E> {
  override fun apply(id: ID, state: AWSet<E>, e: E): AWSet<E> {
    val m: DotMap<E, DotSet> = state.store
    val cs = state.cs
    val d: Set<Pair<ID, Nat>> = setOf(cs.next(id))

    val me = (m[e] ?: DotSet())
    return AWSet(
      store = DotMap(mapOf(e to DotSet(d))),
      cs = CasualContext(d.union(me.dots))
    )
  }
}

class Remove<E> : Mutator<AWSet<E>, E> {
  override fun apply(id: ID, state: AWSet<E>, e: E): AWSet<E> {
    val m: DotMap<E, DotSet> = state.store
    val me = (m[e] ?: DotSet())
    return AWSet(
      store = DotMap(emptyMap()),
      cs = CasualContext(me.dots)
    )
  }
}

class Elements<E> : Query<AWSet<E>, Set<E>> {
  override fun apply(state: AWSet<E>): Set<E> {
    val m: DotMap<E, DotSet> = state.store
    return m.keys
  }
}

class ORRMap<K, DS: DotStore, V: Casual<DS, V>>(
  private val casual: CasualDotMap<K, DS>,
  override val store: DS = TODO(),
  override val cs: CasualContext = casual.cs
): Casual<DS, ORRMap<K, DS, V>> {
  override fun join(other: ORRMap<K, DS, V>): ORRMap<K, DS, V> {
    return ORRMap(casual.join(other.casual))
  }
}

typealias ORMap<K, DS, V> = CasualDotMap<K, DS>

class Apply<K, DS : DotStore, Args, M : Mutator<V, Args>, V : Casual<DS, V>>(
  val supplier: () -> DS
) : Mutator<ORMap<K, DS, V>, Triple<K, M, Args>> {
  override fun apply(id: ID, state: ORMap<K, DS, V>, args: Triple<K, M, Args>): ORMap<K, DS, V> {
    val (k, deltaMutator, deltaMutatorArgs) = args
    val m: DS = state.store[k] ?: supplier()
    val cs = state.cs
    val result: V = deltaMutator.apply(id, m.asCasual(cs), deltaMutatorArgs)
    val v: DS = result.store
    val cc: CasualContext = result.cs
    return ORMap<K, DS, V>(
      store = DotMap(mapOf(k to v)),
      cs = cc
    )
  }
}

class ORMRemove<K, DS : DotStore, V : Casual<DS, V>> : Mutator<ORMap<K, DS, V>, K> {
  override fun apply(id: ID, state: ORMap<K, DS, V>, k: K): ORMap<K, DS, V> {
    val m: DS? = state.store[k]
    return ORMap<K, DS, V>(
      store = DotMap(),
      cs = CasualContext(m?.dots() ?: emptySet())
    )
  }
}

private fun <DS : DotStore, V : Casual<DS, V>> DS.asCasual(cs: CasualContext): V {
  return when (this) {
    is DotSet -> CasualDotSet(this, cs) as V
    is DotFun<*> -> CasualDotFun(this as DotFun<Nothing>, cs) as V
    is DotMap<*, *> -> CasualDotMap(this as DotMap<Nothing, Nothing>, cs) as V
    else -> throw IllegalArgumentException()
  }
}
