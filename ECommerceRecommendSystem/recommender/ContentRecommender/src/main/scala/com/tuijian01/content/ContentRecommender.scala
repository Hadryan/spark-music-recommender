package com.tuijian01.content

import org.apache.spark.SparkConf
import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession
import org.jblas.DoubleMatrix


/**
  * 基于内容的协同过滤推荐
  */


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


object ContentRecommender {
  //定义MongoDB中存储的表名
  val MONGODB_PRODUCT_COLLECTION = "Product"
  val CONTENT_PRODUCT_RECS = "ContentBasedProductRecs"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://tuijian:27017/recommender",
      "mongo.db" -> "recommender"
    )

    //创建一个spark config
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("ContentRecommender")
    //创建sparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))


    //载入数据，做预处理
    val productTagsDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_PRODUCT_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Product]
      .map(
        x => (x.productId, x.name, x.tags.map(c => if (c == '|') ' ' else c))
      )
      .toDF("productId", "name", "tags")
      .cache()


    // TODO: 用TF-IDF 提取商品的特征向量
    /**
      * 1、实例化一个分词器，用来做分词，默认按照空格分
      */
    val tokenizer = new Tokenizer().setInputCol("tags").setOutputCol("words")

    /**
      * 2、用分词器进行转换，得到一个新列的words的DF
      */
    val wordsDataDF = tokenizer.transform(productTagsDF)

    /**
      * +---------+--------------------+--------------------+--------------------+
      * |productId|                name|                tags|               words|
      * +---------+--------------------+--------------------+--------------------+
      * |     3982|2013李宗盛:理性与感性作品音乐...|理性与感性 喜欢李宗盛 李宗盛理性 沧桑|[理性与感性, 喜欢李宗盛, 李宗...|
      * |   259637|张学友:私人角落 Private ...|     非常的好听 值得收藏 永远支持| [非常的好听, 值得收藏, 永远支持]|
      * |     6797|2013年 张杰专辑:爱,不解释(CD)|送货速度 唱出了心中 出道十年 十...|[送货速度, 唱出了心中, 出道十...|
      */
    //    wordsDataDF.show()


    /**
      * 3、定义一个HashingTF工具，计算频次
      */
    val hashingTF = new HashingTF().setInputCol("words").setOutputCol("rawFeatures").setNumFeatures(800)
    val featurizedDataDF = hashingTF.transform(wordsDataDF)


    /** rawFeatures 稀疏向量
      * (262144,[3007,51121,221379,250136],[1.0,1.0,1.0,1.0])
      */
    //    featurizedDataDF.show(truncate = false)

    /**
      * 4、定义一个IDF工具计算TF-IDF
      */
    val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")
    //训练一个IDF模型,算IDF相对应的东西
    val idfModel = idf.fit(featurizedDataDF)
    //得到增加新列features的DF
    val rescaledDataDF = idfModel.transform(featurizedDataDF)

    /** features 得到对应的词在整个文档中的重要程度
      * (262144,[3007,51121,221379,250136],[3.8815637979434374,3.8815637979434374,3.8815637979434374,3.8815637979434374])
      */
    rescaledDataDF.show(truncate = false)

    /**
      * 5、对数据进行转换，得到一个RDD形式的features
      *
      * SparseVector 稀疏向量的格式
      */
    val productFeatures = rescaledDataDF.map {
      row => (row.getAs[Int]("productId"), row.getAs[SparseVector]("features").toArray)
    }
      .rdd
      .map {
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
      .option("collection", CONTENT_PRODUCT_RECS)
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
