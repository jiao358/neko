package com.estela.neko.Enutype;

/**
 * @author fuming.lj 2018/9/29
 **/
public enum TradeModelType {
    QUA_MODEL("qua_model");

    String model;

    TradeModelType(String name){
        model = name;
    }
    public String getValue(){
        return model;
    }
}
