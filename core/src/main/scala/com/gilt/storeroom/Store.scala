package com.gilt.storeroom

import scala.concurrent._
import play.api.libs.iteratee.Enumerator

trait Closeable {
  def close(): Future[Unit]
}

/**
  * Read-only store
  */
trait ReadableStore[K, +V] extends Closeable {
  /**
    * Get a single value from the store, or None if no entry
    * exists for the key.
    */
  def get(k: K): Future[Option[V]]

  /**
    * Get mutiple values at once.
    *
    * A default implementation using get() is provided,
    * but this should be overridden if a more efficient
    * implementation exists for a particular backing store.
    */
  def multiGet(ks: Set[K]): Map[K, Future[Option[V]]] =
    ks.map { k => (k, get(k)) }.toMap

  /**
    * Call this method to close the Store when you are done using it.
    *
    * This method should be overridden by implementations if any actions
    * need to be taken to close / disconnect from the store.
    */
  override def close() = Future.successful(())
}

/**
  * Write-only store
  */
trait WritableStore[K, -V] extends Closeable {
  /**
    * Add a new key-value pair to the store, or replace
    * an existing mapping.
    */
  def put(kv: (K, V)): Future[Unit]

  /**
    * Add multiple values at once.
    *
    * A default implementation using put() is provided,
    * but this should be overridden if a more efficient
    * implementation exists for a particular backing store.
    */
  def multiPut(kvs: Map[K, V]): Map[K, Future[Unit]] =
    kvs.map { kv => (kv._1, put(kv)) }

  /**
    * Call this method to close the Store when you are done using it.
    *
    * This method should be overridden by implementations if any actions
    * need to be taken to close / disconnect from the store.
    */
  override def close() = Future.successful(())
}

/**
  * Combined trait for a store which supports reads, writes, and deletes
  * (via writing None to a key).
  */
trait Store[K, V] extends ReadableStore[K, V] with WritableStore[K, Option[V]]


/**
  * Trait for stores which can be iterated over.
  */
trait IterableStore[K, V] extends Store[K,V] {
  /**
    * Get all of the mappings in the store.
    *
    * Optionally, limit and offset may be specified to page
    * through the entries in the store.
    */
  def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Enumerator[(K,V)]
}
