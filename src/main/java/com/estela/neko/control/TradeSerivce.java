package com.estela.neko.control;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.core.TradeModelFactory;
import com.estela.neko.domain.Result;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.response.OrdersDetailResponse;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fuming.lj 2018/7/23
 **/
@RequestMapping("/tradeService")
@RestController
public class TradeSerivce {
    private static final Logger logger = LoggerFactory.getLogger(TradeSerivce.class);

    @Autowired
    AccountModel accountModel;
    @Autowired
    TradeModelFactory factory;

    @Autowired
    HttpHelper helper;
    @Autowired
    ApiClient apiClient;

    @Autowired
    TradeModelFactory tradeModelFactory;



    @RequestMapping("/startSystem")
    public synchronized Object startSystem(String symbol) throws Exception {
        Result result = new Result();
        TradeDimension dimension = tradeModelFactory.getDimension(symbol);
        if(!dimension.getDiamond().sysFirstRun.get()){

            dimension.getDiamond().canRunning=true;
            dimension.getPriceStrategy().execute();
            dimension.getDiamond().sysFirstRun.set(true);
        }else{
            dimension.getDiamond().canRunning=true;
        }
        return result;

    }
    @RequestMapping("/stopSystem")
    public Object stopSystem(String symbol) throws Exception {
        Result result = new Result();
        TradeDimension dimension = tradeModelFactory.getDimension(symbol);
        dimension.getDiamond().canRunning=false;
        return result;

    }



    @RequestMapping("/order")
    public Object getOrder(Long orderId) {

        OrdersDetailResponse ordersDetail = apiClient
            .ordersDetail(String.valueOf(orderId));
        return JSONObject.toJSON(ordersDetail);

    }



    @RequestMapping("/setAmount")
    public Object setAmount(String amount) {
        return "";
    }





    /**
     * 暂停执行 当前价格服务
     * @return
     */
    @RequestMapping("/stopPriceService")
    public StrategyStatus stopPriceService(String symbol) {
        TradeDimension dimension = tradeModelFactory.getDimension(symbol);
        dimension.getDiamond().canRunning=false;

        return dimension.getStrategyStatus();
    }


    /**
     * 设置交易策略
     * @return
     */
    @RequestMapping("/setStrategy")
    public Object setStrategy(String symbol,String maxOrderSize,String diffPrice,String lowRisiPrice,String highriskPrice,String lotSize,String emptyOrder,String reflashOrder) {
        TradeDimension dimension = tradeModelFactory.getDimension(symbol);
        Result result = new Result();
        if(!dimension.getDiamond().canRunning){
            if(!StringUtils.isEmpty(maxOrderSize)){
                dimension.getStrategyStatus().setMaxOrderSize(Integer.parseInt(maxOrderSize));
            }

            if(!StringUtils.isEmpty(diffPrice)){
                dimension.getStrategyStatus().setDiffPrice(Integer.parseInt(diffPrice));
            }
            if(!StringUtils.isEmpty(lowRisiPrice)){
                dimension.getStrategyStatus().setLowRisiPrice(Integer.parseInt(lowRisiPrice));
            }
            if(!StringUtils.isEmpty(highriskPrice)){
                dimension.getStrategyStatus().setHighriskPrice(Integer.parseInt(highriskPrice));
            }
            if(!StringUtils.isEmpty(lotSize)){
                dimension.getStrategyStatus().setLotSize(Double.parseDouble(lotSize));
            }

            //增加清除空订单
            if(!StringUtils.isEmpty(emptyOrder)){
                Gson gson = new Gson();
                Map<String, Double> map = new HashMap<String, Double>();
                map = gson.fromJson(emptyOrder, map.getClass());

                for(Map.Entry<String,Double> entry:map.entrySet()){
                    String order = entry.getKey();
                    Double price =entry.getValue();

                    dimension.getPriceStrategy().deleteSellOrder(order,price.intValue()+"");
                }

            }
            //增加可以全量刷新订单的能力
            if(!StringUtils.isEmpty(reflashOrder)){
                Gson gson = new Gson();
                Map<String, Double> map = new HashMap<String, Double>();
                map = gson.fromJson(emptyOrder, map.getClass());

                for(Map.Entry<String,Double> entry:map.entrySet()){
                    String order = entry.getKey();
                    Double price =entry.getValue();

                    dimension.getPriceStrategy().reflashOrder(order,price.intValue()+"");
                }


            }






        }


        return result;
    }



    /**
     * 恢复执行 当前价格服务
     * @return
     */
    @RequestMapping("/startPriceService")
    public StrategyStatus startPriceService(String symbol) {
        TradeDimension dimension = tradeModelFactory.getDimension(symbol);
        dimension.getDiamond().canRunning=true;

        return dimension.getStrategyStatus();
    }



    @RequestMapping("/api")
    public String helloWorld() {
        return " hello world ";
    }
}
