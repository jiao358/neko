package com.estela.neko.utils;

import com.estela.neko.common.AccountModel;
import com.estela.neko.huobi.api.ApiNewClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author fuming.lj 2018/9/10
 **/
@Service
public class LoadApiKey implements InitializingBean{
    final String apiKeyFile ="/root/apikey.properties";
    @Autowired
    ApiNewClient apiNewClient;
    @Autowired
    AccountModel accountModel;

    @Override
    public void afterPropertiesSet() throws Exception {

        FileInputStream fis = new FileInputStream(apiKeyFile);
        Properties properties = new Properties();
        properties.load(fis);
        String key =properties.getProperty("api");
        String security = properties.getProperty("security");
        apiNewClient.accessKeyId= key;
        apiNewClient.accessKeySecret = security;
        accountModel.setKey(key,security);
    }
}
