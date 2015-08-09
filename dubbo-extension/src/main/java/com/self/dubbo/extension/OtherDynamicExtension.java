package com.self.dubbo.extension;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;

/**
 * Created by   shaojieyue
 * Created date 2015-07-29 17:38
 */

public class OtherDynamicExtension implements DynamicExtension {
    public void dynamic(URL url, String name) {
        System.out.println("other. url="+url+" hello '"+name+"'!");
    }
}
