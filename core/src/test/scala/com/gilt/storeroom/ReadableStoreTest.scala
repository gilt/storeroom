package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures

object ReadableStoreTest {
  // should not compile, uncomment to test
  //class NoImpls[-K, +V] extends ReadableStore[K,V]

  class GetImpl(val map: Map[Int,Int]) extends ReadableStore[Int,Int] {
    override def get(k: Int) = Future.successful(map.get(k))
  }
}

class ReadableStoreTest extends FunSuite with Matchers with ScalaFutures {
  import ReadableStoreTest._

  // TODO: should probably use scalacheck properties

  test("can get an item in the store") {
    val store = new GetImpl(Map(1->2,3->4))
    store.get(1).futureValue should be(Some(2))
  }

  test("an item not in the store returns None") {
    val store = new GetImpl(Map(1->2,3->4))
    store.get(10).futureValue should be(None)
  }

  test("multiGet works correctly") {
    val store = new GetImpl(Map(1->2,3->4,5->6,7->8,9->10))
    // ugh... so annoying that Future.sequence won't work directly on a Map[A,Future[B]]
    val results = Future.sequence(store.multiGet(Set(1,2,3,4,5,6,7,8,9,10)).map {
      case (k,v) => v.map(k->_)
    }).futureValue.toMap
    results should be(Map(1->Some(2),2->None,3->Some(4),4->None,5->Some(6),6->None,7->Some(8),8->None,9->Some(10),10->None))
  }

  test("can close") {
    val map = Map.empty[Int,Int]
    val store = new GetImpl(map)
    store.close.futureValue
  }
}
