package com.self;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by   shaojieyue
 * Created date 2015-07-22 11:15
 */
public class ServerApplication {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("start ....");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"dubbo-study-provider.xml"});
        System.out.println("start up");
        context.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("主程序退出");
            }
        }));
        countDownLatch.await();
    }
}
