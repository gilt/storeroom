package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures

trait StoreTest extends  FunSuite with Matchers with ScalaFutures {
  def emptyStore: Store[Int,Int]

  // TODO: should probably use scalacheck properties

  test("get on an empty store returns None") {
    val store = emptyStore
    store.get(1).futureValue should be(None)
  }

  test("put than get finds item") {
    val store = emptyStore
    store.put(1->Some(2)).futureValue
    store.get(1).futureValue should be(Some(2))
  }

  test("second put replaces item") {
    val store = emptyStore
    store.put(1->Some(2)).futureValue
    store.put(1->Some(3)).futureValue
    store.get(1).futureValue should be(Some(3))
  }

  test("can delete with put") {
    val store = emptyStore
    store.put(1->Some(2)).futureValue
    store.put(1->None).futureValue
    store.get(1).futureValue should be(None)
  }

  test("can close") {
    val store = emptyStore
    store.close.futureValue
  }
}


trait IterableStoreTest extends StoreTest {
  override def emptyStore: IterableStore[Int,Int]

  test("getAll works") {
    val store = emptyStore
    val items = Map(1->2,3->4,5->6)

    // ugh... so annoying that Future.sequence won't work directly on a Map[A,Future[B]]
    Future.sequence(store.multiPut(items.mapValues(Some(_))).map {
      case (k,v) => v.map(k->_)
    }).futureValue

    enumeratorValue(store.getAll()) should contain theSameElementsAs items.toList
  }

}
