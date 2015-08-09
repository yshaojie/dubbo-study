package com.self;

import com.self.api.DemoService;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by   shaojieyue
 * Created date 2015-07-22 10:19
 */
public class DemoAction {
    DemoService demoService;
    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }

    public void start() throws Exception {
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            try {
                String hello = demoService.sayHello("world" + i);
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + hello);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
    }

}
