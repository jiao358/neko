package com.estela.neko.core;

import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.huobi.api.ApiClient;
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

        Object ob=helper.get("https://api.huobipro.com/market/trade?symbol=htusdt&AccessKeyId="+"a7fd725a-502746cd-69b903fd-4418a");
        return ob;


    }
    URLConnection connection ;
    @RequestMapping("/price2")
    public Object getPrice2() throws Exception {
        String result = "";
        BufferedReader in = null;
        long beginTime = System.currentTimeMillis();
        try {
            String urlNameString ="https://api.huobipro.com/market/trade?symbol=htusdt&AccessKeyId=a7fd725a-502746cd-69b903fd-4418a";
            URL realUrl = new URL(urlNameString);
            // if ("https".equalsIgnoreCase(realUrl.getProtocol())) {
            // ignoreSsl();
            // }
            // 打开和URL之间的连接

            if(connection==null){
               connection = realUrl.openConnection();
                // 设置通用的请求属性
                connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("user-agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 "
                        + "Safari/537.36");
                connection.setRequestProperty("Accept-Language", "zh-cn");
                // 建立实际的连接
                connection.setConnectTimeout(500);
                connection.connect();
            }


            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();

            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }


        return result+ "\\n"+ (System.currentTimeMillis()-beginTime);

    }


    @RequestMapping("/setProperties")
    public StrategyStatus setProperties(String riskPrice,String maxOrderSize ,String lotSize ,String diffPrice){
        try{
            if(!StringUtils.isEmpty(riskPrice)){
                //乘以10000 以上
                tradeStatus.setRiskPrice(new BigDecimal(riskPrice));
            }
            if(!StringUtils.isEmpty(maxOrderSize)){

                tradeStatus.setMaxOrderSize(Integer.valueOf(maxOrderSize));
            }
            if(!StringUtils.isEmpty(lotSize)){

                tradeStatus.setLotSize(Double.valueOf(lotSize));
            }

            if(!StringUtils.isEmpty(diffPrice)){

                tradeStatus.setDiffPrice(Integer.valueOf(diffPrice));
            }



        }catch (Exception e){
            logger.error("设置参数错误",e);
            tradeStatus.setSysMsg("设置参数非法");
        }




        return tradeStatus;
    }

    @RequestMapping("/look")
    public StrategyStatus queryStatus(){
        return tradeStatus;
    }


    @RequestMapping("/go")
    public StrategyStatus go(String startOrder){
        if(!StringUtils.isEmpty(startOrder)){
            tradeStatus.setStartOrder(new BigDecimal(startOrder));
        }
        accountModel.setKey("a7fd725a-502746cd-69b903fd-4418a","5774a589-a4b36db6-382fdc6f-6bbae");

        tradeStatus.setTrading(true);
        priceStrategy.startReflashPrice();


        return tradeStatus;
    }

    @RequestMapping("/start")
    public StrategyStatus start(@RequestParam(required = true) String accessKey, @RequestParam(required = true)String securityKey, BigDecimal startOrderPrice){
        if(StringUtils.isEmpty(accessKey)|| StringUtils.isEmpty(securityKey)){
            tradeStatus.setSysMsg("输入参数异常,请检查重试");
            logger.error("输入参数有误:"+accessKey+":"+accessKey+",securityKey:"+securityKey);
            return tradeStatus;
        }
        if(tradeStatus.getTrading()){
            logger.info("系统已经启动");
            return tradeStatus;
        }
        tradeStatus.setStartOrder(startOrderPrice);
        accountModel.setKey(accessKey,securityKey);
        tradeStatus.setTrading(true);
        priceStrategy.startReflashPrice();




        return tradeStatus;
    }


    @RequestMapping("/api")
    public String helloWorld(){
        return " hello world ";
    }
}
