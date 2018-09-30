package com.estela.neko.core;

import com.estela.neko.domain.TradeDimension;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author fuming.lj 2018/9/29
 * multity model used
 **/
@Service
public class TradeModelFactory {
    //mutil dimension
    private Map<String,TradeDimension> dimensionMap  =new HashMap<>();


    //交易策略以及配置添加
    public void addDimention(TradeDimension dimension){
        if(dimension!=null){
            dimensionMap.put(dimension.getTradeSemaphore(),dimension);
        }
    }

    public TradeDimension getDimension(String tradeSemaphore){
        return dimensionMap.get(tradeSemaphore);
    }


    public Set<String> getAllCurrency(){
       return  dimensionMap.keySet();


    }








}
