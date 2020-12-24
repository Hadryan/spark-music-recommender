package com.tuijian01.offline

import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix


/**
  * 离线训练模型，得到用户特征向量，物品特征向量，商品之间的相似度
  * 一个用户一个推荐列表
  *
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
  * 定义用户推荐列表
  *
  */
case class UserRecs(userId: Int, recs: Seq[Recommendation])

/**
  *
  * 定义商品相似度列表
  */
case class ProductRecs(productId: Int, recs: Seq[Recommendation])

object OfflineRecommender {
  //定义MongoDB中存储的表名
  val MONGODB_RATING_COLLECTION = "Rating"

  //业务系统读取并展示的离线推荐列表
  val USER_RECS = "UserRecs"

  //音乐相似度矩阵，供实时推荐进行使用
  val PRODUCT_RECS = "ProductRecs"
  val USER_MAX_RECOMMENDATION = 20

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("OfflineRecommender")
    //创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加载数据
    val ratingRDD = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[ProductRating]
      .rdd
      .map(
        rating => (rating.userId, rating.productId, rating.score)
      ).cache()


    //提取出所有用户和商品的数据集
    val userRDD = ratingRDD.map(_._1).distinct()
    val productRDD = ratingRDD.map(_._2).distinct()

    // 核心计算过程========================================================================
    /**
      * 1、训练隐语义模型的协同过滤训练
      */
    //包装成ALS训练模型的Rating数据结构
    val trainData = ratingRDD.map(x => Rating(x._1, x._2, x._3))

    //（隐特征个数K,迭代次数，经验值正则化系数） 定义模型训练的参数
    val (rank, iterations, lambda) = (5, 10, 0.01)
    val model = ALS.train(trainData, rank, iterations, lambda)


    /**
      * 2、获得预测评分矩阵，进而得到用户的推荐列表
      */

    //用userRDD和productRDD做一个笛卡尔积，得到一个空的userProductRDD
    val userProducts = userRDD.cartesian(productRDD)
    val preRating = model.predict(userProducts)

    //从预测评分矩阵中提取得到用户推荐列表
    val userRecs = preRating.filter(_.rating > 0)
      .map(
        rating => (rating.user, (rating.product, rating.rating))
      )
      .groupByKey()
      .map {
        case (userId, recs) =>
          UserRecs(userId, recs.toList.sortWith(_._2 > _._2).take(USER_MAX_RECOMMENDATION).map(x => Recommendation(x._1, x._2)))
      }
      .toDF()

    userRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", USER_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


    /**
      * 3、利用商品的特性向量，计算商品的相似度列表
      *
      * 主要时为了后面做实时推荐打下基础，如果要实时地给用户一个信息，一个反馈地话，实时计算相似进行推荐是来不及的
      * 在商品信息相对稳定的业务场景里面，先把相似度算出来，实时直接用就比较快了。
      *
      * DoubleMatrix 传入一个数组转换成对应的矩阵
      */
    val productFeatures = model.productFeatures.map {
      case (productId, features) => (productId, new DoubleMatrix(features))
    }
    //两两配对商品，计算余弦相似度 过滤掉与自己的相似度，因为很高
    val productRecs = productFeatures.cartesian(productFeatures)
      .filter {
        case (a, b) => a._1 != b._1
      }
      //计算余弦相似度
      .map {
      case (a, b) =>
        val simScore = consinSim(a._2, b._2)
        (a._1, (b._1, simScore))
    }
      .filter(_._2._2 > 0.4)
      .groupByKey()
      .map {
        case (productId, recs) =>
          ProductRecs(productId, recs.toList.sortWith(_._2 > _._2).map(x => Recommendation(x._1, x._2)))
      }
      .toDF()

    productRecs.write
      .option("uri", mongoConfig.uri)
      .option("collection", PRODUCT_RECS)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


    spark.stop()


  }


  /**
    * 计算余弦相似度 (dot点积，模长其实就是向量的范数2 norm2)
    *
    * @param product1 第一个商品的特征向量
    * @param product2 第二个商品的特征向量
    * @return
    */
  def consinSim(product1: DoubleMatrix, product2: DoubleMatrix): Double = {
    product1.dot(product2) / (product1.norm2() * product2.norm2())


  }


}
