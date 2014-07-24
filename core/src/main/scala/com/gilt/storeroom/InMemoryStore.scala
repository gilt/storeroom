package com.gilt.storeroom

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import play.api.libs.iteratee.Enumerator

/**
  * An in-memory Store implementation.  Generally this should
  * only be used for testing or as a cache, since there is no
  * persistence of data once the process terminates.
  */

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

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Enumerator[(K,V)] = {
    Enumerator.enumerate(store.iterator.drop(offset).take(limit))
  }

}
