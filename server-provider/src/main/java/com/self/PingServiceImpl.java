package com.self;

import com.self.api.PingService;

/**
 * Created by   shaojieyue
 * Created date 2015-07-24 19:29
 */
public class PingServiceImpl implements PingService {
    public String ping() {
        return "ok,time="+System.currentTimeMillis();
    }
}
