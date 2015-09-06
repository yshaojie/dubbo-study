package com.self;

import com.github.kristofa.brave.TraceFilter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单trace规则,此处参考jd的hydra的采样率实现
 * 即:
 * 每个method每秒采集100为上限，过了100按百分之10%采集
 * Created by shaojieyue on 9/6/15.
 */
public class SimpleTraceFilter implements TraceFilter {
    public static final int SAMPLE_RATE = 10;//超过阀值后的采样率
    private ConcurrentMap<String,Long> tsMap = new ConcurrentHashMap<String,Long>();
    private ConcurrentMap<String,AtomicLong> counterMap = new ConcurrentHashMap<String,AtomicLong>();
    private static final int baseNumber = 50;
    public boolean trace(String requestName) {
        System.out.println(requestName);
        if (requestName == null || "".equals(requestName.trim())) {
            return false;
        }

        //采样时间段内已经采样的请求数
        AtomicLong counter = counterMap.get(requestName);

        if (counter == null) {//初始化
            counterMap.putIfAbsent(requestName,new AtomicLong());
            tsMap.putIfAbsent(requestName, System.currentTimeMillis());
            counter = counterMap.get(requestName);
        }
        //最近一次采样时间段开始时间
        long lastTime = tsMap.get(requestName);
        boolean isTrace = true;
        long n = counter.incrementAndGet();
        if(System.currentTimeMillis() - lastTime  < 1000){
            if(n > baseNumber){//超过采样阀值,按采样率确定本次是否采样
                if(n% SAMPLE_RATE != 0){
                    isTrace = false;
                }
            }
        }else{//已经超过采样时间段,初始化新的采样时间段
            counter.getAndSet(0);
            tsMap.put(requestName,System.currentTimeMillis());
        }
        return isTrace;
    }

    public void close() {

    }
}
