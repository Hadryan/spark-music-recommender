package com.wyh.ems.controller;

import com.wyh.ems.entity.RUser;
import com.wyh.ems.entity.request.LoginUserRequest;
import com.wyh.ems.entity.request.RegisterUserRequest;
import com.wyh.ems.service.UserService;
import com.wyh.ems.service.UserService2;
import com.wyh.ems.utils.ValidateImageCodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Controller
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserService2 userService2;

    //开发用户登录
    @RequestMapping("login")
    public String login(String username, String password, HttpSession session, Model model) {
        RUser user = userService2.loginUser(new LoginUserRequest(username, password));
//        User user = userService.login(username, password);
        //判断  user 为空  用户户名密码错误      //不为空  登录成功
        if (user != null) {
            session.setAttribute("user", user);
            System.out.println(">>>>>" + user.getUsername() + ">>>>>" + user.getPassword());
            return "redirect:/product/findHotMusic";//查询员工的所有
        } else {
            System.out.println("查询失败");
            return "redirect:/ems/login.jsp";
        }

    }


    //开发用户注册
    @PostMapping("regist")
    public String regist(@RequestParam("username") String username, @RequestParam("password") String password, String code, HttpSession session) {
        System.out.println("username = " + username + "password = " + password);
        //1、判断验证码是否通过
        if (session.getAttribute("code").toString().equalsIgnoreCase(code)) {//2、通过之后注册
            if (userService2.checkUserExist(username)) {
                return " 用户名已经被注册！";
            }
            boolean b = userService2.registerUser(new RegisterUserRequest(username, password));

            return "redirect:/ems/login.jsp";
        } else {//3、不通过直接回到注册界面
            return "redirect:/ems/regist.jsp";
        }
    }

    //生成验证码
    @GetMapping("getImage")
    public void getImage(HttpSession session, HttpServletResponse response) throws IOException {
        //生成验证码
        String securityCode = ValidateImageCodeUtils.getSecurityCode();
        //将验证码放入session
        session.setAttribute("code", securityCode);
        //生成图片
        BufferedImage image = ValidateImageCodeUtils.createImage(securityCode);
        //输出图片  通过相应的方式输出
        ServletOutputStream os = response.getOutputStream();
        //调用工具类
        ImageIO.write(image, "png", os);
    }
}
