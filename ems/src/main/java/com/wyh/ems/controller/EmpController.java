package com.wyh.ems.controller;


import com.wyh.ems.entity.*;
import com.wyh.ems.service.EmpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.SessionScope;
import scala.util.parsing.combinator.testing.Str;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("emp")
public class EmpController {

    /**
     * 即将改成查询所有热门音乐
     */
    @Autowired
    private EmpService empService;

    //查询热门
    @GetMapping("findHotMusic")
    public String findAll(ModelMap model){
        List<Song> musics = empService.findHotMusic();
        model.addAttribute("musics",musics);
        return "ems/topNList";
    }

    @RequestMapping("selectSong")
    public String selectMusic(String songName,ModelMap model){
        List<Song> selectSongs = empService.selectMusic(songName);
        model.addAttribute("selectSongs",selectSongs);
        return "ems/selectsongs";
    }

//    //查询所有
//    @GetMapping("findAll")
//    public String findAll(ModelMap model){
//        List<Emp> emps = empService.findAll();
//        model.addAttribute("emps",emps);
//        return "ems/emplist";
//    }

    //添加员工
    @PostMapping("save")
    public String save(Emp emp){
        empService.save(emp);
        return "redirect:/emp/findAll";
    }


    /**
     * 根据音乐name进行删除
     * @param song_name
     * @return
     */
//    //删除一个员工 根据id进行删除
//    @GetMapping("delete")
//    public String delete(String song_name, HttpSession session){
//
//        User user = (User)session.getAttribute("user");
//        String username = user.getRealname();
//        System.out.println(song_name+"=="+username);
//        empService.delete(song_name,username);
//        return "ems/ShouCang";
//    }


//    //删除一个员工 根据id进行删除
//    @GetMapping("delete")
//    public String delete(String id){
//        empService.delete(id);
//        return "redirect:/emp/findAll";
//    }


//    //根据id查询员工信息
//    @GetMapping("findOne")
//    public String findOne(String id, Model model){
//        Emp emp = empService.findById(id);
//        System.out.println(emp.getName());
//        model.addAttribute("emp",emp);
//        return "/ems/updateEmp";
//    }


    /**
     * 改成根据用户name查询收藏音乐
     * @param song_id
     * @param model
     * @returnCREATE TABLE songsD AS SELECT DISTINCT(song_name),song_length, genre_ids, artist_name, musician, zuoCijJia,language FROM topN
     */
    //根据id查询歌曲
    @GetMapping("findOne")
    public String findOne(String song_id, Model model, HttpSession session){
        Song song = empService.findById(song_id);
        System.out.println(song);
        model.addAttribute("song",song);
        session.setAttribute("song",song);
        return "/ems/updateEmp";
    }

//    //根据id查询歌曲
//    @GetMapping("findsongOne")
//    public String findsongOne(String song2, Model model, HttpSession session){
//        Song song3 = empService.findById(song2);
//        System.out.println(song3.getSong_name());
//        model.addAttribute("song3",song3);
//        session.setAttribute("song3",song3);
//        return "/ems/serchSong";
//    }

    //确认收藏
    @PostMapping("ShouCang")
    public String update(String realname,HttpSession session){
//        User user1 = (User)session.getAttribute("user");
        Song song1 = (Song) session.getAttribute("song");
        System.out.println(song1);
        String song_name = song1.getSong_name();
        String song_length = song1.getSong_length();
        String genre_ids = song1.getGenre_ids();
        String artist_name = song1.getArtist_name();
        String musician = song1.getMusician();
        String zuoCijJia = song1.getZuoCijJia();
        ShouCang shouCang = new ShouCang(realname,song_name,song_length,genre_ids,artist_name,musician,zuoCijJia);
        System.out.println(shouCang);
//        System.out.println(user);
        empService.ShouCangM(realname,song_name,song_length,genre_ids,artist_name,musician,zuoCijJia);

        return "redirect:/emp/findHotMusic";
    }


    /**
     *
     * 通过用户名查询收藏列表
     * @param myName
     * @return
     */
    @GetMapping("MyShouCang")
    public String MyShouCang(String myName,Model model){
        List<Song1> mySongs = empService.findBymyName(myName);
        model.addAttribute("mySongs",mySongs);
        return "ems/ShouCang";
    }

//    //更新员工的信息
//    @PostMapping("update")
//    public String update(Emp emp){
//        empService.update(emp);
//        return "redirect:/emp/findAll";
//    }
}
