package com.xicp.util;

import com.xicp.server.Request;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * @description: SerializeUtils
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class SerializeUtils {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static byte[] serializeRequest(Request request){
        String reqStr = StringUtils.getString(request);
        return reqStr.getBytes(UTF_8);
    }

    public static Request unSerializeRequest(byte[] request) {
        String reqStr = new String(request, UTF_8);
        return (Request) StringUtils.getObjecctByClazz(reqStr, Request.class);
    }
}
