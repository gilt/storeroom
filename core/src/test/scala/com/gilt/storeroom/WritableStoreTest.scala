package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.collection.mutable.Map
import scala.collection.immutable.{Map => IMap}

object WritableStoreTest {
  // should not compile, uncomment to test
  //class NoImpls[-K, -V] extends WritableStore[K,V]

  class PutImpl(val map: Map[Int,Int]) extends WritableStore[Int,Int] {
    override def put(kv: (Int,Int)) = {
      map.put(kv._1, kv._2)
      Future.successful(())
    }
  }
}

class WritableStoreTest extends FunSuite with Matchers with ScalaFutures {
  import WritableStoreTest._

  // TODO: should probably use scalacheck properties

  test("can put an item in the store") {
    val map = Map.empty[Int,Int]
    val store = new PutImpl(map)
    whenReady(store.put((1,2))) { _ =>
      map(1) should be(2)
    }
  }

  test("multiPut works correctly") {
    val map = Map.empty[Int,Int]
    val store = new PutImpl(map)

    // ugh... so annoying that Future.sequence won't work directly on a Map[A,Future[B]]
    Future.sequence(store.multiPut(IMap(1->2,3->4,5->6,7->8,9->10)).map {
      case (k,v) => v.map(k->_)
    }).futureValue

    map should be(Map(1->2,3->4,5->6,7->8,9->10))
  }

  test("can close") {
    val map = Map.empty[Int,Int]
    val store = new PutImpl(map)
    store.close.futureValue
  }
}
