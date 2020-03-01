package com.xicp.server;

import java.io.File;

/**
 * @description: FileUtil
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FileUtil {


    public static String findLastFile(File file, String preffix){
        try {
            long max = 0;
            String lastFile = "";
            File[] files = file.listFiles();
            for (File f : files){
                String fileName = f.getName();
                if (fileName.startsWith(preffix)) {
                    long millis = Long.valueOf(fileName.split("_")[1]);
                    if (millis > max) {
                        max = millis;
                        lastFile = fileName;
                    }
                }
            }
            System.out.println(lastFile);
            return lastFile;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
