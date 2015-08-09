package com.self.dubbo.extension;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 *
 * Created by   shaojieyue
 * Created date 2015-07-29 11:35
 */
public class AdaptiveMyExtensionMain {
    public static void main(String[] args) {
        final ExtensionLoader<StaticExtension> extensionLoader = ExtensionLoader.getExtensionLoader(StaticExtension.class);
        final StaticExtension cc = extensionLoader.getExtension("other");
        cc.hello(ExtensionType.DEFAULT);
        final StaticExtension adaptiveExtension = extensionLoader.getAdaptiveExtension();
        adaptiveExtension.hello(ExtensionType.UNKNOW);

    }
}
