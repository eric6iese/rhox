/*
 * Copyright (C) 2015 Eric Giese
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jjstk.combridge;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Implements the Basic Engine for any number of single-threaded
 * gui-guards.<br/>
 * These consists of checks which are continously performed (and must therefore
 * be non-blocking). As soon as a check matches, its associated action is
 * performed.<br/>
 *
 * TODO: The 'pause' function should simply stop (or restart) the executor
 * service, instead of unscheduling.<br/>
 * The 'exit' function should stop lthe executor service and then call
 * System.exit(1);
 */
public class ScriptGuard {

    private static final Logger LOG = Logger.getLogger(ScriptGuard.class.getName());

    private static final long TIMEOUT = 250L;
    private static final long SHUTDOWN_SECONDS = 10L;

    private final TrayIcon trayIcon;
    private ScheduledExecutorService executor;

    private final List<Runnable> tasks = new CopyOnWriteArrayList<>();
    private final List<ScheduledFuture<?>> results = new LinkedList<>();

    public ScriptGuard() {
        Image img;
        try {
            img = ImageIO.read(getClass().getResource("icon.png"));
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        trayIcon = new TrayIcon(img);

        // Create a pop-up menu components
        PopupMenu popup = new PopupMenu();

        // Clicking this stops all running tasks
        CheckboxMenuItem pauseItem = new CheckboxMenuItem("Pause");
        pauseItem.addActionListener(e -> {
            if (pauseItem.getState()) {
                tasks.forEach(this::scheduleTask);
            } else {
                unschedule();
            }
        });

        // Clicking this stops the entire running
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            stopService();
            System.exit(1);
        });

        popup.add(pauseItem);
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);
    }

    public void start() throws InterruptedException {
        if (executor != null) {
            throw new IllegalStateException("Has already been started!");
        }
        if (!SystemTray.isSupported()) {
            throw new IllegalStateException("SystemTray is not supported");
        }
        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            throw new IllegalStateException(e);
        }
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void add(Supplier<Boolean> condition, Runnable task) {
        Runnable t = () -> {
            try {
                if (!condition.get()) {
                    return;
                }
                task.run();
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, e, () -> "An Exception occured in one of the tasks!");
            }
        };
        scheduleTask(t);
        tasks.add(t);
    }

    private synchronized void scheduleTask(Runnable task) {
        results.add(executor.schedule(task, TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private synchronized void togglePause(boolean stop) {
        /*  if (pauseItem.getState()) {

        } else {
            unschedule();
        }*/
    }

    private synchronized void startService() {
        tasks.forEach(this::scheduleTask);
    }

    private synchronized void stopService() {
        shutdownAndAwaitTermination(executor, SHUTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Unschedules all running tasks after completion.
     */
    private synchronized void unschedule() {
        Iterator<ScheduledFuture<?>> itr = results.iterator();
        while (itr.hasNext()) {
            ScheduledFuture<?> f = itr.next();
            try {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            } catch (RuntimeException ex) {
                LOG.log(Level.SEVERE, "Cannot cancel a task!", ex);
            }
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.log(Level.SEVERE, "Cannot wait for a task", ex);
            }
            itr.remove();
        }
    }

    /**
     * Code below copied from guava's MoreExecutors.
     */
    private static boolean shutdownAndAwaitTermination(
            ExecutorService service, long timeout, TimeUnit unit) {
        // Disable new tasks from being submitted
        service.shutdown();
        try {
            long halfTimeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, unit) / 2;
            // Wait for half the duration of the timeout for existing tasks to terminate
            if (!service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS)) {
                // Cancel currently executing tasks
                service.shutdownNow();
                // Wait the other half of the timeout for tasks to respond to being cancelled
                service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException ie) {
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            // (Re-)Cancel if current thread also interrupted
            service.shutdownNow();
        }
        return service.isTerminated();
    }
}
