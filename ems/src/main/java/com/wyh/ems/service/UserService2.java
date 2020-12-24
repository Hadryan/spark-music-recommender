package com.wyh.ems.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wyh.ems.entity.RUser;
import com.wyh.ems.entity.request.LoginUserRequest;
import com.wyh.ems.entity.request.RegisterUserRequest;
import com.wyh.ems.utils.Constant;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserService2 {


    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private ObjectMapper objectMapper;

    private MongoCollection<Document> userCollection;

    private MongoCollection<Document> getUserCollection(){
        if(null == userCollection)
            userCollection = mongoClient.getDatabase(Constant.MONGODB_DATABASE).getCollection(Constant.MONGODB_USER_COLLECTION);
        return userCollection;
    }


    public RUser loginUser(LoginUserRequest request){
        RUser user = findByUsername(request.getUsername());
        System.out.println(userCollection);
        if(null == user) {
            return null;
        }else if(!user.passwordMatch(request.getPassword())){
            return null;
        }
        return user;
    }


    public RUser findByUsername(String username){
        Document user = getUserCollection().find(new Document("username",username)).first();
        if(null == user || user.isEmpty())
            return null;
        return documentToUser(user);
    }

    private RUser documentToUser(Document document){
        try{
            return objectMapper.readValue(JSON.serialize(document),RUser.class);
        } catch (JsonParseException e) {
            e.printStackTrace();
            return null;
        } catch (JsonMappingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean checkUserExist(String username){
        return null != findByUsername(username);
    }

    public boolean registerUser(RegisterUserRequest request){
        RUser user = new RUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFirst(true);
        user.setTimestamp(System.currentTimeMillis());
        try{
            getUserCollection().insertOne(Document.parse(objectMapper.writeValueAsString(user)));
            return true;
        }catch (JsonProcessingException e){
            e.printStackTrace();
            return false;
        }
    }
}
