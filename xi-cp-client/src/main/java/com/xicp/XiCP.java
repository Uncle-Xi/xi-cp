package com.xicp;

import com.alibaba.fastjson.JSON;
import com.xicp.client.XiCPClient;
import com.xicp.server.data.Message;
import com.xicp.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 * @description: XiCPServer
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class XiCP {

    protected final XiCPClient cnxn;

    public XiCP(String connectString, Watcher watcher) throws IOException {
        System.out.println("Initiating client connection, connectString=" + connectString + " watcher=" + watcher);
        cnxn = new XiCPClient(connectString, this, watcher);
        cnxn.start();
    }

    public String createSync(final String path, byte data[], boolean ephemeral, boolean orderly) throws Exception {
        Message message = new Message();
        message.setType(OpCode.create);
        message.setPath(path);
        message.setSync(true);
        message.setOrderly(orderly);
        message.setContent(data == null? null : new String(data));
        message.setEphemer(ephemeral);
        Message result = cnxn.submitRequest(message);
        //System.out.println("客户端返回：" + StringUtils.getString(result));
        return result.getPath();
    }

    public void create(final String path, byte data[], boolean ephemer, boolean orderly) throws Exception {
        Message message = new Message();
        message.setType(OpCode.create);
        message.setPath(path);
        message.setSync(false);
        message.setOrderly(orderly);
        message.setContent(data == null? null : new String(data));
        message.setEphemer(ephemer);
        cnxn.submitRequest(message);
        //if (watched) {
        //    System.out.println("create watched...");
        //    message.setSync(false);
        //    message.setType(OpCode.getData);
        //    message.setPath(path);
        //    cnxn.submitRequest(message);
        //}
    }

    public void delete(final String path) throws Exception {
        Message message = new Message();
        message.setType(OpCode.delete);
        message.setPath(path);
        message.setSync(false);
        cnxn.submitRequest(message);
    }

    public void setData(final String path, byte data[], boolean watched) throws Exception {
        Message message = new Message();
        message.setType(OpCode.setData);
        message.setPath(path);
        message.setSync(false);
        message.setContent(new String(data));
        cnxn.submitRequest(message);
        //if (watched) {
        //    message.setSync(false);
        //    message.setType(OpCode.getData);
        //    message.setPath(path);
        //    cnxn.submitRequest(message);
        //}
    }

    public synchronized String getData(final String path, boolean watched) throws Exception {
        Message message = new Message();
        message.setSync(true);
        message.setType(OpCode.getData);
        message.setPath(path);
        Message result = cnxn.submitRequest(message);
        return result.getContent();
    }

    public synchronized List<String> getChildren(final String path, boolean watched) throws Exception {
        Message message = new Message();
        message.setSync(true);
        message.setType(OpCode.getChildren);
        message.setPath(path);
        Message result = cnxn.submitRequest(message);
        List<String> list = new ArrayList<>();
        //System.out.println("客户端返回：" + StringUtils.getString(list));
        if (result.getContent() != null && !result.getContent().trim().equals("")) {
            try {
                list = JSON.parseArray(result.getContent(), String.class);
            } catch (Exception e){

            }
        }
        return list;
    }

    public synchronized boolean exists(final String path, boolean watched) throws Exception {
        Message message = new Message();
        message.setSync(true);
        message.setType(OpCode.exists);
        message.setPath(path);
        Message result = cnxn.submitRequest(message);
        return result.getExists();
    }

    public String createTermOrderlyNode(String path) {
        try {
            return this.createSync(path, path.getBytes(), true, true);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void createTermNode(String path) {
        this.createNode(path, path, true);
    }

    public void createPermNode(String path) {
        this.createNode(path, path, false);
    }

    public void createTermNode(String path, String data) {
        this.createNode(path, data, true);
    }

    public void createPermNode(String path, String data) {
        this.createNode(path, data, false);
    }

    public void createNode(String path, String data, boolean ephemer) {
        if (path == null) {
            System.out.println("path is null...");
            return;
        }
        try {
            if (!this.exists(path, true)) {
                this.create(path, (data == null ? path : data).getBytes(), ephemer, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("createNode Exception -> " + path);
        }
    }

    public synchronized boolean exsits(String path) {
        if (path == null) {
            System.out.println("path is null...");
            return false;
        }
        try {
            return this.exists(path, true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("exsits Exception -> " + path);
        }
        return false;
    }

    public void delNode(String path) {
        if (path == null) {
            System.out.println("path is null...");
            return;
        }
        try {
            if (this.exists(path, true)) {
                this.delete(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("delNode Exception -> " + path);
        }
    }

    public String getData(String path) {
        if (path == null) {
            System.out.println("path is null...");
            return null;
        }
        try {
            if (this.exists(path, true)) {
                return this.getData(path, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData Exception -> " + path);
        }
        return null;
    }

    public List<String> getChildren(String path) {
        if (path == null) {
            System.out.println("path is null...");
            return null;
        }
        try {
            if (this.exists(path, true)) {
                return this.getChildren(path, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getChildren Exception -> " + path);
        }
        return null;
    }

    public void close(){
        cnxn.shutdownNow();
    }
}
