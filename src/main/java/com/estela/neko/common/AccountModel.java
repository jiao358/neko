package com.estela.neko.common;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author fuming.lj 2018/7/23
 **/
@Service
public class AccountModel {
    public  String apiKey ;

    public  String apiSecret;


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
        apiKey=api;
        apiSecret=sec;

    }

}
