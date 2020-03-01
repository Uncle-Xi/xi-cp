package com.demo.xi.cp.client;

import com.alibaba.fastjson.JSON;
import com.xicp.WatchedEvent;
import com.xicp.Watcher;
import com.xicp.XiCP;
import com.xicp.server.data.Message;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * @description: HICP
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class HelloXiCp implements Watcher {

    public static void main(String[] args) throws Exception {
        XiCP xc = new XiCP("127.0.0.1:2181", new HelloXiCp());
        System.out.println("请输入操作序号：");
        Message message = null;
        Scanner scanner = new Scanner(System.in);
        while (true){
            if (scanner.hasNext()) {
                String input = scanner.nextLine();
                if ("1".equals(input)) {
                    xc.create("/hello", (input + "create").getBytes(), false, true);
                }
                if ("2".equals(input)) {
                    xc.create("/hello/emp", (input + "create/emp").getBytes(), false, true);
                }
                if ("3".equals(input)) {
                    String result = xc.getData("/hello", true);
                    System.out.println("getData -> " + result);
                }
                if ("4".equals(input)) {
                    xc.setData("/hello/emp", (input + "getData -> setData").getBytes(), true);
                    String result = xc.getData("/hello", true);
                    System.out.println("getData -> setData : getData -> " + result);
                }
                if ("5".equals(input)) {
                    xc.delete("/hello/emp");
                    System.out.println(xc.exists("/hello/emp", true));
                }
                if ("6".equals(input)) {
                    String uuid = UUID.randomUUID().toString();
                    xc.create("/hello/emp", uuid.getBytes(), true, true);
                    List<String> strList = xc.getChildren("/hello", true);
                    for (String str : strList) {
                        String result = xc.getData("/hello/" + str, true);
                        System.out.println("getData -> " + result);
                    }
                }
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        System.out.println("收到 XiCP 回调：" + JSON.toJSONString(event));
    }
}
