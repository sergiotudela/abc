package com.rklaehn.abc

import algebra.ring._
import cats.Show

import cats.syntax.show._
import scala.reflect.ClassTag
import scala.util.hashing.MurmurHash3
import scala.{ specialized ⇒ sp }
import algebra._

final class TotalArrayMap[@sp(Int, Long, Double) K, @sp(Int, Long, Double) V](
    private[abc] val keys0: Array[K],
    private[abc] val values0: Array[V],
    private[abc] val default: V
  ) extends NoEquals { self ⇒

  import TotalArrayMap._

  def iterator = keys0.iterator zip values0.iterator

  def size: Int = keys0.length

  def keys: ArraySet[K] = new ArraySet[K](keys0)

  def values: ArraySeq[V] = new ArraySeq[V](values0)

  def apply(k: K)(implicit kOrder: Order[K]): V = {
    val i = Searching.search(keys0, 0, keys0.length, k)
    if (i >= 0) values0(i)
    else default
  }

  def combine(that: TotalArrayMap[K, V], f: (V, V) ⇒ V)(implicit kOrder: Order[K], kClassTag: ClassTag[K], vClassTag: ClassTag[V], vEq: Eq[V]): TotalArrayMap[K, V] =
    new Combine[K, V](this, that, f).result

  def mapValues(f: V ⇒ V)(implicit kClassTag: ClassTag[K], vEq: Eq[V], vClassTag: ClassTag[V]): TotalArrayMap[K, V] = {
    val rk = new Array[K](size)
    val rv = new Array[V](size)
    val rd = f(default)
    var i = 0
    var ri = 0
    while(i < keys0.length) {
      val r = f(values0(i))
      if(Eq.neqv(r, rd)) {
        rk(ri) = keys0(i)
        rv(ri) = r
        ri += 1
      }
      i += 1
    }
    val keys1 = if(ri == keys0.length) keys0 else rk.resizeInPlace(ri)
    val values1 = rv.resizeInPlace(ri)
    new TotalArrayMap[K, V](keys1, values1, rd)
  }

  override def toString: String =
    iterator
      .map { case (k, v) ⇒ s"$k->$v"}
      .mkString(s"ArrayMap(", ",", s").withDefaultValue($default)")
}

private[abc] trait TotalArrayMap1 {

  implicit def eqv[K: Eq, V: Eq]: Eq[TotalArrayMap[K, V]] = new Eq[TotalArrayMap[K, V]] {
    def eqv(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]) =
      Eq.eqv(x.keys0, y.keys0) && Eq.eqv(x.values0, y.values0) && Eq.eqv(x.default, y.default)
  }

  implicit def monoid[K: ClassTag : Order, V: ClassTag: Monoid: Eq]: Monoid[TotalArrayMap[K, V]] =
    new ArrayTotalMapMonoid[K, V]

  implicit def group[K: ClassTag: Order, V: ClassTag: Group: Eq]: Group[TotalArrayMap[K, V]] =
    new ArrayTotalMapGroup[K, V]

  implicit def additiveMonoid[K: ClassTag: Order, V: ClassTag: AdditiveMonoid: Eq]: AdditiveMonoid[TotalArrayMap[K, V]] =
    new ArrayTotalMapAdditiveMonoid[K, V]

  implicit def additiveGroup[K: ClassTag: Order, V: ClassTag: AdditiveGroup: Eq]: AdditiveGroup[TotalArrayMap[K, V]] =
    new ArrayTotalMapAdditiveGroup[K, V]

  implicit def multiplicativeMonoid[K: ClassTag: Order, V: ClassTag: MultiplicativeMonoid: Eq]: MultiplicativeMonoid[TotalArrayMap[K, V]] =
    new ArrayTotalMapMultiplicativeMonoid[K, V]

  implicit def multiplicativeGroup[K: ClassTag: Order, V: ClassTag: MultiplicativeGroup: Eq]: MultiplicativeGroup[TotalArrayMap[K, V]] =
    new ArrayTotalMapMultiplicativeGroup[K, V]
}

private class ArrayTotalMapMonoid[K: ClassTag : Order, V: ClassTag: Monoid: Eq] extends Monoid[TotalArrayMap[K, V]] {
  override def empty: TotalArrayMap[K, V] = TotalArrayMap.fromDefault[K, V](Monoid[V].empty)
  override def combine(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x, y) ⇒ Semigroup.combine(x, y))
}

private class ArrayTotalMapGroup[K: ClassTag: Order, V: ClassTag: Group: Eq] extends ArrayTotalMapMonoid[K, V] with Group[TotalArrayMap[K, V]] {
  override def inverse(x: TotalArrayMap[K, V]): TotalArrayMap[K, V] = x.mapValues(x ⇒ Group.inverse(x))
  override def remove(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x,y) ⇒ Group.remove(x, y))
}

