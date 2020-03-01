package com.demo.xi.cp.client;

import com.xicp.WatchedEvent;
import com.xicp.Watcher;
import com.xicp.XiCP;

/**
 * @description: SimpleUse
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class SimpleUse {

    private static final String CONNECT_ADDRS = "127.0.0.1:2181";
    private static XiCP xiCP;
    private static String ROOT_NODE = "/root";

    public static void main(String[] args) throws Exception {
        xiCP = new XiCP(CONNECT_ADDRS, new SimpleWatch());
        xiCP.createPermNode(ROOT_NODE);
        xiCP.getData(ROOT_NODE);
        xiCP.getChildren(ROOT_NODE);
    }
    static class SimpleWatch implements Watcher {
        @Override
        public void process(WatchedEvent event) {
            System.out.println("【收到监控事件】【" + event.getEventType() + "】【监控节点】【" + event.getPath() + "】");
        }
    }
}
