package com.self.dubbo.extension;

import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * 静态代码形式的默认适配器
 * @Adaptive 在class上
 * 在扩展配置文件里面配置上adaptive=com.self.dubbo.extension.AdaptiveMyExtension
 * 这样ExtensionLoader.getExtensionLoader(MyExtension.class); 就能获取到AdaptiveMyExtension的实例了
 * Created by   shaojieyue
 * Created date 2015-07-29 11:15
 */
@Adaptive
public class AdaptiveStaticExtension implements StaticExtension {
    public void hello(ExtensionType type) {
        /**
         * 其实它的动态适配也是经过URL传输的参数，写的统一的适配逻辑
         */
        //进行适配
        StaticExtension extension = adaptive(type);
        extension.hello(type);
    }

    /**
     * 适配方法
     * @param type
     * @return
     */
    private StaticExtension adaptive(ExtensionType type) {
        ExtensionLoader extensionLoader = ExtensionLoader.getExtensionLoader(StaticExtension.class);
        StaticExtension extension = null;
        String name =  null;
        switch (type){
            case  DEFAULT:
                name = "default";
                break;
            case OTHER:
                name = "other";
                break;
            default:
                //默认采用spi 默认值
                final SPI annotation = StaticExtension.class.getAnnotation(SPI.class);
                name = annotation.value();
        }
        extension = (StaticExtension)extensionLoader.getExtension(name);
        //合法性判断
        if (extension == null) {
            throw new IllegalArgumentException("no name="+name+" adaptive MyExtension");
        }
        return extension;
    }
}
