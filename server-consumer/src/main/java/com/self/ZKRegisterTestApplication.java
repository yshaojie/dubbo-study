package com.self;

import com.self.api.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by   shaojieyue
 * Created date 2015-07-24 11:52
 */
public class ZKRegisterTestApplication {
    final DemoService demoService;
    final String message ;
    final AtomicInteger counter = new AtomicInteger();
    final String name;
    public ZKRegisterTestApplication(int length) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"dubbo-demo-action.xml"});
        context.start();
        demoService = (DemoService)context.getBean("demoService"); // 获取远程服务代理
        //length kb 的数据
        byte[] data = new byte[1024*length];
        for (int i = 0; i < data.length; i++) {
            data[i]='a';
        }
        message = new String(data);
        name = UUID.randomUUID().toString();
        System.out.println("messageLength="+message.length()+" message="+message);
        System.out.println("dubbo consumer init success. consumerId="+name);
    }

    public boolean hello() {
        String result = demoService.sayHello(message);
        final int count = counter.incrementAndGet();
        if(count %10000==0){
            System.out.println("name="+name+" count="+count+" ts="+(System.currentTimeMillis()/1000));
        }
        return message.equals(result);
    }
}
