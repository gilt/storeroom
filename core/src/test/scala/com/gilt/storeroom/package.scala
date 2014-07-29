package com.gilt

import play.api.libs.iteratee.{Enumerator, Iteratee}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures._

package object storeroom {

  def enumeratorValue[T](e: Enumerator[T]): List[T] =
    (e run Iteratee.getChunks).futureValue

  // Future.sequence won't work directly on a Map[A,Future[B]]
  def sequenceMapOfFutures[A,B](futures: Map[A, Future[B]]): Future[Iterable[(A,B)]] =
    Future.sequence(futures.map {
      case (k,v) => v.map(k->_)
    })

}
