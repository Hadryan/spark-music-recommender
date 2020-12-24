package com.tuijian01.online

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis


/**
  * 定义一个连接助手对象，建立到redis到mongodb的连接
  *
  * scala中没有impl实现的方法这一说，都是extends，extends一个接口相当于是实现一个接口
  */
object ConnHelper extends Serializable {
  //懒变量定义，使用的时候才去初始化
  lazy val jedis = new Jedis("localhost")
  lazy val mongoClient = MongoClient(MongoClientURI("mongodb://tuijian:27017/recommender"))
}


/**
  * MongoDB连接配置
  *
  * @param uri MongoDB连接的uri
  * @param db  要操作的db
  */
case class MongoConfig(uri: String, db: String)

/**
  * 定义标准推荐对象
  */
case class Recommendation(productId: Int, score: Double)

/**
  * 定义用户推荐列表
  *
  */
case class UserRecs(userId: Int, recs: Seq[Recommendation])

/**
  *
  * 定义商品相似度列表
  */
case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object OnlineRecommender {
  //定义常量和表名
  val STREAM_RECS = "StreamRecs"
  val PRODUCT_RECS = "ProductRecs"
  val MONGODB_RATING_COLLECTION = "Rating"

  val MAX_USER_RATING_NUM = 20
  val MAX_SIM_PRODUCTS_NUM = 20

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender",
      "kafka.topic" -> "recommender"
    )

    //创建sparkConfig
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OnlineRecommender")
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(2))

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))


    //加载数据，相似度矩阵，为了性能考虑，广播出去
    val simProductsMatrix = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", PRODUCT_RECS)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRecs]
      .rdd
      //为了后续查询相似度方便，把数据转换成 K-V格式 DF没有collectAsMap方法
      .map { item =>
      (item.productId, item.recs.map(x => (x.productId, x.score)).toMap)
    }
      .collectAsMap()

    //定义广播变量
    val simProductsMatrixBC = sc.broadcast(simProductsMatrix)

    //创建连接Kafka的配置参数
    val kafkaParam = Map(
      "bootstrap.servers" -> "tuijian:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "recommender",
      "auto.offset.reset" -> "latest"
    )

    //创建一个DStream
    val kafkaStream = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(config("kafka.topic")), kafkaParam)
    )

    //对kafkaStream进行处理，产生评分流，必须包含四个元素  userId|productId|score|timestamp
    val ratingStream = kafkaStream.map { msg =>
      val attr = msg.value().split("\\|")
      (attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }

    //    ratingStream.print()
    //核心算法部分，定义评分流的处理流程
    ratingStream.foreachRDD {
      adds =>
        adds.foreach {
          case (userId, productId, score, timestamp) =>
            println("kafka数据到来！>>>>>>>>>>>>>>>>>>>")

            /**
              * 核心算法流程
              */
            // 1、从redis取出当前用户的最近评分，保存成一个数组Array[(productId,score)]
            val userRecentlyRatings = getUserRecentlyRatings(MAX_USER_RATING_NUM, userId, ConnHelper.jedis)


            // 2、从相似度矩阵中获取当前商品最相似的商品列表，作为备选列表，保存成一个数组Array[productId]
            val candidateProducts = getTopSimProducts(MAX_SIM_PRODUCTS_NUM, productId, userId, simProductsMatrixBC.value)


            // 3、计算每个备选商品的推荐优先级，得到当前用户的实时推荐列表，保存成一个数组Array[productId,score]
            val streamRecs = computeProductScore(candidateProducts, userRecentlyRatings, simProductsMatrixBC.value)

            println("将数据保存到数据库")

            // 4、把推荐列表保存到mongoDB
            saveDataToMongoDB(userId, streamRecs)
        }
    }

    //启动straming
    ssc.start()
    println("流处理开始！>>>>>>>>>>>>>>>>>>>>>>>>")

    ssc.awaitTermination()

  }

  /**
    * 从redis里获取最近num次评分
    *
    */
  //引入这个之后，可以对Java的数据结构进行map操作了
  import scala.collection.JavaConversions._

  def getUserRecentlyRatings(num: Int, userId: Int, jedis: Jedis): Array[(Int, Double)] = {
    //从redis用户的评分队列里获取评分数据，list健名为uid:USERID,值的格式是PRODUCTID:SCORE
    jedis.lrange("userId:" + userId.toString, 0, num)
      .map { item =>
        val attr = item.split("\\:")
        (attr(0).trim.toInt, attr(1).trim.toDouble)
      }
      .toArray
  }


  /**
    *
    * 获取当前商品的相似列表并过滤掉用户已经评分过的作为备选列表
    *
    * @param num         获取的个数
    * @param productId   商品Id
    * @param userId      用户Id
    * @param simProducts 相似度矩阵
    * @param mongoConfig mongoDB配置
    * @return
    */
  def getTopSimProducts(num: Int,
                        productId: Int,
                        userId: Int,
                        simProducts: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]])
                       (implicit mongoConfig: MongoConfig): Array[Int] = {
    //从广播变量相似度矩阵中拿到当前商品的相似度列表 x._1 是productId x._2是评分
    val allSimProducts = simProducts(productId).toArray


    //通过传进来的userID,获得用户已经评分过的商品，过滤掉,排序输出
    //从mongoDB获取到表
    val ratingCollection = ConnHelper.mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)
    val ratingExist = ratingCollection.find(MongoDBObject("userId" -> userId))
      .toArray
      .map { item => //只需要productId
        item.get("productId").toString.toInt
      }

    //从所有的相似商品中进行过滤
    allSimProducts.filter(x => !ratingExist.contains(x._1))
      .sortWith(_._2 > _._2)
      .take(num)
      .map(x => x._1)
  }


  /**
    * 计算每隔备选商品的推荐评分，优质评分
    *
    * @param candidateProducts   备选列表
    * @param userRecentlyRatings 最近的K次评分
    * @param simProducts         相似度矩阵
    */
  def computeProductScore(candidateProducts: Array[Int],
                          userRecentlyRatings: Array[(Int, Double)],
                          simProducts: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Array[(Int, Double)] = {
    //定义一个长度可变的数组ArrayBuffer,用它来保存每一个备选商品的基础得分,(productId,score)
    val scores = scala.collection.mutable.ArrayBuffer[(Int, Double)]()

    //定义两个Map用于保存每个商品的高分和低分的计数器（变量）,productId -> count
    val increMap = scala.collection.mutable.HashMap[Int, Int]()
    val decreMap = scala.collection.mutable.HashMap[Int, Int]()


    //遍历每一个备选商品，应该要去计算和已经评分商品的相似度
    for (candidateProduct <- candidateProducts; userRecentlyRating <- userRecentlyRatings) {
      //应该从相似度矩阵里面拿到当前备选商品和当前已评分商品的相似度
      val simScore = getProductsSimScore(candidateProduct, userRecentlyRating._1, simProducts)

      if (simScore > 0.4) {
        //按照公式进行加权计算,得到基础评分
        scores += ((candidateProduct, simScore * userRecentlyRating._2))
        if (userRecentlyRating._2 > 3) {
          increMap(candidateProduct) = increMap.getOrDefault(candidateProduct, 0) + 1
        } else {
          decreMap(candidateProduct) = decreMap.getOrDefault(candidateProduct, 0) + 1
        }
      }
    }

    //根据公式计算所有的推荐优先级，推荐评分，以productId做key进行groupBy
    scores.groupBy(_._1).map {
      case (productId, scoreList) =>
        (productId, scoreList.map(_._2).sum / scoreList.length + log(increMap.getOrDefault(productId, 1)) - log(decreMap.getOrDefault(productId, 1)))
    }.toArray
      //返回一个推荐列表，按照得分排序
      .sortWith(_._2 > _._2)
  }

  /**
    * 用于获取备选列表的商品与近期商品的一个相似度
    *
    * @param product1    备选商品Id
    * @param product2    近期的商品Id
    * @param simProducts 相似度矩阵
    * @return
    */
  def getProductsSimScore(product1: Int,
                          product2: Int,
                          simProducts: scala.collection.Map[Int, scala.collection.immutable.Map[Int, Double]]): Double = {
    simProducts.get(product1) match {
      case Some(sims) => sims.get(product2) match {
        case Some(score) => score
        case None => 0.0
      }
      case None => 0.0
    }
  }

  /**
    * 自定义log 以10为底
    *
    * 利用换底公式，将math的以e为底的公式进行转化
    *
    */
  def log(m: Int): Double = {
    val N = 10
    math.log(m) / math.log(N)

  }


  /**
    * 将结果写入MongoDB
    *
    * @param userId     用户Id
    * @param streamRecs 推荐列表（productId,score）
    */
  // 写入mongodb
  def saveDataToMongoDB(userId: Int, streamRecs: Array[(Int, Double)])(implicit mongoConfig: MongoConfig): Unit = {
    println("正在写入>>>>>>>>>>>>>>")
    val streamRecsCollection = ConnHelper.mongoClient(mongoConfig.db)(STREAM_RECS)
    // 按照userId查询并更新
    streamRecsCollection.findAndRemove(MongoDBObject("userId" -> userId))
    streamRecsCollection.insert(MongoDBObject("userId" -> userId,
      "recs" -> streamRecs.map(x => MongoDBObject("productId" -> x._1, "score" -> x._2))))

    println("写入完成>>>>>>>>>>>>>>")
  }


}
