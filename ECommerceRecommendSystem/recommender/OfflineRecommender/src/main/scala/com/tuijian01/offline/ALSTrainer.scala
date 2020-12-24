package com.tuijian01.offline

import breeze.numerics.sqrt
import com.tuijian01.offline.OfflineRecommender.MONGODB_RATING_COLLECTION
import org.apache.spark.SparkConf
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession


/**
  * 模型评估选取最优参数  均方根误差RMSE 越小的那一组参数就是优的
  *
  *     (5,0.1,1.3026174687833083)
  */
object ALSTrainer {
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
        rating => Rating(rating.userId, rating.productId, rating.score)
      ).cache()


    /**
      * 不能直接那原来的数据进行评估，需要先将数据集切分成训练集和测试集
      *
      */

    //二八开的数据集
    val splits = ratingRDD.randomSplit(Array(0.8, 0.2))
    //八为训练集
    val trainingRDD = splits(0)
    //二为测试集
    val testingRDD = splits(1)

    //核心实现，输出最优参数
    abjustALSParams(trainingRDD, testingRDD)


    spark.stop()

  }

  def abjustALSParams(trainData: RDD[Rating], testData: RDD[Rating]): Unit = {

    //遍历数组中定义的参数取值 yield 关键字把每一次的for循环的结果保存下来
    val result = for (rank <- Array(5, 10, 20, 50); lambda <- Array(1, 0.1, 0.01))
      yield {
        val model = ALS.train(trainData, rank, 10, lambda)
        val rmse = getRMSE(model, testData)
        (rank, lambda, rmse)
      }
    //那招rmse排序并输出最优参数
    println(result.minBy(_._3))

  }

  def getRMSE(model: MatrixFactorizationModel, data: RDD[Rating]): Double = {
    //构建userProduct,得到预测评分矩阵 因为里面是样例类，所以不能用_下划线进行取值
    val userProducts = data.map(item => (item.user, item.product))
    //得到每一个用户对每一个商品的预测评分
    val predicRating = model.predict(userProducts)

    //按照公式计算rmse 首先把预测评分和实际评分表按照（userId,productId）做一个连接 mean()这个方法直接求出均值
    //真实评分
    val observed = data.map(item => ((item.user, item.product), item.rating))
    //预测评分
    val predict = predicRating.map(item => ((item.user, item.product), item.rating))

    sqrt(
      observed.join(predict).map {
        case ((userId, productId), (actual, pre)) =>
          val err = actual - pre
          err * err
      }.mean()
    )


  }

}
