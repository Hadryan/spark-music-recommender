package com.wyh.ems.dao;

import com.wyh.ems.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserDAO {

    //保存用户方法
    void save(User user);

    //登陆方法
    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    void createTuijianTable(@Param("userid") String id);

}
