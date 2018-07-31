package com.estela.neko.config;

import com.estela.neko.Enutype.FloatEnu;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author fuming.lj 2018/7/29
 * 用于存放 策略变量
 **/
@Component
public class Diamond {

    /**
     * 用于控制 流程交易是否能够执行
     */
    public static volatile  boolean canRunning= false;

    /**
     * 使用哪种 价格浮动策略
     */

    public static volatile FloatEnu floatStrategy = FloatEnu.DOWN_FLOAT;

    public static AtomicBoolean sysFirstRun  = new AtomicBoolean(false);


}
