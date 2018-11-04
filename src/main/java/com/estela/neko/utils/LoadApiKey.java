package com.estela.neko.utils;

import com.estela.neko.Enutype.TradeModelType;
import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.core.TradeModelFactory;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.api.ApiNewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author fuming.lj 2018/9/10
 **/
@Service
public class LoadApiKey implements InitializingBean{
    private  static Logger logger = LoggerFactory.getLogger(LoadApiKey.class);
    final String apiKeyFile ="/root/apikey.properties";
    //final String dimensionDic = "/Users/estelasu/basaka";
    final String dimensionDic = "/root/basaka";
    @Autowired
    TradeModelFactory tradeModelFactory;
    @Autowired
    ApiNewClient apiNewClient;
    @Autowired
    AccountModel accountModel;

    @Autowired
    HttpHelper httpHelper;
    @Autowired
    CommonUtil commonUtil;

    private  void changeFlu(TradeDimension dimension){
        TradeModelType type =dimension.getTradeModelType();
        if(type.equals(TradeModelType.QUA_MODEL)){
            dimension.setFluFromDimension(150);
        }else if(type.equals(TradeModelType.QUA_MODEL100)){
            dimension.setFluFromDimension(100);
        }else if(type.equals(TradeModelType.QUA_MODEL200)){
            dimension.setFluFromDimension(200);
        }else if(type.equals(TradeModelType.QUA_MODEL300)){
            dimension.setFluFromDimension(300);
        }
    }
    @Override
    public void afterPropertiesSet() throws Exception {
    try{
        File rootDir = new File(dimensionDic);
        if(!rootDir.isDirectory()){
            return;
        }

        Arrays.stream(rootDir.list()).forEach(str->{
            try{
                String file = dimensionDic+"/"+str;
                FileInputStream fis = new FileInputStream(file);
                Properties properties = new Properties();
                properties.load(fis);
                String key =properties.getProperty("api");
                String security = properties.getProperty("security");
                String semaphore = properties.getProperty("semaphore");
                String model=properties.getProperty("model");
                String accountId = properties.getProperty("account");
                String currency = properties.getProperty("currency");

                TradeDimension dimension = new TradeDimension();
                dimension.setAccountId(accountId);
                dimension.setApiKey(key);
                dimension.setSecurityKey(security);
                dimension.setTradeSemaphore(semaphore);
                dimension.setCurrency(currency);
                dimension.setTradeModelType(TradeModelType.valueOf(model));
                changeFlu(dimension);


                PriceStrategy priceStrategy = new PriceStrategy(dimension);
                priceStrategy.setApiNewClient(apiNewClient,httpHelper,commonUtil);
                dimension.setPriceStrategy(priceStrategy);
                tradeModelFactory.addDimention(dimension);
            }catch (Exception e){
               logger.error("初始化配置体系失败:"+str);
            }



        });

    }catch (Exception e){}

    }

}
