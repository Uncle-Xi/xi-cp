package com.xicp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @description: WatchManager
 * ...
 * @author: Uncle.Xi 2020
 * @since: 1.0
 * @Environment: JDK1.8 + CentOS7.x + ?
 */
public class WatchManager {

    private final HashMap<String, HashSet<Watcher>> watchTable = new HashMap<>();
    private final HashMap<Watcher, HashSet<String>> watch2Paths = new HashMap<>();

    public synchronized int size(){
        int result = 0;
        for(Set<Watcher> watches : watchTable.values()) {
            result += watches.size();
        }
        return result;
    }

    public synchronized void addWatch(String path, Watcher watcher) {
        HashSet<Watcher> list = watchTable.get(path);
        if (list == null) {
            list = new HashSet<Watcher>(4);
            watchTable.put(path, list);
        }
        list.add(watcher);

        HashSet<String> paths = watch2Paths.get(watcher);
        if (paths == null) {
            paths = new HashSet<String>();
            watch2Paths.put(watcher, paths);
        }
        paths.add(path);
        //System.out.println("watchTable.size() -> " + watchTable.size());
    }

    public synchronized void removeWatcher(Watcher watcher) {
        HashSet<String> paths = watch2Paths.remove(watcher);
        if (paths == null) {
            return;
        }
        for (String p : paths) {
            HashSet<Watcher> list = watchTable.get(p);
            if (list != null) {
                list.remove(watcher);
                if (list.size() == 0) {
                    watchTable.remove(p);
                }
            }
        }
    }

    public Set<Watcher> triggerWatch(String path, EventType type) {
        return triggerWatch(path, type, null);
    }

    public Set<Watcher> triggerWatch(String path, EventType type, Set<Watcher> supress) {
        //System.out.println("EventType type -> " + type);
        WatchedEvent e = new WatchedEvent(type, path);
        HashSet<Watcher> watchers;
        synchronized (this) {
            watchers = watchTable.remove(path);
            if (watchers == null || watchers.isEmpty()) {
                return null;
            }
            for (Watcher w : watchers) {
                HashSet<String> paths = watch2Paths.get(w);
                if (paths != null) {
                    paths.remove(path);
                }
            }
        }
        for (Watcher w : watchers) {
            if (supress != null && supress.contains(w)) {
                continue;
            }
            System.out.println("[WatchManager] [EventType]:[" + type);
            w.process(e);
        }
        return watchers;
    }
}
