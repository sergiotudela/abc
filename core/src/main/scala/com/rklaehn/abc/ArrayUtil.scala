package com.rklaehn.abc

import algebra.{Order, Eq}

// scalastyle:off return
private[abc] object ArrayUtil {

  def eqv[A: Eq](x: Array[A], y: Array[A]): Boolean = {
    x.length == y.length && {
      var i = 0
      while(i < x.length) {
        if(!Eq.eqv(x(i), y(i)))
          return false
        i += 1
      }
      true
    }
  }

  def hash[@sp A: Hash](a: Array[A]): Int = {
    import scala.util.hashing.MurmurHash3
    var result = MurmurHash3.arraySeed
    var i = 0
    while(i < a.length) {
      result = MurmurHash3.mix(result, Hash.hash(a(i)))
      i += 1
    }
    result
  }

  def compare[@sp A: Order](x: Array[A], y: Array[A]): Int = {
    var i = 0
    while (i < x.length && i < y.length) {
      val cmp = Order.compare(x(i), y(i))
      if (cmp != 0) return cmp
      i += 1
    }
    sign(x.length - y.length)
  }

  def dropRightWhile[T: Eq](a: Array[T], z: T): Array[T] = {
    @tailrec
    def lastIndexWhereZero(i: Int): Int =
      if(i == 0) i
      else if(Eq.neqv(a(i - 1), z)) i
      else lastIndexWhereZero(i - 1)
    a.resizeInPlace(lastIndexWhereZero(a.length))
  }

  def vectorCompare[@sp A: Order](x: Array[A], xd:A, y: Array[A], yd: A): Int = {
    var i = 0
    while (i < x.length && i < y.length) {
      val cmp = Order.compare(x(i), y(i))
      if (cmp != 0) return cmp
      i += 1
    }
    while (i < x.length) {
      val cmp = Order.compare(x(i), yd)
      if (cmp != 0) return cmp
      i += 1
    }
    while (i < y.length) {
      val cmp = Order.compare(xd, y(i))
      if (cmp != 0) return cmp
      i += 1
    }
    0
  }

  def combine[A](x: Array[A], x_d: A, y: Array[A], y_d: A)(f: (A, A) => A): Array[A] = {
    val re = newArray(x.length max y.length, x)
    var i = 0
    while (i < x.length && i < y.length) {
      re(i) = f(x(i), y(i))
      i += 1
    }
    while (i < x.length) {
      re(i) = f(x(i), y_d)
      i += 1
    }
    while (i < y.length) {
      re(i) = f(x_d, y(i))
      i += 1
    }
    re
  }

  def filter[@sp T](a: Array[T], f: T => Boolean): Array[T] = {
    val r = newArray(a.length, a)
    var ri = 0
    var i = 0
    while (i < a.length) {
      if (f(a(i))) {
        r(ri) = a(i)
        ri += 1
      }
      i += 1
    }
    if (ri == r.length) a
    else r.resizeInPlace(ri)
  }

  private[this] def sign(x: Int) =
    if(x > 0) 1
    else if(x < 0) -1
    else 0
}