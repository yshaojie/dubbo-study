package com.self;

/**
 * Created by   shaojieyue
 * Created date 2015-07-22 15:56
 */
public class ConsumerTest {
    public static void main(String[] args) {
        ZKRegisterTestApplication application =new ZKRegisterTestApplication(50);
        final boolean hello = application.hello();
        System.out.println(hello);
    }
}




