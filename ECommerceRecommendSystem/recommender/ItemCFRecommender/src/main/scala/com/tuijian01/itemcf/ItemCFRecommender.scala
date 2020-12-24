package com.tuijian01.itemcf

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession


/**
  * 基于协同过滤的推荐（同现矩阵算法）
  * 需要行为数据 就是评分数据
  */
/**
  * ratings数据集
  *
  * 4867          用户ID
  * 457976        商品ID
  * 5.0           评分数据
  * 1395676800    时间戳
  */
case class ProductRating(userId: Int, productId: Int, score: Double, timestamp: Long)

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
  *
  * 定义商品相似度列表
  */
case class ProductRecs(productId: Int, recs: Seq[Recommendation])


object ItemCFRecommender {
  //定义常量和表明
  val MONGODB_RATING_COLLECTION = "Rating"

  val ITEM_CF_PRODUCT_RECS = "ItemCFProductRecs"
  val MAX_RECOMMENDATION = 10

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ItemCFRecommender")
    //创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))


    //加载数据，转换成DF进行处理
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRating]
      .map(
        x => (x.userId, x.productId, x.score)
      )
      .toDF("userId", "productId", "score")
      .cache()


    //TODO: 核心算法 计算同现相似度得到商品的相似度列表
    /**
      * 1、统计每个商品的评分个数，按照productId来做group by
      */
    val productRatingCountDF = ratingDF.groupBy("productId").count()

    //    productRatingCountDF.show(10000000)

    /**
      * 2、在原有的评分表上rating，添加count这一列
      */
    val ratingWithCountDF = ratingDF.join(productRatingCountDF, "productId")

    //    ratingWithCountDF.show()

    /**
      * 3、将评分按照用户id进行两两配对join，统计两个商品被同一个用户评分过的次数
      */
    val joinedDF = ratingWithCountDF.join(ratingWithCountDF, "userId")
      .toDF("userId", "product1", "score1", "count1", "product2", "score2", "count2")
      .select("userId", "product1", "count1", "product2", "count2")

    //    joinedDF.show()
    /**
      * 创建一张临时表，用于写sql查询
      */
    joinedDF.createOrReplaceTempView("joined")

    /**
      * 按照product1,product2进行group by,统计userId的数量，就是对两个商品同时评分的次数（人数）
      * 按照product1,product2进行group by,统计userId的数量，就是对两个商品同时评分的次数（人数）
      */
    val cooccurrenceDF = spark.sql(
      """
        |select product1
        |,product2
        |,count(userId) as cocount
        |,first(count1) as count1
        |,first(count2) as count2
        |from joined
        |group by product1,product2
      """.stripMargin
    ).cache()

    //    cooccurrenceDF.show()
    /**
      * 提取需要的数据，包装成（productId1,(productId2,score)）
      */
    val simDF = cooccurrenceDF.map {
      row =>
        val coocSim = cooccurrenceSim(row.getAs[Long]("cocount"), row.getAs[Long]("count1"), row.getAs[Long]("count2"))
        (row.getInt(0), (row.getInt(1), coocSim))
    }
      .rdd
      .groupByKey()
      .map {
        case (productId, recs) =>
          ProductRecs(productId, recs.toList
            .filter(x => x._1 != productId)
            .sortWith(_._2 > _._2)
            .take(MAX_RECOMMENDATION)
            .map(x => Recommendation(x._1, x._2)))
      }
      .toDF()


    //保存到mongoDB
    simDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", ITEM_CF_PRODUCT_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


    spark.stop()


  }

  /**
    * 按照公式计算同现相似度
    *
    * @param coCount 同现的个数
    * @param count1  物品1的数量
    * @param count2  物品2的数量
    * @return
    */
  def cooccurrenceSim(coCount: Long, count1: Long, count2: Long): Double = {
    coCount / math.sqrt(count1 * count2)
  }


}
