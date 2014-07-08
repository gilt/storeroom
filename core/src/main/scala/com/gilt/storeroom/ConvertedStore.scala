package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class ConvertedStore[K, V1, V2](s: Store[K, V1])(to: V1 => V2)(from: V2 => V1)
    extends Store[K,V2] {

  override def get(k: K): Future[Option[V2]] = s.get(k).map(_.map(to))
  override def put(kv: (K, Option[V2])): Future[Unit] = s.put((kv._1, kv._2.map(from)))

  //TODO optimized multi

  override def close() = s.close()
}

class ConvertedIterableStore[K, V1, V2](s: IterableStore[K, V1])(to: V1 => V2)(from: V2 => V1)
    extends ConvertedStore[K, V1, V2](s)(to)(from)
    with IterableStore[K, V2]
{

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Future[List[(K,V2)]] = s.getAll(limit, offset).map(_.map {
    case (k,v1) => (k, to(v1))
  })
}
