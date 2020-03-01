package com.xicp;

import com.xicp.server.FileUtil;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @description: DatadirCleanupManager
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class DatadirCleanupManager {

    private PurgeTaskStatus purgeTaskStatus = PurgeTaskStatus.NOT_STARTED;

    private final String snapDir;

    private final String dataLogDir;

    private final int snapRetainCount;

    private final int purgeInterval;

    private Timer timer;

    public DatadirCleanupManager(String snapDir, String dataLogDir,
                                 int snapRetainCount, int purgeInterval) {
        this.snapDir = snapDir;
        this.dataLogDir = dataLogDir;
        this.snapRetainCount = snapRetainCount;
        this.purgeInterval = purgeInterval;
    }

    public enum PurgeTaskStatus {
        NOT_STARTED, STARTED, COMPLETED;
    }

    public void start() {
        if (PurgeTaskStatus.STARTED == purgeTaskStatus) {
            System.out.println("Purge task is already running.");
            return;
        }
        if (purgeInterval <= 0) {
            System.out.println("Purge task is not scheduled.");
            return;
        }

        timer = new Timer("PurgeTask", true);
        TimerTask task = new PurgeTask(dataLogDir, snapDir, snapRetainCount);
        timer.scheduleAtFixedRate(task, 0, TimeUnit.MINUTES.toMillis(purgeInterval));
        purgeTaskStatus = PurgeTaskStatus.STARTED;
    }

    static class PurgeTask extends TimerTask {
        private String logsDir;
        private String snapsDir;
        private int snapRetainCount;
        public final static int VERSION = 2;
        public final static String version = "version-";
        private final static String SNAPSHOT_PREFIX = "snapshot";

        public PurgeTask(String dataDir, String snapDir, int count) {
            logsDir = dataDir;
            snapsDir = snapDir;
            snapRetainCount = count;
        }

        @Override
        public void run() {
            //while (true){ }

            System.out.println("Purge task started.");
            try {
                //PurgeTxnLog.purge(new File(logsDir), new File(snapsDir), snapRetainCount);
                System.out.println("snapsDir >>> " + snapsDir);
                File snapshotFile = new File(snapsDir, FileUtil.findLastFile(
                        new File(snapsDir, version + VERSION), SNAPSHOT_PREFIX));
                if (!snapshotFile.exists()) {
                    System.out.println("!snapshotFile.exists()");
                    return;
                }
                File snd = new File(snapsDir);
                File[] snds = snd.listFiles();
                int fileCnt = snds.length;
                for (File file : snds) {
                    if (fileCnt-- < snapRetainCount) {
                        return;
                    }
                    if (file.isFile() && file.getName().contains(SNAPSHOT_PREFIX)
                            && !file.getName().equals(snapshotFile.getName())) {
                        file.delete();
                        System.out.println("删除历史快照：" + file.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error occurred while purging." + e);
            }
            System.out.println("Purge task completed.");
        }
    }
}
