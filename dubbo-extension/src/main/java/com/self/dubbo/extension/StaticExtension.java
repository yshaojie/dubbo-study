package com.self.dubbo.extension;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * 静态扩展接口
 * Created by   shaojieyue
 * Created date 2015-07-29 11:06
 */
@SPI("default")
public interface StaticExtension {
    public void hello(ExtensionType type);
}

