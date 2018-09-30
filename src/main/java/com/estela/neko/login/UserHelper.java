package com.estela.neko.login;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.AccountModel;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.response.Accounts;
import com.estela.neko.huobi.response.AccountsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @author fuming.lj 2018/7/30
 **/
@Controller
@RequestMapping("/")
public class UserHelper {
    @Autowired
    SessionControl sessionControl;

    @Autowired
    ApiClient apiClient;

    @Autowired
    AccountModel accountModel;

    public static String symbol ="htusdt";

    @GetMapping("/")
    public String index( String account, Model model) {
        model.addAttribute("name", account);
        return "login";
    }

    @PostMapping("/loginOn")
    public String loginOn(String username, String password, HttpSession session) {
        if("estelasu".equals(username)&& "qaz8893".equals(password)){
            sessionControl.addSession(session.getId());
            return "forward:/select";

        }else{
            return "noLogin";
        }
    }

    @PostMapping("/select")
    public String selectCurrency(){

        return "SelectPage";
    }

    @PostMapping("/afterSelect")
    public String afterSelect(String userSelect ,HttpSession session){
        session.setAttribute("userCurrency",userSelect);
        String key=session.getId();
        if(sessionControl.isLogin(key)){
            return "forward:/system";
        }else{
            return "noLogin";
        }
    }



    @PostMapping("/system")
    public String systemStatus(){

        return "SystemStatus";
    }
    @RequestMapping("/account")
    public Object queryAccount(){
        //GET /v1/account/accounts/{account-id}/balance
        accountModel.setKey("a7fd725a-502746cd-69b903fd-4418a", "5774a589-a4b36db6-382fdc6f-6bbae");
        AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
        int account = accounts.getData().get(0).getId();
        return JSONObject.toJSON(apiClient.getBalance(account+""));
    }


}
