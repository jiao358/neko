package com.estela.neko.core;

import com.estela.neko.common.HttpConnectionManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

/**
 * @author fuming.lj 2018/10/16
 **/
public class ConnectJob implements Job{

    HttpConnectionManager manager;
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Map jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Object managerTemp =jobDataMap.get("HttpConnectionManager");
        System.out.println("执行:"+managerTemp);
        if(managerTemp!=null && managerTemp instanceof HttpConnectionManager){
            manager = (HttpConnectionManager)managerTemp;
            manager.init();;
        }

    }




}