private class ArrayTotalMapAdditiveMonoid[K: ClassTag : Order, V: ClassTag: AdditiveMonoid: Eq] extends AdditiveMonoid[TotalArrayMap[K, V]] {
  override def zero: TotalArrayMap[K, V] = TotalArrayMap.fromDefault[K, V](AdditiveMonoid[V].zero)
  override def plus(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x, y) ⇒ AdditiveSemigroup.plus(x, y))
}

private class ArrayTotalMapAdditiveGroup[K: ClassTag: Order, V: ClassTag: AdditiveGroup: Eq] extends ArrayTotalMapAdditiveMonoid[K, V] with AdditiveGroup[TotalArrayMap[K, V]] {
  override def negate(x: TotalArrayMap[K, V]): TotalArrayMap[K, V] = x.mapValues(x ⇒ AdditiveGroup.negate(x))
  override def minus(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x,y) ⇒ AdditiveGroup.minus(x, y))
}

private class ArrayTotalMapMultiplicativeMonoid[K: ClassTag : Order, V: ClassTag: MultiplicativeMonoid: Eq] extends MultiplicativeMonoid[TotalArrayMap[K, V]] {
  override def one: TotalArrayMap[K, V] = TotalArrayMap.fromDefault[K, V](MultiplicativeMonoid[V].one)
  override def times(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x, y) ⇒ MultiplicativeMonoid.times(x, y))
}

private class ArrayTotalMapMultiplicativeGroup[K: ClassTag: Order, V: ClassTag: MultiplicativeGroup: Eq] extends ArrayTotalMapMultiplicativeMonoid[K, V] with MultiplicativeGroup[TotalArrayMap[K, V]] {
  override def reciprocal(x: TotalArrayMap[K, V]): TotalArrayMap[K, V] = x.mapValues(x ⇒ MultiplicativeGroup.reciprocal(x))
  override def div(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]): TotalArrayMap[K, V] =
    x.combine(y, (x,y) ⇒ MultiplicativeGroup.div(x, y))
}

object TotalArrayMap extends TotalArrayMap1 {

  implicit def show[K: Show, V: Show]: Show[TotalArrayMap[K, V]] = Show.show(m ⇒
    m.iterator
      .map { case (k,v) ⇒ s"${k.show}->${v.show}" }
      .mkString(s"TotalArrayMap((", ",", s"), ${m.default.show})")
  )

  implicit def hash[K: Hash, V: Hash]: Hash[TotalArrayMap[K, V]] = new Hash[TotalArrayMap[K, V]] {
    def hash(x: TotalArrayMap[K, V]) =
      MurmurHash3.mix(Hash.hash(x.keys0), Hash.hash(x.values0))

    def eqv(x: TotalArrayMap[K, V], y: TotalArrayMap[K, V]) = {
      Eq.eqv(x.keys0, y.keys0) && Eq.eqv(x.values0, y.values0)
    }
  }

  private final class Merge[@sp(Int, Long, Double) K: Order: ClassTag, @sp(Int, Long, Double) V: ClassTag](
    a: TotalArrayMap[K, V],
    b: TotalArrayMap[K, V]
  ) extends BinaryMerge {

    @inline def ak = a.keys0
    @inline def av = a.values0
    @inline def bk = b.keys0
    @inline def bv = b.values0

    val rd = b.default
    val rk = new Array[K](a.size + b.size)
    val rv = new Array[V](a.size + b.size)
    var ri = 0

    def compare(ai: Int, bi: Int) = Order.compare(ak(ai), bk(bi))

    def fromA(a0: Int, a1: Int, bi: Int) = {
      System.arraycopy(ak, a0, rk, ri, a1 - a0)
      System.arraycopy(av, a0, rv, ri, a1 - a0)
      ri += a1 - a0
    }

    def fromB(ai: Int, b0: Int, b1: Int) = {
      System.arraycopy(bk, b0, rk, ri, b1 - b0)
      System.arraycopy(bv, b0, rv, ri, b1 - b0)
      ri += b1 - b0
    }

    def collision(ai: Int, bi: Int) = {
      rk(ri) = bk(bi)
      rv(ri) = bv(bi)
      ri += 1
    }

    merge0(0, ak.length, 0, bk.length)

    def result: TotalArrayMap[K, V] = new TotalArrayMap[K, V](rk.resizeInPlace(ri), rv.resizeInPlace(ri), rd)
  }

