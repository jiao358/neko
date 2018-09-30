package com.estela.neko.control;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.core.TradeModelFactory;
import com.estela.neko.domain.Result;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.utils.CommonUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * @author fuming.lj 2018/7/31
 **/
@RequestMapping("/tradeInfo")
@RestController
public class TradeInfoService {
    @Autowired
    CommonUtil commonUtil;

    @Autowired
    TradeModelFactory factory;


    @RequestMapping("/get")
    public Object getTradeInfo(HttpSession session){
        String symbol =session.getAttribute("userCurrency").toString();
        SystemModel systemModel = commonUtil.generateCurrentModel(symbol);
        return JSONObject.toJSON(systemModel);
    }
    @RequestMapping("/getCurrency")
    public Object getCurrency(){
        Set<String> currencySet= factory.getAllCurrency();
        List<String> list = new LinkedList(currencySet);
        Map keyMap =new HashMap();
        keyMap.put("currencys",list);
        return JSONObject.toJSON(keyMap);
    }





    @RequestMapping("/setOrderInfo")
    public Object setOrderInfo(String initBuyOrder,String initSellOrder,String symbol){
        TradeDimension dimension = factory.getDimension(symbol);
        Result result = new Result();

        if(dimension.getDiamond().canRunning || dimension.getPriceStrategy().sellOrder.size()>0){
            return result;
        }


        if(!StringUtils.isEmpty(initBuyOrder)){
            Gson gson = new Gson();
            Map<String, Double> map = new HashMap<String, Double>();
            map = gson.fromJson(initBuyOrder, map.getClass());

            for(Map.Entry<String,Double> entry:map.entrySet()){
                String order = entry.getKey();
                Double price =entry.getValue();

                dimension.getPriceStrategy().addBuyOrder(order,price.intValue()+"");
            }



        }


        if(!StringUtils.isEmpty(initSellOrder)){
            Gson gson = new Gson();
            Map<String, Double> map = new HashMap<String, Double>();
            map = gson.fromJson(initSellOrder, map.getClass());

            for(Map.Entry<String,Double> entry:map.entrySet()){
                String order = entry.getKey();
                Double price =entry.getValue();

                dimension.getPriceStrategy().addSellOrder(order,price.intValue()+"");
            }

        }

        return result;
    }
}
