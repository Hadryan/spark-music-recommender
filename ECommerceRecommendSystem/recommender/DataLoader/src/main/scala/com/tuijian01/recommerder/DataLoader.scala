package com.tuijian01.recommerder

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}


/**
  * products 数据集（物品数据集）
  *
  * 13316                                                商品 ID
  * 汪峰:第9张创作大碟 生无所求(2CD)                     商品名称
  * 1057,268,378                                         商品分类ID,不需要
  * B003TJ99M4                                           亚马逊ID，也不需要
  * https://images-cn.ssl-images-amazo                   商品图片URL
  * 音质|摇滚|原创|汪峰创作|创作大碟                     商品的分类
  * 生无所求|喜欢汪峰|值得收藏|真正的音乐                用户生成的UGC标签（基于内容推荐，进行特征的提取和处理）
  */
//对应的商品样例类
case class Product(productId: Int, name: String, imageUrl: String, categories: String, tags: String)

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

object DataLoader {
  //定义数据文件路径
  val PRODUCT_DATA_PATH = "E:\\ECommerceRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\songs.csv"
  val RATING_DATA_PATH = "E:\\ECommerceRecommendSystem\\recommender\\DataLoader\\src\\main\\resources\\ratings.csv"
  //定义MongoDB中存储的表名
  val MONGODB_PRODUCT_COLLECTION = "Product"
  val MONGODB_RATING_COLLECTION = "Rating"


  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("DataLoader")
    //创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._


    //加载数据
    val productRDD = spark.sparkContext.textFile(PRODUCT_DATA_PATH)
    //因为最后是要存储到mongoDB中的，需要表结构的，就要将其转成DataFrame
    val productDF = productRDD.map(item => {
      //数据通过^进行分割，转换成所需的样例类，两个\\表示一个\转义
      val attr = item.split("\\^")
      //转换成Product
      Product(attr(0).toInt, attr(1).trim, attr(4).trim, attr(5).trim, attr(6).trim)
    }).toDF()

    //    productDF.show()


    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF = ratingRDD.map(item => {
      val attr = item.split(",")
      Rating(attr(0).toInt, attr(1).toInt, attr(2).toDouble, attr(3).toInt)
    }).toDF()

    //    ratingDF.show()


    //考虑到后面调用mongDB的配置不止一次，每一次没有必要都传入，就将它写成隐式的参数
    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))


    storeDataInMongoDB(productDF, ratingDF)


        spark.stop()

  }

  //定义函数 保存数据到MongoDB
  def storeDataInMongoDB(productDF: DataFrame, ratingDF: DataFrame)(implicit mongoConfig: MongoConfig): Unit = {
    //新建一个到MongoDB的连接
    val mongoClient = MongoClient(MongoClientURI(mongoConfig.uri))

    //定义要操作的MongoDB中的表,函数柯里化  可以理解成 db.Product,获取到里面的表
    val productCollection = mongoClient(mongoConfig.db)(MONGODB_PRODUCT_COLLECTION)
    val ratingCollection = mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)

    //如果表已经存在则删掉
    productCollection.dropCollection()
    ratingCollection.dropCollection()

    //将当前数据存入对应的表中
    productDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_PRODUCT_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF.write
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    //对表创建索引
    productCollection.createIndex(MongoDBObject("productId" -> 1))
    ratingCollection.createIndex(MongoDBObject("productId" -> 1))
    ratingCollection.createIndex(MongoDBObject("userId" -> 1))

    //关闭mongoDB的连接
    mongoClient.close()

  }

}
