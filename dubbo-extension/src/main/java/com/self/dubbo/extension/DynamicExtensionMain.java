package com.self.dubbo.extension;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by   shaojieyue
 * Created date 2015-07-29 17:45
 */
public class DynamicExtensionMain {
    public static void main(String[] args) {
        final ExtensionLoader<DynamicExtension> extensionLoader = ExtensionLoader.getExtensionLoader(DynamicExtension.class);
        Map map = new HashMap();
        map.put("type","other");
        map.put("type1","default");
        map.put("type2","other");
        URL url = new URL("test","localhost",8080,"/api",map);
        extensionLoader.getAdaptiveExtension().dynamic(url,"shao");
    }
}
