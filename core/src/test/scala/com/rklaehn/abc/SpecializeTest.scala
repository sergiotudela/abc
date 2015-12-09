package com.rklaehn.abc

import DebugUtil._
import algebra.std.all._
import Instances._
import org.scalatest.FunSuite

class SpecializeTest extends FunSuite {

  import scala.util.hashing.Hashing._
  import OrderedArrayTag.generic

  test("seqSpecialization") {
    assert(ArraySeq.empty[Int].isSpecialized)
    assert(ArraySeq.singleton(1).isSpecialized)
    assert(ArraySeq(1,2,3).isSpecialized)
    assert(ArraySeq(1,2,3).concat(ArraySeq(1,2,3)).isSpecialized)
  }

  test("setSpecialization") {
    assert(ArraySet.empty[Int].isSpecialized)
    assert(ArraySet.singleton(1).isSpecialized)
    assert(ArraySet(1,2,3).isSpecialized)
    assert(ArraySet(1,2,3).union(ArraySet(3,4,5)).isSpecialized)
    assert(ArraySet(1,2,3).intersect(ArraySet(3,4,5)).isSpecialized)
    assert(ArraySet(1,2,3).diff(ArraySet(3,4,5)).isSpecialized)
    assert(ArraySet(1,2,3).xor(ArraySet(3,4,5)).isSpecialized)
    assert(ArraySet(1,2,3).asArraySeq.isSpecialized)
  }

  test("mapSpecialization") {
    // for an int/int map we expect full specialization
    assert(ArrayMap.empty[Int, Int].isSpecialized)
    assert(ArrayMap.singleton(1, 1).isSpecialized)
    assert(ArrayMap(1 → 1, 2 → 2).isSpecialized)
    assert(ArrayMap(1 → 1, 2 → 2).keys.isSpecialized)
    assert(ArrayMap(1 → 1, 2 → 2).values.isSpecialized)
    assert(ArrayMap(1 → 1, 2 → 2).justKeys(ArraySet(1)).isSpecialized)
    assert(ArrayMap(1 → 1, 2 → 2).exceptKeys(ArraySet(1)).isSpecialized)

    // for a mixed primitive/anyref map there is no specialization (partial specialization does not work).
    // but we can still use a primitive array for the key array because we have the class tag
    assert(ArrayMap.empty[Int, String].keys0.isIntArray)
    assert(ArrayMap.singleton(1, "1").keys0.isIntArray)
    assert(ArrayMap(1 -> "1", 2 -> "2").keys0.isIntArray)
    assert(ArrayMap(1 -> "1", 2 -> "2").justKeys(ArraySet(1)).keys0.isIntArray)
    assert(ArrayMap(1 -> "1", 2 -> "2").exceptKeys(ArraySet(1)).keys0.isIntArray)
  }

  test("multiMapSpecialization") {
    assert(ArrayMultiMap.empty[Int, Int].isSpecialized)
    assert(ArrayMultiMap.singleton(1, ArraySet(1, 2)).isSpecialized)
    assert(ArrayMultiMap.fromKVs(1 → 1, 1 → 2).isSpecialized)
    assert(ArrayMultiMap.fromKVs(1 → 1, 1 → 2).apply(1).isSpecialized)
    assert(ArrayMultiMap.fromKVs(1 → 1, 1 → 2).apply(1).elements.isIntArray)
    assert(ArrayMultiMap.fromKVs(1 → 1, 1 → 2).map.keys0.isIntArray)
  }

  test("biMapSpecialization") {
    assert(ArrayBiMap.empty[Int, Int].isSpecialized)
    assert(ArrayBiMap.singleton(0, 0).isSpecialized)
    assert(ArrayBiMap(1 → 1, 1 → 2).isSpecialized)
    assert(ArrayBiMap(1 → 1, 1 → 2).kv.isSpecialized)
    assert(ArrayBiMap(1 → 1, 1 → 2).vk.isSpecialized)

    // for a mixed primitive/anyref map there is no specialization (partial specialization does not work).
    // but we can still use a primitive array for the key array because we have the class tag
    assert(ArrayBiMap.empty[Int, String].kv.keys0.isIntArray)
    assert(ArrayBiMap.singleton(0, "0").kv.keys0.isIntArray)
    assert(ArrayBiMap(1 → "1", 1 → "2").kv.keys0.isIntArray)
    assert(ArrayBiMap(1 → "1", 1 → "2").vk.values0.isIntArray)
  }
}
