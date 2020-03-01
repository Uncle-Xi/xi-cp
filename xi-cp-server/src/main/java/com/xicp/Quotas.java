package com.xicp;

/**
 * @description: Quotas
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class Quotas {

    public static final String procXiCP = "/XiCP";
    public static final String quotaXiCP = "/XiCP/quota";
    public static final String limitNode = "XiCP_limits";
    public static final String statNode = "XiCP_stats";

    public static String quotaPath(String path) {
        return quotaXiCP + path + "/" + limitNode;
    }

    public static String statPath(String path) {
        return quotaXiCP + path + "/" + statNode;
    }
}
