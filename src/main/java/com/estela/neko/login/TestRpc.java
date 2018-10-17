package com.estela.neko.login;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.core.TradeModelFactory;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.response.Accounts;
import com.estela.neko.huobi.response.AccountsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fuming.lj 2018/10/16
 **/
@RequestMapping("/testRpc")
@RestController
public class TestRpc {
    @Autowired
    ApiClient apiClient;

    @Autowired
    AccountModel accountModel;

    @Autowired
    TradeModelFactory factory;
    @Autowired
    HttpHelper httpHelper;


    @RequestMapping("/account")
    public Object queryAccount(String symbol){
        //GET /v1/account/accounts/{account-id}/balance
        TradeDimension dimension = factory.getDimension(symbol) ;
        accountModel.setKey(dimension.getApiKey(), dimension.getSecurityKey());
        AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
        int account = accounts.getData().get(0).getId();
        Map result = new HashMap<>();
        result.put("account",account);
        return JSONObject.toJSON(result);
    }
    @RequestMapping("/price")
    public Object queryPrice(String symbol){
        TradeDimension dimension = factory.getDimension(symbol) ;
        BigDecimal price = httpHelper.getPrice(dimension.getApiKey(),symbol);
        Map result = new HashMap();
        result.put("price",price);
        return JSONObject.toJSON(result);
    }
    @RequestMapping("/hblog")
    public Object setLogSwitch(String symbol){
        TradeDimension dimension = factory.getDimension(symbol) ;
        dimension.getDiamond().HUOBILog =  !dimension.getDiamond().HUOBILog;
        Map result = new HashMap();
        result.put("当前HUOBIlog:", dimension.getDiamond().HUOBILog );
        return JSONObject.toJSON(result);
    }
    @RequestMapping("/ghost")
    public Object setGhostFlag(String symbol){
        TradeDimension dimension = factory.getDimension(symbol) ;
        boolean flag =dimension.isGhostFlag();
        dimension.setGhostFlag(!flag);
        Map result = new HashMap();
        result.put("先前ghostFlag 为:", flag );
        return JSONObject.toJSON(result);
    }

}
