package com.estela.neko.huobi.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.HttpConnectionManager;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.request.CreateOrderRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fuming.lj 2018/8/7
 * 使用请求线程池管理
 **/
@Service
public class ApiNewClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiNewClient.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("HUOBI");
    @Autowired
    HttpConnectionManager manager;
    static final String API_HOST = "api.huobi.pro";

    static final String API_URL = "https://" + API_HOST;


    int huobiCount = 0;

    /**
     * 执行订单
     * @return
     */
    public String executeOrder(long orderId, TradeDimension dimension){
        String uri ="/v1/order/orders/" + orderId + "/place";

        ApiSignature sign = new ApiSignature();
        Map<String,String> access =new HashMap<>();
        sign.createSignature(dimension.getApiKey(), dimension.getSecurityKey(), "POST", API_HOST, uri, access);
        String execute="";
        try {
            CloseableHttpClient httpClient = manager.getHttpClient();

            HttpPost httpPost = new HttpPost(API_URL + uri+ "?" + toQueryString(access));
            setHeader(httpPost);
            StringEntity postBody =new StringEntity(JsonUtil.writeValue(null),"utf-8");
            postBody.setContentType("application/json; charset=utf-8");
            postBody.setContentEncoding("utf-8");
            httpPost.setEntity(postBody);
            HttpResponse response = httpClient.execute(httpPost);
            String result ="";
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                    debugLogger.info("执行订单请求返回:"+result);
                    JSONObject ds = JSONObject.parseObject(result);
                    execute = ds.getString("data");
                }
            }


        } catch (IOException e) {
            logger.error("创建订单异常:",e);

        }finally {
            return execute;
        }


    }



    /**
     * 用于创建订单
     * @return
     */
    public Long createOrder(CreateOrderRequest request ,TradeDimension dimension){
        String uri = "/v1/order/orders";
        Map<String,String> access =new HashMap<>();

        ApiSignature sign = new ApiSignature();
        sign.createSignature(dimension.getApiKey(), dimension.getSecurityKey(), "POST", API_HOST, uri, access);
        long order=0L;
        try {
            CloseableHttpClient httpClient = manager.getHttpClient();
            HttpPost httpPost = new HttpPost(API_URL + uri+ "?" + toQueryString(access));
            setHeader(httpPost);
            StringEntity postBody =new StringEntity(JsonUtil.writeValue(request),"utf-8");
            postBody.setContentType("application/json; charset=utf-8");
            postBody.setContentEncoding("utf-8");
            httpPost.setEntity(postBody);

            HttpResponse response = httpClient.execute(httpPost);

            String result ="";
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                    JSONObject ds = JSONObject.parseObject(result);
                    order = ds.getLong("data");
                }
            }

        } catch (IOException e) {
            logger.error("创建订单异常:",e);

        }finally {
            return order;
        }

    }


    /**
     * 用于获取订单信息, 状态以及手数
     * @param orderId
     * @return
     * @throws Exception
     */
    public Map<String,String> getOrderInfoMap(String orderId,TradeDimension dimension) throws Exception {
        ApiSignature sign = new ApiSignature();
        String uri="/v1/order/orders/" + orderId;
        Map<String,String> param = new HashMap<>();
        Map<String, String> map = new HashMap<>();
        try {
            sign.createSignature(dimension.getApiKey(), dimension.getSecurityKey(), "GET", API_HOST, uri, param);
            CloseableHttpClient httpClient = manager.getHttpClient();
            String result = "";
            HttpGet httpGet = new HttpGet(API_URL + uri + "?" + toQueryString(param));
            setHeader(httpGet);
            HttpResponse response = httpClient.execute(httpGet);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                    JSONObject ds = JSONObject.parseObject(result).getJSONObject("data");
                    map.put("state", ds.getString("state"));
                    map.put("field-amount", ds.getString("field-amount"));
                    map.put("field-cash-amount", ds.getString("field-cash-amount"));
                    map.put("field-fees", ds.getString("field-fees"));


                    map.put("data" ,ds.toJSONString());
                }
            }


            return map;
        }catch (com.alibaba.fastjson.JSONException e){
            logger.error("查询订单信息异常:","429 Too Many Requests");
            return map;
        }catch (Exception e){
            logger.error("查询订单信息异常:",e);
            return map;
        }


    }

    public String getAccounts(TradeDimension dimension){
        String uri="/v1/account/accounts";
        Map<String,String> param = new HashMap<>();
        JSONObject  ds = JSONObject.parseObject(get(uri,param,dimension));
        String success = ds.getString("status");
        if(success.equals("ok")){
            JSONArray array=  ds.getJSONArray("data");
            JSONObject objt=array.getJSONObject(0);
            String accountId =objt.getString("id");
            return accountId;

        }
        return "";
    }


    public List<JSONObject> getAccountAmount(int accounts,String currency,TradeDimension dimension) throws Exception {
        String uri="/v1/account/accounts/"+accounts+"/balance";
        Map<String,String> param = new HashMap<>();


        JSONObject  ds = JSONObject.parseObject(get(uri,param,dimension));
        List<JSONObject> result = new ArrayList();
        String success = ds.getString("status");
        if(success.equals("ok")){
            JSONArray array=  ds.getJSONObject("data").getJSONArray("list");
            List<JSONObject> linkedList = new ArrayList();
            for(int i=0;i<array.size();i++){
                linkedList.add(array.getJSONObject(i));

            }
    //usdt为基础货币   currency为法币
            linkedList.forEach(domain->{
                if(domain.getString("currency").equals("usdt") || currency.equals(domain.getString("currency"))){
                    result.add(domain);
                }
            });
        }
        return result;
    }


    String get(String uri,Map<String,String > param ,TradeDimension dimension){
        ApiSignature sign = new ApiSignature();
        sign.createSignature(dimension.getApiKey(), dimension.getSecurityKey(), "GET", API_HOST, uri, param);
        CloseableHttpClient httpClient = manager.getHttpClient();
        String result="";
        try{
            HttpGet httpGet = new HttpGet(API_URL+uri+"?"+toQueryString(param));
            setHeader(httpGet);
            HttpResponse response = httpClient.execute(httpGet);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    result = EntityUtils.toString(resEntity, "utf-8");
                }
            }
        }catch (Exception e){
            System.out.println("get 请求失败");
        }


        return result;

    }

    String toQueryString(Map<String, String> params) {
        return String.join("&", params.entrySet().stream().map((entry) -> {
            return entry.getKey() + "=" + ApiSignature.urlEncode(entry.getValue());
        }).collect(Collectors.toList()));
    }

    private void setHeader(HttpRequestBase base){
        if( base instanceof HttpGet){
            base.setHeader("Content-Type","application/x-www-form-urlencoded");
        }else{
            base.setHeader("Content-Type","application/json");
        }
        base.addHeader("user-agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 "
                + "Safari/537.36");
        base.addHeader("Accept-Language", "zh-cn");


    }



}
