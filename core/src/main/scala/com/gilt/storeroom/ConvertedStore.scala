package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import TupleOps._

/**
  * ConvertedStore allows a Store to be wrapped via a pair of conversion
  * functions changing the type of the stored values.
  */
class ConvertedStore[K, V1, V2](s: Store[K, V1])(to: V1 => V2)(from: V2 => V1)
    extends Store[K,V2] {

  override def get(k: K): Future[Option[V2]] =
    s.get(k).map(_.map(to))

  override def multiGet(ks: Set[K]): Map[K, Future[Option[V2]]] =
    s.multiGet(ks).mapValues(_.map(_.map(to)))

  override def put(kv: (K, Option[V2])): Future[Unit] =
    s.put(kv.map2(_.map(from)))

  override def multiPut(kvs: Map[K, Option[V2]]): Map[K, Future[Unit]] =
    s.multiPut(kvs.mapValues(_.map(from)))

  override def close() = s.close()
}

class ConvertedIterableStore[K, V1, V2](s: IterableStore[K, V1])(to: V1 => V2)(from: V2 => V1)
    extends ConvertedStore[K, V1, V2](s)(to)(from)
    with IterableStore[K, V2]
{
  import play.api.libs.iteratee.Enumerator

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Enumerator[(K,V2)] =
    s.getAll(limit, offset).map(_.map2(to))
}


object TupleOps {
  implicit class Tuple2Ops[A,B](t: (A,B)) {
    def map1[A2](f: A => A2): (A2,B) = (f(t._1), t._2)
    def map2[B2](f: B => B2): (A,B2) = (t._1, f(t._2))
  }
}
