package com.self.dubbo.extension;

/**
 * Created by   shaojieyue
 * Created date 2015-07-29 11:14
 */
public class OtherStaticExtension implements StaticExtension {
    public void hello(ExtensionType type) {
        System.out.println(this.getClass()+" hello world");
    }
}
