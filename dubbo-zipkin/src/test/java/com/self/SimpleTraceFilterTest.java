package com.self;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.*;

/**
 * Created by shaojieyue on 9/6/15.
 */
public class SimpleTraceFilterTest{
    SimpleTraceFilter traceFilter =null;

    @Before
    public void init(){
        traceFilter = new SimpleTraceFilter();
    }

    @org.junit.Test
    public void test_trace() throws InterruptedException {
        //采样率测试
        String helloRequest = "com.self.HelloService.hello";
        String pingRequest = "com.self.HelloService.ping";

        single(helloRequest, pingRequest);
        Thread.sleep(1000L);
        //一秒采样后重新开始
        single(helloRequest, pingRequest);

        boolean trace = traceFilter.trace(null);
        Assert.assertEquals(false,trace);

        trace = traceFilter.trace(" ");
        Assert.assertEquals(false,trace);


    }

    private void single(String helloRequest, String pingRequest) {
        int helloCount = 0;
        int pingCount = 0;
        for (int i=0;i<100;i++){
            final boolean helloTrace = traceFilter.trace(helloRequest);
            final boolean pingTrace = traceFilter.trace(pingRequest);
            if (i > 49) {
                if (helloTrace) {
                    helloCount++;
                }

                if (pingTrace) {
                    pingCount++;
                }
            }else {
                Assert.assertEquals(true, helloTrace);
                Assert.assertEquals(true,pingTrace);
            }
        }
        Assert.assertEquals(5, helloCount);
        Assert.assertEquals(5, pingCount);
    }


}
