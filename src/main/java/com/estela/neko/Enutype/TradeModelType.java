package com.estela.neko.Enutype;

/**
 * @author fuming.lj 2018/9/29
 **/
public enum TradeModelType {
    QUA_MODEL("qua_model"),QUA_MODEL200("qua_model200"),QUA_MODEL100("qua_model100"),
    QUA_MODEL300("qua_model300");

    String model;

    TradeModelType(String name){
        model = name;
    }
    public String getValue(){
        return model;
    }
}
