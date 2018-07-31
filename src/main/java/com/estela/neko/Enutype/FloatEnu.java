package com.estela.neko.Enutype;

/**
 * @author fuming.lj 2018/7/29
 * 用于使用价格波动策略
 * down  负值浮动波动利率
 * up  正直波动利率
 **/
public enum FloatEnu {

    UP_FLOAT("up"),DOWN_FLOAT("down") , ALL_FLOAT("all");

    String strategy;

     FloatEnu(String name){
        strategy = name;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getValue(){
         return strategy;
    }
}
