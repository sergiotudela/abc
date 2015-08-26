package com.rklaehn.abc

import ichi.bench.Thyme
import ichi.bench.Thyme.HowWarm
import algebra.Eq

import scala.collection.immutable.{HashMap, SortedMap}
import scala.io.Source
import scala.util.hashing.Hashing

object RadixTreeBench2 extends App {
  val names = Source.fromURL("http://www-01.sil.org/linguistics/wordlists/english/wordlist/wordsEn.txt").getLines.toArray
  println(names.length)
  println(names.take(10).mkString("\n"))

  implicit object UnitEq extends Eq[Unit] {

    override def eqv(x: Unit, y: Unit): Boolean = true
  }

  implicit object EqHashing extends Hashing[Unit] {

    override def hash(x: Unit): Int = 0
  }

  lazy val th = new Thyme() // Thyme.warmed(verbose = println, warmth = HowWarm.BenchOff)

  val kvs = names.map(s => s -> (()))

  val radixTree = RadixTree(kvs: _*).packed

  val sortedMap = SortedMap(kvs: _*)

  val hashMap = HashMap(kvs: _*)

  def create0[K: Ordering, V](kvs: Array[(K, V)]): Int = {
    SortedMap(kvs: _*).size
  }

  def create1[K, V](kvs: Array[(K, V)])(implicit f:RadixTree.Family[K, V]): Int = {
    RadixTree[K,V](kvs: _*).count
  }

  def lookup0(): Boolean = {
    kvs.forall {
      case (k,v) => radixTree.contains(k)
    }
  }

  def lookup1(): Boolean = {
    kvs.forall {
      case (k,v) => hashMap.contains(k)
    }
  }

  def lookup2(): Boolean = {
    kvs.forall {
      case (k,v) => sortedMap.contains(k)
    }
  }

  def filterPrefixS(): AnyRef = {
    sortedMap.filter { case (k,v) => k.startsWith("one") }
  }

  def filterPrefixH(): AnyRef = {
    hashMap.filter { case (k,v) => k.startsWith("one") }
  }

  def filterPrefixR(): AnyRef = {
    radixTree.filterPrefix("one")
  }

  def filterContainsS(): AnyRef = {
    sortedMap.filter { case (k,v) => k.contains("one") }
  }

  def filterContainsH(): AnyRef = {
    hashMap.filter { case (k,v) => k.contains("one") }
  }

  def filterContainsR(): AnyRef = {
    radixTree.filterKeysContaining("one")
  }

  th.pbenchOffWarm("Create 1000 SortedMap vs. RadixTree")(th.Warm(create0(kvs)))(th.Warm(create1(kvs)))
  th.pbenchOffWarm("Lookup 1000 SortedMap vs. RadixTree")(th.Warm(lookup0()))(th.Warm(lookup1()))

  th.pbenchOffWarm("FilterPrefix HashMap vs. RadixTree")(th.Warm(filterPrefixH()))(th.Warm(filterPrefixR()))
  th.pbenchOffWarm("FilterPrefix SortedMap vs. RadixTree")(th.Warm(filterPrefixS()))(th.Warm(filterPrefixR()))

  th.pbenchOffWarm("FilterContains HashMap vs. RadixTree")(th.Warm(filterContainsH()))(th.Warm(filterContainsR()))
  th.pbenchOffWarm("FilterContains SortedMap vs. RadixTree")(th.Warm(filterContainsS()))(th.Warm(filterContainsR()))
}
