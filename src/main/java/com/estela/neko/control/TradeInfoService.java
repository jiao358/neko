package com.estela.neko.control;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.utils.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author fuming.lj 2018/7/31
 **/
@RequestMapping("/tradeInfo")
@RestController
public class TradeInfoService {
    @Autowired
    CommonUtil commonUtil;

    @RequestMapping("/get")
    public Object getTradeInfo(){
        SystemModel systemModel = commonUtil.generateCurrentModel();

        return JSONObject.toJSON(systemModel);

    }
}
