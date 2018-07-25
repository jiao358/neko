package com.estela.neko.common;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

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
@Service
public class HttpHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);
    @Autowired
    HttpConnectionManager manager;

    private final static BigDecimal rule = new BigDecimal(10000);

    public BigDecimal getPrice(String apiKey) {
        BigDecimal proPrice = null;
        try {


           String sendGet = sendGet("https://api.huobipro.com/market/trade",
                "symbol=htusdt&AccessKeyId=" + apiKey);
          /*  String sendGet = get("https://api.huobipro.com/market/trade",
                "symbol=htusdt&AccessKeyId=" + apiKey);*/
            logger.info("获取的价格数据结构:"+sendGet);
            JSONObject parseObject = JSON.parseObject(sendGet);
            proPrice = parseObject.getJSONObject("tick").getJSONArray("data").getJSONObject(0)
                .getBigDecimal("price");

            proPrice = proPrice.multiply(rule);

        } catch (Exception e) {
            logger.error("查询最新价格失败", e);
        }

        return proPrice;
    }

    public String get(String urlNameString) throws Exception {

        String result = null;
        try {

            HttpClient httpClient = manager.getHttpClient();
            HttpGet httpGet = new HttpGet(urlNameString);
            httpGet.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpGet.addHeader("user-agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 "
                    + "Safari/537.36");
            httpGet.addHeader("Accept-Language", "zh-cn");

            HttpResponse response = httpClient.execute(httpGet);
            logger.info("http get response:"+response);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                }
            }
        } catch (Exception ex) {
            logger.error("调用价格查询接口失败:", ex);
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
