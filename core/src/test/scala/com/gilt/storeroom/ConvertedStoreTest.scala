package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

class ConvertedStoreTest extends FunSuite with Matchers with ScalaFutures with MockitoSugar {

  test("gets are converted") {
    val underlying = mock[Store[Int,Int]]
    when(underlying.get(1)).thenReturn(Future.successful(Some(2)))
    when(underlying.get(2)).thenReturn(Future.successful(None))

    val store = new ConvertedStore(underlying)(_+1)(_-1)

    store.get(1).futureValue should be(Some(3))
    store.get(2).futureValue should be(None)
  }

  test("puts are converted") {
    val underlying = mock[Store[Int,Int]]
    when(underlying.put(any())).thenReturn(Future.successful(()))

    val store = new ConvertedStore(underlying)(_+1)(_-1)

    store.put(1->Some(2)).futureValue

    verify(underlying).put(1->Some(1))
  }

  test("multiGets are converted") {
    val underlying = mock[Store[Int,Int]]
    when(underlying.multiGet(Set(1,2))).thenReturn(Map(1 -> Future.successful(Some(2)), 2 -> Future.successful(None)))

    val store = new ConvertedStore(underlying)(_+1)(_-1)

    Future.sequence(store.multiGet(Set(1,2)).map {
      case (k,v) => v.map(k->_)
    }).futureValue.toMap should be(Map(1 -> Some(3), 2 -> None))
  }

  test("multiPuts are converted") {
    val underlying = mock[Store[Int,Int]]
    when(underlying.multiPut(Map(1 -> Some(1), 2 -> None))).thenReturn(Map(1 -> Future.successful(()), 2 -> Future.successful(())))

    val store = new ConvertedStore(underlying)(_+1)(_-1)

    store.multiPut(Map(1->Some(2), 2 -> None)).mapValues(_.futureValue)

    verify(underlying).multiPut(Map(1 -> Some(1), 2 -> None))
  }

  test("close is passed through") {
    val underlying = mock[Store[Int,Int]]
    when(underlying.close()).thenReturn(Future.successful(()))

    val store = new ConvertedStore(underlying)(_+1)(_-1)

    store.close().futureValue

    verify(underlying).close()
  }

  test("getAll is converted") {
    val underlying = mock[IterableStore[Int,Int]]
    when(underlying.getAll(any(), any())).thenReturn(Future.successful(List(1->2,3->4)))

    val store = new ConvertedIterableStore(underlying)(_+1)(_-1)

    store.getAll().futureValue should be(List(1->3,3->5))
  }

  test("failures are propagated") {
    val underlying = mock[IterableStore[Int,Int]]
    when(underlying.get(any())).thenReturn(Future.failed(new MyException()))
    when(underlying.put(any())).thenReturn(Future.failed(new MyException()))
    when(underlying.getAll(any(), any())).thenReturn(Future.failed(new MyException()))
    when(underlying.close()).thenReturn(Future.failed(new MyException()))

    val store = new ConvertedIterableStore(underlying)(_+1)(_-1)

    intercept[TestFailedException] {
      store.get(1).futureValue
    }.getCause shouldBe a [MyException]

    intercept[TestFailedException] {
      store.put(1->Some(2)).futureValue
    }.getCause shouldBe a [MyException]

    intercept[TestFailedException] {
      store.getAll().futureValue
    }.getCause shouldBe a [MyException]

    intercept[TestFailedException] {
      store.close.futureValue
    }.getCause shouldBe a [MyException]

  }


  class MyException extends Exception
}
