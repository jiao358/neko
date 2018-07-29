package com.estela.neko.api;

/**
 * @author fuming.lj 2018/7/29
 * 网格交易策略 API
 **/
public interface NetTradeService {

    /**
     * 执行策略
     */
    void execute();

    /**
     * 初始化
     */
    void init();
}
