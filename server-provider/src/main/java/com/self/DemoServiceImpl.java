package com.self;

import com.self.api.DemoService;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by shaojieyue on 7/22/15.
 */
public class DemoServiceImpl implements DemoService{

    private AtomicInteger counter = new AtomicInteger();

    public String sayHello(String name) {
        final int cunt = counter.incrementAndGet();
        if (cunt%10000==0) {
            System.out.println(cunt);
        }
        return name;
    }

    public String queryInfo(String name) {
        return System.currentTimeMillis()+"";
    }
}
