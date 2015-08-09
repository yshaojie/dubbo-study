package com.self.dubbo.extension;

import com.alibaba.dubbo.common.URL;

/**
 * Created by   shaojieyue
 * Created date 2015-07-29 17:36
 */
public class DefaultDynamicExtension implements DynamicExtension {
    public void dynamic(URL url, String name) {
        System.out.println("default. url="+url+" hello '"+name+"'!");
    }
}
