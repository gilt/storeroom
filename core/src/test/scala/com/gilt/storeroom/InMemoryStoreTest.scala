package com.gilt.storeroom

class InMemoryStoreTest extends IterableStoreTest {
  def emptyStore = new InMemoryStore[Int,Int]
}
