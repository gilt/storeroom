package com.gilt.storeroom.dynamodb

import com.gilt.storeroom._

import java.util.{ Map => JMap }
import java.util.concurrent.Executors

import scala.concurrent._
import scala.collection.JavaConverters._

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBClient, AmazonDynamoDB }
import com.amazonaws.services.dynamodbv2.model._

object DynamoStore {

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String): DynamoStore = {
    val processors = Runtime.getRuntime.availableProcessors
    this(tableName, primaryKeyColumn, valueColumn, processors)
  }

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String, numberWorkerThreads: Int): DynamoStore = {
    val client = new AmazonDynamoDBClient()
    new DynamoStore(client, tableName, primaryKeyColumn, valueColumn, numberWorkerThreads)
  }

}

class DynamoStore(val client: AmazonDynamoDB, val tableName: String,
  val primaryKeyColumn: String, val valueColumn: String, numberWorkerThreads: Int)
  extends IterableStore[String, AttributeValue]
{

  implicit val apiRequestFuturePool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberWorkerThreads))

  override def put(kv: (String, Option[AttributeValue])): Future[Unit] = {
    kv match {
      case (key, Some(value)) => {
        //write the new entry to AWS
        val attributes = Map(
          primaryKeyColumn -> new AttributeValue(key),
          valueColumn -> value
        ).asJava
        val putRequest = new PutItemRequest(tableName, attributes)

        Future { client.putItem(putRequest) }
      }

      case (key, None) => {
        val attributes = Map(primaryKeyColumn -> new AttributeValue(key)).asJava
        val deleteRequest = new DeleteItemRequest(tableName, attributes)

        Future { client.deleteItem(deleteRequest) }
      }

    }

  }

  override def get(k: String): Future[Option[AttributeValue]] = {
    val attributes = Map(primaryKeyColumn -> new AttributeValue(k)).asJava
    val getRequest = new GetItemRequest(tableName, attributes)

    Future {
      Option(client.getItem(getRequest).getItem).map(_.get(valueColumn))
    }
  }

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Future[List[(String, AttributeValue)]] = {
    val attributes = List(primaryKeyColumn, valueColumn)

    Future {
      var needed = offset + limit
      var scanRequest = new ScanRequest(tableName).withAttributesToGet(attributes.asJava).withLimit(needed)
      var lastKey: java.util.Map[String, AttributeValue] = null
      val buffer = collection.mutable.Buffer.empty[(String,AttributeValue)]

      do {
        val result = client.scan(scanRequest)
        buffer ++= result.getItems.asScala.map { kavMap =>
          (kavMap.get(primaryKeyColumn).getS, kavMap.get(valueColumn))
        }

        needed -= result.getCount
        lastKey = result.getLastEvaluatedKey
        scanRequest = scanRequest.withExclusiveStartKey(lastKey).withLimit(needed)

      } while (lastKey != null && needed > 0)

      // still accumulated the unneeded items above, so slice them out now
      buffer.slice(offset, offset + limit).toList
    }
  }

}

object DynamoStringStore {

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String): DynamoStringStore = {
    val processors = Runtime.getRuntime.availableProcessors
    this(tableName, primaryKeyColumn, valueColumn, processors)
  }

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String, numberWorkerThreads: Int): DynamoStringStore = {
    new DynamoStringStore(DynamoStore(tableName, primaryKeyColumn, valueColumn, numberWorkerThreads))
  }

}

class DynamoStringStore(underlying: DynamoStore)
  extends ConvertedIterableStore[String, AttributeValue, String](underlying)(_.getS)(new AttributeValue(_))


object DynamoSeqStore {

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String): DynamoSeqStore = {
    val processors = Runtime.getRuntime.availableProcessors
    this(tableName, primaryKeyColumn, valueColumn, processors)
  }

  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String, numberWorkerThreads: Int): DynamoSeqStore = {
    new DynamoSeqStore(DynamoStore(tableName, primaryKeyColumn, valueColumn, numberWorkerThreads))
  }

}

class DynamoSeqStore(underlying: DynamoStore)
  extends ConvertedIterableStore[String, AttributeValue, Seq[String]](underlying)(_.getSS.asScala)(l => new AttributeValue(l.asJava))
