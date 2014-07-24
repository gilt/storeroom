package com.gilt.storeroom.dynamodb

import com.gilt.storeroom._

import java.util.{ Map => JMap }
import java.util.concurrent.Executors

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

import play.api.libs.iteratee.{Enumerator, Enumeratee}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.{ AmazonDynamoDBClient, AmazonDynamoDB }
import com.amazonaws.services.dynamodbv2.model._

object DynamoStore {

  /**
    * Create a Store for the specified table, primary key, and value column in Dynamo.
    * This assumes your AWS credentials are available in the environment, as described
    * in the AWS SDK documentation.
    *
    * Asynchronous operations are executed in a thread pool sized according to the
    * number of available processors
    *
    * @see <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html#AmazonDynamoDBClient()">SDK Javadoc</a>
    */
  def apply(tableName: String, primaryKeyColumn: String, valueColumn: String): DynamoStore = {
    val processors = Runtime.getRuntime.availableProcessors
    this(tableName, primaryKeyColumn, valueColumn, processors)
  }

  /**
    * Create a Store for the specified table, primary key, and value column in Dynamo.
    * This assumes your AWS credentials are available in the environment, as described
    * in the AWS SDK documentation.
    *
    * @see <a href="http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html#AmazonDynamoDBClient()">SDK Javadoc</a>
    */
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

        Future { blocking(client.putItem(putRequest)) }
      }

      case (key, None) => {
        val attributes = Map(primaryKeyColumn -> new AttributeValue(key)).asJava
        val deleteRequest = new DeleteItemRequest(tableName, attributes)

        Future { blocking(client.deleteItem(deleteRequest)) }
      }

    }

  }

  override def get(k: String): Future[Option[AttributeValue]] = {
    val attributes = Map(primaryKeyColumn -> new AttributeValue(k)).asJava
    val getRequest = new GetItemRequest(tableName, attributes)

    Future {
      Option(blocking(client.getItem(getRequest).getItem)).map(_.get(valueColumn))
    }
  }

  // TODO - implement multiGet and multiPut

  override def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Enumerator[(String, AttributeValue)] = {
    val attributes = List(primaryKeyColumn, valueColumn)

    val initialState: Option[Option[java.util.Map[String, AttributeValue]]] = None

    Enumerator.unfoldM(initialState) { state => state match {
      case Some(None) => Future.successful(None)
      case _ => {
        Future {
          val scanRequest = new ScanRequest(tableName)
            .withAttributesToGet(attributes.asJava)
            .withExclusiveStartKey(state.map(_.get).getOrElse(null))

          val result = blocking(client.scan(scanRequest))

          val lastKey = Option(result.getLastEvaluatedKey)

          val items = result.getItems.asScala.map { kavMap =>
            (kavMap.get(primaryKeyColumn).getS, kavMap.get(valueColumn))
          }

          Some((Some(lastKey), items))
        }
      }
    }}.flatMap(Enumerator.enumerate).through(Enumeratee.drop(offset)).through(Enumeratee.take(limit))
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
