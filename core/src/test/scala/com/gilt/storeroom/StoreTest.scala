package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures

trait StoreTest[T <: Store[String,Long]] extends  FunSuite with Matchers with ScalaFutures {
  def emptyStore: T
  def cleanupStore(store: T): Unit = {}

  def withStore(f: T => Unit) = {
    val store = emptyStore
    try {
      f(store)
    } finally {
      cleanupStore(store)
    }
  }

  // TODO: should probably use scalacheck properties

  test("get on an empty store returns None") {
    withStore { store =>
      store.get("1").futureValue should be(None)
    }
  }

  test("put than get finds item") {
    withStore { store =>
      store.put("1"->Some(2)).futureValue
      store.get("1").futureValue should be(Some(2))
    }
  }

  test("second put replaces item") {
    withStore { store =>
      store.put("1"->Some(2)).futureValue
      store.put("1"->Some(3)).futureValue
      store.get("1").futureValue should be(Some(3))
    }
  }

  test("can delete with put") {
    withStore { store =>
      store.put("1"->Some(2)).futureValue
      store.put("1"->None).futureValue
      store.get("1").futureValue should be(None)
    }
  }

  test("can close") {
    withStore { store =>
      store.close.futureValue
    }
  }
}


trait IterableStoreTest[T <: IterableStore[String,Long]] extends StoreTest[T] {

  test("getAll works") {
    withStore { store =>
      val items = Map("1"->2,"3"->4,"5"->6)

      sequenceMapOfFutures(store.multiPut(items.mapValues(Some(_)))).futureValue

      enumeratorValue(store.getAll()) should contain theSameElementsAs items.toList
    }
  }
}
