package com.gilt.storeroom.dynamodb

import com.gilt.storeroom._

import scala.collection.JavaConverters._
import org.scalatest.time._
import play.api.libs.iteratee.Iteratee

import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBClient, AmazonDynamoDB }
import com.amazonaws.services.dynamodbv2.model._

class DynamodDbStoreTest extends IterableStoreTest[DynamoLongStore] {
  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val client = new AmazonDynamoDBClient()

  def emptyStore = emptyStore(1)

  def emptyStore(throughput: Int) = {
    val tableName = s"test_${scala.util.Random.nextInt}"
    val request = (new CreateTableRequest)
      .withTableName(tableName)
      .withAttributeDefinitions(new AttributeDefinition("key", ScalarAttributeType.S))
      .withKeySchema(new KeySchemaElement("key", KeyType.HASH))
      .withProvisionedThroughput(new ProvisionedThroughput(throughput,throughput))

    client.createTable(request)

    // createTable is async, so now wait for it to be available

    waitForCreation(tableName)

    DynamoLongStore(tableName, "key", "value")
  }


  override def cleanupStore(store: DynamoLongStore) = {
    val request = new DeleteTableRequest(store.tableName)
    client.deleteTable(request)
  }

  def waitForCreation(tableName: String) = {
    val request = new DescribeTableRequest(tableName)
    while (client.describeTable(request).getTable.getTableStatus != "ACTIVE") {
      Thread.sleep(1000)
    }
  }

  def withStore(throughput: Int)(f: DynamoLongStore => Unit) = {
    val store = emptyStore(throughput)
    try {
      f(store)
    } finally {
      cleanupStore(store)
    }
  }

  test("big getAll works") {
    // this might take a while
    implicit val patienceConfig =
      PatienceConfig(timeout = Span(240, Seconds), interval = Span(1, Seconds))

    withStore(10) { intStore =>
      // piggyback with a new string attribute
      val store = DynamoStringStore(intStore.tableName, "key", "big")

      // need at least 1MB to force multiple pages.  4 * 2500 * 200 = 2MB
      val items = (0L until 200).map(i => (i.toString, "asdf" * 2500)).toMap

      sequenceMapOfFutures(store.multiPut(items.mapValues(Some(_)))).futureValue

      // not using enumeratorValue because the implicit PatienceConfig goes all
      // wonky and I can't be bothered to sort it out.
      (store.getAll() run Iteratee.getChunks).futureValue should contain theSameElementsAs items.toList
    }
  }

}
