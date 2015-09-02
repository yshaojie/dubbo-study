package com.self;

/**
 * Created by shaojieyue on 8/29/15.
 */
public class IpUtil {
    public static final int seed = 0x7fffffff;

    public static final int ip2int(String ip){
        if (ip == null) {
            throw new RuntimeException("error ip.ip="+ip);
        }
        if ("localhost".equalsIgnoreCase(ip)) {
            ip = "127.0.0.1";
        }

        final String[] strs = ip.split("\\.");

        if (strs.length!=4) {
            throw new RuntimeException("error ip.ip="+ip);
        }
        int ipInt = 0;
        // iterate over each octet
        for(String part : strs) {
            // shift the previously parsed bits over by 1 byte
            ipInt = ipInt << 8;
            // set the low order bits to the current octet
            ipInt |= Integer.parseInt(part);
        }
        return ipInt;
    }

    public static final String int2ip(int ipInt){
        return ((ipInt >> 24 ) & 0xFF) + "." +
                ((ipInt >> 16 ) & 0xFF) + "." +
                ((ipInt >> 8 ) & 0xFF) + "." +
                (ipInt & 0xFF);
    }

    public static void main(String[] args) {
        final int x = ip2int("255.255.255.255");
        System.out.println(x);
        System.out.println(int2ip(x));
        System.out.println(int2ip(1235));
    }
}
