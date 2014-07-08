package com.gilt.storeroom

import scala.concurrent._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class InMemoryStore[K,V] extends IterableStore[K,V] {
  import collection.concurrent.Map

  val store: Map[K,V] = (new java.util.concurrent.ConcurrentHashMap).asScala

  override def get(k: K): Future[Option[V]] = Future.successful(store.get(k))
  override def put(kv: (K, Option[V])): Future[Unit] = {
    kv match {
      case (k, Some(v)) => store.put(k,v)
      case (k, None)    => store.remove(k)
    }
    Future.successful(())
  }

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Future[List[(K,V)]] = {
    Future.successful(store.iterator.drop(offset).take(limit).toList)
  }

}
