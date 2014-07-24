package com.gilt

package object storeroom {
  import play.api.libs.iteratee.{Enumerator, Iteratee}
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.scalatest.concurrent.ScalaFutures._

  def enumeratorValue[T](e: Enumerator[T]): List[T] =
    (e map (List(_)) run Iteratee.consume[List[T]]()).futureValue


}
