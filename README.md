# Storeroom

Storeroom is a general interface to key-value stores.  It is inspired by Twitter's [Storehaus](https://github.com/twitter/storehaus) library, but uses standard Scala Futures (and supports a substantially smaller number of back-end implementations).

## storeroom-core

The primary abstractions are ReadableStore, WriteableStore, and the combined read-write Store:

    trait ReadableStore[K, +V] extends Closeable {
      def get(k: K): Future[Option[V]]
      def multiGet[K](ks: Set[K]): Map[K, Future[Option[V]]]
      def close()
    }

    trait WritableStore[K, -V] extends Closeable {
      def put(kv: (K, V)): Future[Unit]
      def multiPut[K](kvs: Map[K, V]): Map[K, Future[Unit]]
      def close()
    }

    trait Store[K, V] extends ReadableStore[K, V] with WritableStore[K, Option[V]]

For backing stores which support it, it's also possible to iterate over all the entries in the store:

    trait IterableStore[K, V] extends Store[K,V] {
      def getAll(limit: Int = Int.MaxValue, offset: Int = 0): Future[List[(K,V)]]
    }


## implementations

Currently only two implementations are provided:

* InMemoryStore - for simple caches, etc
* DynamoStore

## storeroom-dynamodb

To use the DynamoDB implementation, add to your dependencies:

    "com.gilt" %% "storeroom-dynamodb" % "0.0.1"

and instantiate with your table details:

    object DynamoStore {
        def apply(client: AmazonDynamoDBClient, tableName: String, primaryKeyColumn: String, valueColumn: String): DynamoStore
    }

If your AWS credentials are available in the environment, as described [here](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/AmazonDynamoDBClient.html#AmazonDynamoDBClient()), you may omit `client`.

Note that when running the dynamodb tests, your credentials must be provided by your environment.  The test will fail if acceptable credentials aren't available.  Running the tests will cost you money (albeit a very small amount).
