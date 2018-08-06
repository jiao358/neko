package com.estela.neko.control;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.domain.Result;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.utils.CommonUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fuming.lj 2018/7/31
 **/
@RequestMapping("/tradeInfo")
@RestController
public class TradeInfoService {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    PriceStrategy priceStrategy;
    @RequestMapping("/get")
    public Object getTradeInfo(){
        SystemModel systemModel = commonUtil.generateCurrentModel();

        return JSONObject.toJSON(systemModel);

    }

    @RequestMapping("/setOrderInfo")
    public Object setOrderInfo(String initBuyOrder,String initSellOrder){

        Result result = new Result();

        if(Diamond.canRunning || priceStrategy.sellOrder.size()>0){
            return result;
        }


        if(!StringUtils.isEmpty(initBuyOrder)){
            Gson gson = new Gson();
            Map<String, String> map = new HashMap<String, String>();
            map = gson.fromJson(initBuyOrder, map.getClass());
            map.forEach((key,value)->{
                priceStrategy.addBuyOrder(key,value);
            });
        }


        if(!StringUtils.isEmpty(initSellOrder)){
            Gson gson = new Gson();
            Map<String, Double> map = new HashMap<String, Double>();
            map = gson.fromJson(initSellOrder, map.getClass());

            for(Map.Entry<String,Double> entry:map.entrySet()){
                String order = entry.getKey();
                Double price =entry.getValue();

                priceStrategy.addSellOrder(order,price.intValue()+"");
            }

        }

        return result;
    }
}
