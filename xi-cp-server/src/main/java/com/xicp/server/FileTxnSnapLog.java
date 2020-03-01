package com.xicp.server;

import com.xicp.util.StringUtils;
import io.netty.util.internal.StringUtil;

import javax.management.MXBean;
import javax.swing.text.html.parser.Entity;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;

/**
 * @description: FileTxnSnapLog
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class FileTxnSnapLog {

    private final File dataDir;
    private final File snapDir;
    public final static int VERSION = 2;
    public final static String version = "version-";

    public FileTxnSnapLog(File dataDir, File snapDir) throws IOException {
        System.out.printf("Opening datadir:{%s} snapDir:{%s}\n", dataDir, snapDir);
        this.dataDir = new File(dataDir, version + VERSION);
        this.snapDir = new File(snapDir, version + VERSION);
    }

    public File getDataDir() {
        return this.dataDir;
    }

    public File getSnapDir() {
        return this.snapDir;
    }

    public long restore(DataTree dt, Map<Long, Integer> sessions) throws IOException {
        System.out.println("FileTxnSnapLog restore...");
        System.out.println(dataDir.getAbsolutePath());
        System.out.println(snapDir.getAbsolutePath());
        // 找到最新快照
        // 加载数据到内存中
        long total = 0 ;
        try {
            File snapshotFile = new File(dataDir, FileUtil.findLastFile(snapDir, SNAPSHOT_PREFIX));
            if (!snapshotFile.exists()) {
                System.out.println("!snapshotFile.exists()");
                snapshotFile.mkdirs();
            }
            if (!snapshotFile.isFile()) {
                return total;
            }
            InputStreamReader isr = new InputStreamReader(new FileInputStream(snapshotFile));
            BufferedReader br = new BufferedReader(isr);
            String result = null ;

            while((result = br.readLine()) != null){
                String[] kv = result.split("\\$_\\$");
                if (kv.length < 1 || kv.length > 2) {
                    continue;
                }
                if (kv.length < 2) {
                    System.out.println(kv[0].replaceAll("\\$_\\$", ""));
                    dt.addDataNode("", (DataNode) StringUtils.getObjecctByClazz(kv[0].replaceAll("\\$_\\$", ""), DataNode.class));
                } else {
                    dt.addDataNode(kv[0], (DataNode) StringUtils.getObjecctByClazz(kv[1], DataNode.class));
                }
                total ++;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return total;
    }

    private final static String SNAPSHOT_PREFIX = "snapshot";
    private final static String SPLIT_NODE_KV = "$_$";;


    public void save(DataTree dataTree, Map<Long, HashSet<String>> ephemerals) throws IOException {
        long lastZxid = dataTree.lastProcessedZxid;
        File snapshotFile = new File(snapDir,
                SNAPSHOT_PREFIX + "_" + (System.currentTimeMillis() + "_" + lastZxid));
        System.out.println("Snapshotting: 0x{} to {}" + "-" + Long.toHexString(lastZxid) + "-" + snapshotFile);
        System.out.println("snapshotFile -> " + snapshotFile.getAbsolutePath());
        // 过滤掉临时节点
        // 按照 path DataNode 一行序列化，$_$ 分割
        HashSet<String> allEphemerals = new HashSet<>();
        FileOutputStream dest = new FileOutputStream(snapshotFile) ;
        CheckedOutputStream checksum = new CheckedOutputStream(dest,new Adler32());
        OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(checksum), "UTF-8") ;
        PrintWriter pw = new PrintWriter(osw) ;
        Map<String, DataNode> nodes = dataTree.getNodes();
        for (Map.Entry<String, DataNode> entry : nodes.entrySet()) {
            if (allEphemerals.contains(entry.getKey())) {
                continue;
            }
            pw.println(entry.getKey() + SPLIT_NODE_KV + StringUtils.getString(entry.getValue()));
        }
        pw.flush();
        pw.close();
    }
}


