package com.gilt.storeroom

// storehaus inspired, but using standard Scala Futures

import scala.concurrent._

trait Closeable {
  def close(): Future[Unit]
}

trait ReadableStore[K, +V] extends Closeable {
  def get(k: K): Future[Option[V]]
  def multiGet(ks: Set[K]): Map[K, Future[Option[V]]] =
    ks.map { k => (k, get(k)) }.toMap
  override def close() = Future.successful(())
}

trait WritableStore[K, -V] extends Closeable {
  def put(kv: (K, V)): Future[Unit]
  def multiPut(kvs: Map[K, V]): Map[K, Future[Unit]] =
    kvs.map { kv => (kv._1, put(kv)) }
  override def close() = Future.successful(())
}

trait Store[K, V] extends ReadableStore[K, V] with WritableStore[K, Option[V]]

trait IterableStore[K, V] extends Store[K,V] {
  // Should perhaps be something like an Enumerator instead
  def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Future[List[(K,V)]]
}
