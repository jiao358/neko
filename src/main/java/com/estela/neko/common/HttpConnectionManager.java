package com.estela.neko.common;

import com.estela.neko.core.ConnectJob;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.DateBuilder.*;

/**
 * @author fuming.lj 2018/7/23
 * http链接管理吃
 **/
@Service
public class HttpConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(HttpConnectionManager.class);
    PoolingHttpClientConnectionManager cm = null;
    AtomicBoolean init = new AtomicBoolean(false);

    @PostConstruct
    public void init(){
        PoolingHttpClientConnectionManager tempcm = initPool();
        cm =tempcm;
        boolean initFlag = init.compareAndSet(false,true);
        if(initFlag){
            //注册定时重拾起
            try{
                SchedulerFactory schedulerFactory = new StdSchedulerFactory();
                Scheduler scheduler = schedulerFactory.getScheduler();
                Trigger trigger  = TriggerBuilder.newTrigger().withIdentity("AutoConnection", "AutoConnection")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 16 * * ?")).build();
                JobDetail job = JobBuilder.newJob(ConnectJob.class).withIdentity("job1", "group1").build();
                job.getJobDataMap().put("HttpConnectionManager",this);
                scheduler.scheduleJob(job, trigger);
                scheduler.start();
            }catch (Exception e){
                logger.error("初始化调度器异常");
            }

        }

    }

    public PoolingHttpClientConnectionManager initPool() {
        LayeredConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
            .register("https", sslsf)
            .register("http", new PlainConnectionSocketFactory())
            .build();
        PoolingHttpClientConnectionManager cm =new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(40);
        return cm;
    }

    public synchronized CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build();

        /*CloseableHttpClient httpClient = HttpClients.createDefault();//如果不采用连接池就是这种方式获取连接*/
        return httpClient;
    }
}
