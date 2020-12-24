package com.wyh.ems.dao;

import com.wyh.ems.entity.Emp;
import com.wyh.ems.entity.ShouCang;
import com.wyh.ems.entity.Song;
import com.wyh.ems.entity.Song1;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EmpDAO {

    //查询所有的方法
    List<Song> findHotMusic();

    List<Song> selectMusic(@Param("songName")String songName);

    List<Song1> findBymyName(@Param("myName") String myName);

    //保存员工的方法
    void save(Emp emp);

    //删除员工
    void delete(@Param("song_name")String id,@Param("username")String username);

    //根据id查询员工信息
    Song findById(@Param("songId") String song_id);

    //根据用户的真实姓名进行收藏
    void ShouCangM(@Param("realname") String realname, @Param("song_name") String song_name, @Param("song_length") String song_length,
                   @Param("genre_ids") String genre_ids, @Param("artist_name") String artist_name,
                   @Param("musician") String musician, @Param("zuoCijJia") String zuoCijJia);
}
