package com.wyh.ems.service;

import com.wyh.ems.dao.EmpDAO;
import com.wyh.ems.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmpServiceImpl implements EmpService {
    @Autowired
    private EmpDAO empDAO;

    @Override
    public List<Song> findHotMusic() {
        return empDAO.findHotMusic();
    }

    @Override
    public List<Song> selectMusic(String songName) {
        return empDAO.selectMusic(songName);
    }

    @Override
    public List<Song1> findBymyName(String myName) {
        return empDAO.findBymyName(myName);
    }

    @Override
    public void save(Emp emp) {
        emp.setId(UUID.randomUUID().toString());
        empDAO.save(emp);
    }

    @Override
    public void delete(String song_name,String username) {
        empDAO.delete(song_name,username);
    }

    @Override
    public Song findById(String song_id) {
        return empDAO.findById(song_id);

    }

    @Override
    public void ShouCangM(String realname,String song_name,String song_length,String genre_ids,String artist_name,String musician,String zuoCijJia) {
        empDAO.ShouCangM(realname,song_name,song_length,genre_ids,artist_name,musician,zuoCijJia);
    }
}
