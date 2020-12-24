package com.wyh.ems.service;


import com.wyh.ems.entity.*;

import java.util.List;

public interface EmpService {
    //查询所有的方法
    List<Song> findHotMusic();

    List<Song> selectMusic(String songName);

    List<Song1> findBymyName(String myName);

    //保存员工的方法
    void save(Emp emp);

    //删除员工的方法
    void delete(String song_name,String username);

    //根据id查询员工信息
    Song findById(String song_id);

    //更新员工信息
    void ShouCangM(String realname,String song_name,String song_length,String genre_ids,String artist_name,String musician,String zuoCijJia);
}
