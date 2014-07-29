package com.gilt.storeroom

class InMemoryStoreTest extends IterableStoreTest[InMemoryStore[String,Long]] {
  def emptyStore = new InMemoryStore[String,Long]
}
