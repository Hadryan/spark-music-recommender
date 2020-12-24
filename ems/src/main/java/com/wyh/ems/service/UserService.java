package com.wyh.ems.service;

import com.wyh.ems.entity.User;

public interface UserService {
    //保存用户
    void save(User user);

    //登陆
    User login(String username,String password);

    //根据用户id注册推荐表
    void createTuijianTable(String id);
}