  private final class Combine[@sp(Int, Long, Double) K: Order: ClassTag, @sp(Int, Long, Double) V: Eq: ClassTag](
    a: TotalArrayMap[K, V], b: TotalArrayMap[K, V], f: (V, V) => V)
    extends BinaryMerge {

    @inline def ak = a.keys0
    @inline def av = a.values0
    @inline def bk = b.keys0
    @inline def bv = b.values0

    val rd = f(a.default, b.default)
    val rk = new Array[K](a.size + b.size)
    val rv = new Array[V](a.size + b.size)
    var ri = 0

    def compare(ai: Int, bi: Int) = Order.compare(ak(ai), bk(bi))

    def fromA(a0: Int, a1: Int, bi: Int) = {
      var ai = a0
      while(ai < a1) {
        val r = f(av(ai), b.default)
        if(Eq.neqv(r, rd)) {
          rk(ri) = ak(ai)
          rv(ri) = r
          ri += 1
        }
        ai += 1
      }
    }

    def fromB(ai: Int, b0: Int, b1: Int) = {
      var bi = b0
      while(bi < b1) {
        val r = f(a.default, bv(bi))
        if(Eq.neqv(r, rd)) {
          rk(ri) = bk(bi)
          rv(ri) = r
          ri += 1
        }
        bi += 1
      }
    }

    def collision(ai: Int, bi: Int) = {
      val r = f(av(ai), bv(bi))
      if(Eq.neqv(r, rd)) {
        rk(ri) = bk(bi)
        rv(ri) = r
        ri += 1
      }
    }

    merge0(0, ak.length, 0, bk.length)

    def result: TotalArrayMap[K, V] = new TotalArrayMap[K, V](rk.resizeInPlace(ri), rv.resizeInPlace(ri), rd)
  }

//  private final class IntersectWith[@sp(Int, Long, Double) K: Order: ClassTag, @sp(Int, Long, Double) V: ClassTag](
//    a: TotalArrayMap[K, V], b: TotalArrayMap[K, V], f: (V, V) => V)
//    extends BinaryMerge {
//
//    @inline def ak = a.keys0
//    @inline def av = a.values0
//    @inline def bk = b.keys0
//    @inline def bv = b.values0
//
//    val rk = new Array[K](a.size min b.size)
//    val rv = new Array[V](a.size min b.size)
//    var ri = 0
//
//    def compare(ai: Int, bi: Int) = Order.compare(ak(ai), bk(bi))
//
//    def fromA(a0: Int, a1: Int, bi: Int) = {}
//
//    def fromB(ai: Int, b0: Int, b1: Int) = {}
//
//    def collision(ai: Int, bi: Int) = {
//      rk(ri) = bk(bi)
//      rv(ri) = f(av(ai), bv(bi))
//      ri += 1
//    }
//
//    merge0(0, ak.length, 0, bk.length)
//
//    def result: TotalArrayMap[K, V] = new TotalArrayMap[K, V](rk.resizeInPlace(ri), rv.resizeInPlace(ri))
//  }

  private final class JustKeys[@sp(Int, Long, Double) K: Order: ClassTag, @sp(Int, Long, Double) V: ClassTag](a: TotalArrayMap[K, V], b: ArraySet[K]) extends BinaryMerge {

    @inline def ak = a.keys0
    @inline def av = a.values0
    @inline def bk = b.elements

    val rk = new Array[K](ak.length min bk.length)
    val rv = new Array[V](ak.length min bk.length)
    var ri = 0

    def compare(ai: Int, bi: Int) = Order.compare(ak(ai), bk(bi))

    def fromA(a0: Int, a1: Int, bi: Int) = {}

    def fromB(ai: Int, b0: Int, b1: Int) = {}

    def collision(ai: Int, bi: Int) = {
      rk(ri) = ak(ai)
      rv(ri) = av(ai)
      ri += 1
    }

    merge0(0, ak.length, 0, bk.length)

    def result: TotalArrayMap[K, V] =
      if(ri == ak.length) a
      else new TotalArrayMap[K, V](rk.resizeInPlace(ri), rv.resizeInPlace(ri), a.default)
  }

  private final class ExceptKeys[@sp(Int, Long, Double) K: Order: ClassTag, @sp(Int, Long, Double) V: ClassTag](a: TotalArrayMap[K, V], b: ArraySet[K]) extends BinaryMerge {

    @inline def ak = a.keys0
    @inline def av = a.values0
    @inline def bk = b.elements

    val rk = new Array[K](ak.length)
    val rv = new Array[V](ak.length)
    var ri = 0

    def compare(ai: Int, bi: Int) = Order.compare(ak(ai), bk(bi))

    def fromA(a0: Int, a1: Int, bi: Int) = {
      System.arraycopy(ak, a0, rk, ri, a1 - a0)
      System.arraycopy(av, a0, rv, ri, a1 - a0)
      ri += a1 - a0
    }

    def fromB(ai: Int, b0: Int, b1: Int) = {}

    def collision(ai: Int, bi: Int) = {}

    merge0(0, ak.length, 0, bk.length)

    def result: TotalArrayMap[K, V] =
      if(ri == ak.length) a
      else new TotalArrayMap[K, V](rk.resizeInPlace(ri), rv.resizeInPlace(ri), a.default)
  }

  def fromDefault[@sp(Int, Long, Double) K: ClassTag, @sp(Int, Long, Double) V: ClassTag](default: V): TotalArrayMap[K, V] =
    new TotalArrayMap(Array.empty[K], Array.empty[V], default)
}