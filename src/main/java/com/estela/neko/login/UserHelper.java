package com.estela.neko.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * @author fuming.lj 2018/7/30
 **/
@Controller
@RequestMapping("/")
public class UserHelper {
    @Autowired
    SessionControl sessionControl;


    @GetMapping("/")
    public String index( String account, Model model) {
        model.addAttribute("name", account);
        return "login";
    }

    @PostMapping("/loginOn")
    public String loginOn(String username, String password, HttpSession session) {
        if("estelasu".equals(username)&& "qaz8893".equals(password)){
            sessionControl.addSession(session.getId());
            return "forward:/system";

        }else{
            return "noLogin";
        }
    }
    @PostMapping("/system")
    public String systemStatus(){

        return "SystemStatus";
    }


}
