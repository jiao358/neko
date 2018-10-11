package com.estela.neko.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.TradeModelFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fuming.lj 2018/7/23
 **/
@Service
public class HttpHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("HUOBI");
    @Autowired
    HttpConnectionManager manager;
    @Autowired
    TradeModelFactory factory;
    private ThreadLocal<AtomicInteger> countLocal = new ThreadLocal<>();
    private final static BigDecimal rule = new BigDecimal(10000);

    public BigDecimal getPrice(String apiKey,String symbol) {
        BigDecimal proPrice = null;
        try {
            int count =0;
            if(countLocal.get()!=null){
                count=countLocal.get().incrementAndGet();
            }else{
                AtomicInteger cc= new AtomicInteger();
                countLocal.set(cc);
            }



          /** String sendGet = sendGet("https://api.huobipro.com/market/trade",
                "symbol=htusdt&AccessKeyId=" + apiKey);**/
         String sendGet = get("https://api.huobipro.com/market/trade?symbol="+symbol+"&AccessKeyId="+apiKey,symbol);
         if(count>100 || factory.getDimension(symbol).getDiamond().HUOBILog){
             debugLogger.info(symbol+" 价格信息返回:"+sendGet);
             countLocal.get().set(0);
         }


            JSONObject parseObject = JSON.parseObject(sendGet);
            proPrice = parseObject.getJSONObject("tick").getJSONArray("data").getJSONObject(0)
                .getBigDecimal("price");

            //5位数

            proPrice = proPrice.multiply(rule);

        } catch (Exception e) {
            logger.error(symbol+" 货币查询最新价格失败", e);
        }

        return proPrice;
    }

    private BigDecimal multiplyPrice(BigDecimal price){
        BigDecimal temp= price.multiply(rule);
        int length=temp.toString().length();
        if(length>5){
            temp = new BigDecimal(temp.toString().substring(0,5));
        }
        return temp;
    }


    public String get(String urlNameString,String symbol) throws Exception {

        String result = null;
        try {

            CloseableHttpClient httpClient = manager.getHttpClient();

            HttpGet httpGet = new HttpGet(urlNameString);
            httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");

            httpGet.addHeader("user-agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 "
                    + "Safari/537.36");
            httpGet.addHeader("Accept-Language", "zh-cn");

            HttpResponse response = httpClient.execute(httpGet);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                }
            }
        } catch (Exception ex) {
            logger.error(symbol+" 调用价格查询接口失败:", ex);
        }
        return result;

    }

    private String sendGet(String url, String param) throws Exception {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // if ("https".equalsIgnoreCase(realUrl.getProtocol())) {
            // ignoreSsl();
            // }
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();

            // 设置通用的请求属性
            connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("user-agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 "
                    + "Safari/537.36");
            connection.setRequestProperty("Accept-Language", "zh-cn");
            // 建立实际的连接
            connection.connect();
            connection.setConnectTimeout(500);
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
        return result;
    }

}
