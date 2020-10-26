/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class AsyncManager {

    private static AsyncManager instance = null;
    public static final Logger log = Logger.getLogger("AsyncManager");

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private List<String> names = new ArrayList<>();
    private List<Future<?>> futures = new ArrayList<>();

    public static AsyncManager getInstance() {
        if (instance == null) {
            synchronized (log) {
                if (instance == null) {
                    instance = new AsyncManager();
                }
            }
        }
        return instance;
    }

    public void scheduleService(Runnable service, String name, int delayBeforeExecute, int interval) {
        if (!names.contains(name)) {
            Future<?> future = scheduler.scheduleAtFixedRate(
                    service, delayBeforeExecute, interval, TimeUnit.SECONDS);
            futures.add(future);
        }
    }

    public void executeOnceInSeconds(Runnable service, long delayInSeconds) {
        scheduler.schedule(service, delayInSeconds, TimeUnit.SECONDS);
    }

    public void destroyServices() {
        if (futures.size() > 0) {
            for (Future<?> future : futures) {
                try {
                    future.cancel(true);
                } catch (Exception e) {
                    //
                }
            }
        }
    }
}
