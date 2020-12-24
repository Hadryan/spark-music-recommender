package com.wyh.ems.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.wyh.ems.entity.Product;
import com.wyh.ems.entity.RUser;
import com.wyh.ems.entity.Rating;
import com.wyh.ems.entity.recom.Recommendation;
import com.wyh.ems.entity.request.*;
import com.wyh.ems.service.ProductService;
import com.wyh.ems.service.RatingService;
import com.wyh.ems.service.RecommenderService;
import com.wyh.ems.service.UserService2;
import com.wyh.ems.utils.Constant;


import org.bson.Document;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@RequestMapping("product")
@Controller
public class ProductRestApi {

    private static Logger logger = Logger.getLogger(ProductRestApi.class.getName());

    @Autowired
    private RecommenderService recommenderService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService2 userService2;
    @Autowired
    private RatingService ratingService;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoClient mongoClient;


    /**
     * 查询各种推荐列表
     */
    //查询各种推荐列表
    @RequestMapping("findHotMusic")
    public String getHotProducts(Model model, HttpSession session) {
        RUser user = (RUser) session.getAttribute("user");
        String username = user.getUsername();
//        try {
//            BufferedWriter bw = new BufferedWriter(new FileWriter("F:\\Spring_boot\\ems\\src\\main\\java\\com\\wyh\\ems\\log\\agent.log"));
//            bw.write(Constant.PRODUCT_RATING_PREFIX + ":" + user.getUserId()  +"|"+ System.currentTimeMillis()/1000);
//            bw.flush();
//            bw.newLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //Constant.PRODUCT_RATING_PREFIX + ":" + userD.getUserId() +"|"+ id +"|"+ request.getScore() +"|"+ System.currentTimeMillis()/1000
        logger.info(Constant.PRODUCT_RATING_PREFIX + ":" + user.getUserId() + "|" + System.currentTimeMillis() / 1000);

        System.out.println(">>>>>>>>>>>>>>>>>>>>>实时推荐歌曲列表<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        //实时推荐列表
        try {
            RUser user3 = userService2.findByUsername(username);
            List<Recommendation> recommendations = recommenderService.getStreamRecommendations(new UserRecommendationRequest(user.getUserId(), 8));
            List<Product> onlineProducts = productService.getRecommendProducts(recommendations);
            model.addAttribute("onlineProducts", onlineProducts);
            for (Product product3 : onlineProducts) {
                System.out.println(product3.getProductId() + "===" + product3.getScore() + "=====" + product3.getImageUrl());
            }
        } catch (Exception e) {
            System.out.println("该用户暂时没有实时推荐列表");
        }


        System.out.println(">>>>>>>>>>>>>>>>>>>>>离线推荐歌曲列表<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        //针对每一个用户离线推荐歌曲列表
        try {
            RUser user2 = userService2.findByUsername(username);
            List<Recommendation> recommendations2 = recommenderService.getCollaborativeFilteringRecommendations(new UserRecommendationRequest(user2.getUserId(), 8));
            List<Product> offlineProducts = productService.getRecommendProducts(recommendations2);
            model.addAttribute("offlineProducts", offlineProducts);
            for (Product product2 : offlineProducts) {
                System.out.println(product2.getProductId() + "===" + product2.getScore() + "=====" + product2.getImageUrl());
            }
        } catch (Exception e) {
            System.out.println("该用户暂时没有推荐列表");
        }


        System.out.println(">>>>>>>>>>>>>>>>>>>>>获取评分多的歌曲<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        //获取评分多的歌曲
        List<Recommendation> recommendations1 = recommenderService.getRateMoreRecommendations(new RateMoreRecommendationRequest(8));
        List<Product> pingfenProducts = productService.getRecommendProducts(recommendations1);
        model.addAttribute("pingfenProducts", pingfenProducts);
        for (Product product1 : pingfenProducts) {
            System.out.println(product1.getProductId() + "===" + product1.getScore() + "=====" + product1.getImageUrl());
        }


        System.out.println(">>>>>>>>>>>>>>>>>>>>>获取热门的歌曲<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        //获取热门的歌曲
        List<Recommendation> recommendations = recommenderService.getHotRecommendations(new HotRecommendationRequest(8));
        List<Product> hotProducts = productService.getRecommendProducts(recommendations);
        model.addAttribute("hotProducts", hotProducts);

        for (Product product : hotProducts) {
            System.out.println(product.getProductId() + "===" + product.getScore() + "=====" + product.getImageUrl());
        }
//        return ">>>>>>" + recommendations;

        return "ems/shouye";
    }


    /**
     * 获取单个商品的信息
     */
    @GetMapping("xiangqing")
    public String getProductInfo(int productid, Model model, HttpSession session) {
        Product product = productService.findByProductId(productid);
        model.addAttribute("product", product);
//        session.setAttribute("products",products);
        System.out.println(product.getName() + "==" + product.getImageUrl() + "==" + product.getScore() + "==" + product.getTags() + "==" + product.getCategories());
        return "ems/xiangqing";
//        return "";
    }


    // 基于物品的协同过滤
    @GetMapping(value = "itemcf")
    public String getItemCFProducts(int pid2, Model model) {
        try{
            List<Recommendation> recommendations = recommenderService.getItemCFRecommendations(new ItemCFRecommendationRequest(pid2));
            List<Product> itemProducts = productService.getRecommendProducts(recommendations);
            model.addAttribute("itemProducts", itemProducts);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>基于协同过滤推荐的歌曲<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            for (Product product5 : itemProducts) {
                System.out.println(product5.getProductId() + "===" + product5.getName()+ "===" + product5.getScore() + "=====" + product5.getImageUrl());
            }
        }catch (Exception e){
            System.out.println("该歌曲暂时没有推荐");
            String s1 = "该歌曲基于协同过滤算法暂时没有推荐";
            model.addAttribute("itemNull",s1);
        }

        return "ems/itemCF";
//        return "";
    }

    /**
     * 基于内容的推荐
     */

    @GetMapping(value = "neirong")
    public String getContentBasedProducts(int pid1, Model model) {
        try{
            List<Recommendation> recommendations = recommenderService.getContentBasedRecommendations(new ContentBasedRecommendationRequest(pid1));
            List<Product> neirongProducts = productService.getRecommendProducts(recommendations);
            model.addAttribute("neirongProducts", neirongProducts);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>基于内容推荐的歌曲<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            for (Product product4 : neirongProducts) {
                System.out.println(product4.getProductId() + "===" + product4.getName()+ "===" + product4.getScore() + "=====" + product4.getImageUrl());
            }
        }catch (Exception e){
            System.out.println("该歌曲暂时没有推荐");
            String s = "该歌曲基于内容暂时没有推荐";
            model.addAttribute("neirongNull",s);
        }

        return "ems/neiRong";
//        return "";
    }


    /**
     * 模糊查询商品
     */
    @RequestMapping(value = "search")
    public String getSearchProducts(@RequestParam("songName") String songName, Model model) {
        System.out.println(songName);
//        try {
//            songName = new String(songName.getBytes("ISO-8859-1"), "UTF-8");
//            System.out.println(songName);
//        } catch (java.io.UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        List<Product> products = productService.findByProductName(songName);
        model.addAttribute("products", products);
        for (Product product : products) {
            System.out.println(product.getName() + "" + product.getImageUrl() + "===================");
        }
//        return model;
        return "ems/search";
    }

    @RequestMapping(value = "dafen")
    public String rateToProduct(int id, Double pingfen, Model model, HttpSession session) {
        RUser user = (RUser) session.getAttribute("user");
        String username = user.getUsername();
        RUser userD = userService2.findByUsername(username);
        ProductRatingRequest request = new ProductRatingRequest(userD.getUserId(), id, pingfen);
        System.out.println(userD.getUserId() + "====" + id + "====" + request.getScore());

        Rating rating = new Rating(userD.getUserId(), id, pingfen, System.currentTimeMillis() / 1000);

        MongoCollection<Document> rCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_RATING_COLLECTION);
        System.out.println("<<<><><><><><><><>" + rCollection);
        try {
            rCollection.insertOne(Document.parse(objectMapper.writeValueAsString(rating)));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        boolean complete = ratingService.productRating(request);
        //埋点日志
        if (complete) {
            System.out.print("=========埋点=========");

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\11658\\usr\\local\\soft\\log\\agent.log", true));
                bw.write(Constant.PRODUCT_RATING_PREFIX + ":" + userD.getUserId() + "|" + id + "|" + request.getScore() + "|" + System.currentTimeMillis() / 1000 + "\r\n");
                bw.flush();
                bw.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("已经完成评分！");
        return "ems/daFen";
    }

}
