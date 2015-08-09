package com.self.dubbo.extension;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**动态扩展接口
 * Created by   shaojieyue
 * Created date 2015-07-29 17:30
 */

@SPI("default")
public interface DynamicExtension {

    /**
     * 其实这里，是使用了url里的参数作为匹配规则
     * 根据url里的type，type1，type2适配实现类
     * @param url
     * @param name
     */
    @Adaptive({"type", "type1","type2"})
    public void dynamic(URL url,String name);
}
