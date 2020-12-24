package com.tuijian01.statistics

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}


/**
  * ratings数据集
  *
  * 4867          用户ID
  * 457976        商品ID
  * 5.0           评分数据
  * 1395676800    时间戳
  */
case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

/**
  * MongoDB连接配置
  *
  * @param uri MongoDB连接的uri
  * @param db  要操作的db
  */
case class MongoConfig(uri: String, db: String)

object StatisticsRecommender {

  //定义MongoDB中存储的表名
  val MONGODB_RATING_COLLECTION = "Rating"

  //统计的表的名称
  //历史热门统计
  val RATE_MORE_PRODUCTS = "RateMoreProducts"
  //最近评分高的商品
  val RATE_MORE_RECENTLY_PRODUCTS = "RateMoreRecentlyProducts"
  //平均评分高（优质）的商品
  val AVERAGE_PRODUCTS = "AverageProducts"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StatisticsRecommender")
    //创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    //加载数据
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()


    //创建一张ratings的临时表(临时视图)
    ratingDF.createOrReplaceTempView("ratings")

    // TODO:用spark sql 去做不同的统计推荐
    /**
      * 1、历史热门商品 （按照评分个数统计） productId,count
      */

    val rateMoreProductsDF = spark.sql("select productId,count(productId) as count from ratings group by productId order by count desc")
    //    rateMoreProductsDF.show()

    storeDFInMongoDB(rateMoreProductsDF, RATE_MORE_PRODUCTS)

    /**
      * 2、近期热门商品 （把时间戳转换成yyyyMM年月的格式进行评分个数统计）productId,count,yearmonth
      */

    //创建一个日期的格式化工具 date +%s 在linux中看时间戳秒数
    val simpleDateFormat = new SimpleDateFormat("yyyyMM")
    //注册UDF将timestamp转化为年月格式
    spark.udf.register("changeDate", (x: Int) => simpleDateFormat.format(new Date(x * 1000L)).toInt)
    //把原始rating数据转换成想要的结构 productId,score,yearmonth
    val ratingOfYearMonthDF = spark.sql("select productId,score,changeDate(timestamp) as yearmonth from ratings")
//    ratingOfYearMonthDF.show()
    ratingOfYearMonthDF.createOrReplaceTempView("ratingOfMonth")
    val rateMoreRecentlyProductsDF = spark.sql("select productId,count(productId) as count,yearmonth from ratingOfMonth group by yearmonth,productId order by yearmonth desc,count desc")
    //把DF保存到mongoDB
    storeDFInMongoDB(rateMoreRecentlyProductsDF,RATE_MORE_RECENTLY_PRODUCTS)

    /**
      * 3、优质商品统计 （商品的平均评分） productId,avg
      */
    val averageProductsDF = spark.sql("select productId,avg(score) as avg from ratings group by productId order by avg desc")
    storeDFInMongoDB(averageProductsDF,AVERAGE_PRODUCTS)

    spark.stop()


  }

  /**
    * 定义往mongoDb数据库存的方法
    *
    * @param df              传入数据的DF
    * @param collection_nmae 需要创建的表名
    * @param mongoConfig     mongoDB的一些连接配置
    */
  def storeDFInMongoDB(df: DataFrame, collection_nmae: String)(implicit mongoConfig: MongoConfig): Unit = {
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_nmae)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()


  }


}
