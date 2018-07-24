package com.estela.neko.common;

import com.estela.neko.huobi.api.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author fuming.lj 2018/7/23
 **/
@Service
public class AccountModel {
    public  String apiKey ;

    public  String apiSecret;

    @Autowired
    ApiClient apiClient;

    public boolean hasAccountKey(){
        return (!StringUtils.isEmpty(apiKey)) && (!StringUtils.isEmpty(apiSecret));
    }


    public  String getApiKey() {
        return apiKey;
    }



    public  String getApiSecret() {
        return apiSecret;
    }


    public void setKey(String api,String sec){
        apiClient.setAccessKeyId(api);
        apiClient.setAccessKeySecret(sec);
        apiKey=api;
        apiSecret=sec;

    }

}
