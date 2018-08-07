package com.estela.neko.control;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.domain.Result;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.response.OrdersDetailResponse;
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
    StrategyStatus tradeStatus;
    @Autowired
    AccountModel accountModel;
    @Autowired
    PriceStrategy priceStrategy;

    @Autowired
    HttpHelper helper;
    @Autowired
    ApiClient apiClient;

    @RequestMapping("/price")
    public Object getPrice() throws Exception {

        Object ob = helper.get(
            "https://api.huobipro.com/market/trade?symbol=htusdt&AccessKeyId=" + "a7fd725a-502746cd-69b903fd-4418a");
        return ob;

    }

    @RequestMapping("/startSystem")
    public synchronized Object startSystem() throws Exception {
        Result result = new Result();
        if(!Diamond.sysFirstRun.get()){
            accountModel.setKey("205911d9-34c137a1-33ed938c-d9c12", "c35095b1-d25a11d6-a949a451-205a4");
            Diamond.canRunning=true;
            priceStrategy.execute();
            Diamond.sysFirstRun.set(true);
        }else{
            Diamond.canRunning=true;
        }
        return result;

    }
    @RequestMapping("/stopSystem")
    public Object stopSystem() throws Exception {
        Result result = new Result();
        Diamond.canRunning=false;
        return result;

    }



    @RequestMapping("/order")
    public Object getOrder(Long orderId) {

        OrdersDetailResponse ordersDetail = apiClient
            .ordersDetail(String.valueOf(orderId));
        return JSONObject.toJSON(ordersDetail);

    }

    @RequestMapping("/priceInfo")
    public Object getPriceInfo() {
        Map map = new HashMap();

        map.put("price_order:", JSONObject.toJSON(priceStrategy.price_order));
        map.put("sell_order:", JSONObject.toJSON(priceStrategy.sell_order));
        return JSONObject.toJSON(map);

    }

    @RequestMapping("/setAmount")
    public Object setAmount(String amount) {
        return "";
    }

    @RequestMapping("/setProperties")
    public StrategyStatus setProperties(String highriskPrice, String maxOrderSize, String lotSize, String diffPrice,
                                        String lowPrice) {
        try {
            if (!StringUtils.isEmpty(highriskPrice)) {
                //乘以10000 以上
                tradeStatus.setHighriskPrice(Integer.parseInt(highriskPrice));
            }

            if (!StringUtils.isEmpty(lowPrice)) {
                //乘以10000 以上
                tradeStatus.setHighriskPrice(Integer.parseInt(lowPrice));
            }

            if (!StringUtils.isEmpty(maxOrderSize)) {

                tradeStatus.setMaxOrderSize(Integer.valueOf(maxOrderSize));
            }
            if (!StringUtils.isEmpty(lotSize)) {

                tradeStatus.setLotSize(Double.valueOf(lotSize));
            }

            if (!StringUtils.isEmpty(diffPrice)) {

                tradeStatus.setDiffPrice(Integer.valueOf(diffPrice));
            }

        } catch (Exception e) {
            logger.error("设置参数错误", e);
            tradeStatus.setSysMsg("设置参数非法");
        }

        return tradeStatus;
    }

    @RequestMapping("/look")
    public StrategyStatus queryStatus() {
        return tradeStatus;
    }

    @RequestMapping("/go")
    public StrategyStatus go(String startOrder) {
        if (!StringUtils.isEmpty(startOrder)) {
            tradeStatus.setStartOrder(new BigDecimal(startOrder));
        }
        accountModel.setKey("a7fd725a-502746cd-69b903fd-4418a", "5774a589-a4b36db6-382fdc6f-6bbae");
        tradeStatus.setTrading(true);
        priceStrategy.execute();

        return tradeStatus;
    }

    /**
     * 暂停执行 当前价格服务
     * @return
     */
    @RequestMapping("/stopPriceService")
    public StrategyStatus stopPriceService() {

        Diamond.canRunning=false;

        return tradeStatus;
    }


    /**
     * 设置交易策略
     * @return
     */
    @RequestMapping("/setStrategy")
    public Object setStrategy(String maxOrderSize,String diffPrice,String lowRisiPrice,String highriskPrice,String lotSize) {

        Result result = new Result();
        if(!Diamond.canRunning){
            if(!StringUtils.isEmpty(maxOrderSize)){
                tradeStatus.setMaxOrderSize(Integer.parseInt(maxOrderSize));
            }

            if(!StringUtils.isEmpty(diffPrice)){
                tradeStatus.setDiffPrice(Integer.parseInt(diffPrice));
            }
            if(!StringUtils.isEmpty(lowRisiPrice)){
                tradeStatus.setLowRisiPrice(Integer.parseInt(lowRisiPrice));
            }
            if(!StringUtils.isEmpty(highriskPrice)){
                tradeStatus.setHighriskPrice(Integer.parseInt(highriskPrice));
            }
            if(!StringUtils.isEmpty(lotSize)){
                tradeStatus.setLotSize(Double.parseDouble(lotSize));
            }

        }


        return result;
    }



    /**
     * 恢复执行 当前价格服务
     * @return
     */
    @RequestMapping("/startPriceService")
    public StrategyStatus startPriceService() {

        Diamond.canRunning=true;

        return tradeStatus;
    }

    @RequestMapping("/start")
    public StrategyStatus start(@RequestParam(required = true) String accessKey,
                                @RequestParam(required = true) String securityKey, BigDecimal startOrderPrice) {
        if (StringUtils.isEmpty(accessKey) || StringUtils.isEmpty(securityKey)) {
            tradeStatus.setSysMsg("输入参数异常,请检查重试");
            logger.error("输入参数有误:" + accessKey + ":" + accessKey + ",securityKey:" + securityKey);
            return tradeStatus;
        }
        if (tradeStatus.getTrading()) {
            logger.info("系统已经启动");
            return tradeStatus;
        }
        tradeStatus.setStartOrder(startOrderPrice);
        accountModel.setKey(accessKey, securityKey);
        tradeStatus.setTrading(true);
        //priceStrategy.startReflashPrice();

        return tradeStatus;
    }

    @RequestMapping("/api")
    public String helloWorld() {
        return " hello world ";
    }
}
