package com.self;

import com.self.api.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by   shaojieyue
 * Created date 2015-07-23 13:47
 */
public class ConsumerApplication {
    public static void main(String[] args) {
        int length = 1;
        int threads = 3;
        if (args.length >0) {
            length = Integer.valueOf(args[0]);
            threads = Integer.valueOf(args[1]);
        }

        //length kb 的数据
        byte[] data = new byte[1024*length];
        for (int i = 0; i < data.length; i++) {
            data[i]='a';
        }
        final String msg = new String(data);
        System.out.println("msg======" + msg);
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"dubbo-demo-action.xml"});
        context.start();
        final DemoService demoService = (DemoService)context.getBean("demoService"); // 获取远程服务代理
        final AtomicInteger counter = new AtomicInteger(0);
        ExecutorService executorService = new ThreadPoolExecutor(20,30,1, TimeUnit.DAYS,new ArrayBlockingQueue<Runnable>(20000), new ThreadPoolExecutor.DiscardPolicy());
        while (true){
            try {
                //提交任务
                executorService.submit(new Runnable() {
                    public void run() {
                        String hello = null;
                        try {
                            hello = (String) demoService.sayHello(msg); // 执行远程方法
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        final int count = counter.incrementAndGet();
                        if (count%10000==0) {
                            System.out.println("counter=="+count);
                            System.out.println(hello);
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
