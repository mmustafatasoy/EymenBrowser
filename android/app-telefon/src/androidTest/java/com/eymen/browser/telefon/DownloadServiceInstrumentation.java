package com.eymen.browser.telefon;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Runs only in the separate test APK. No receiver or test hook in production. */
public final class DownloadServiceInstrumentation extends Instrumentation {
    private DownloadCoordinator downloads;
    private DownloadRegistry registry;
    private final List<SavedDownload> completed = new ArrayList<>();
    private final String prefix = "eymen-service-audit-" + System.currentTimeMillis() + "-";
    private final CountDownLatch stopped = new CountDownLatch(1);
    private Activity activity;
    private final DownloadObserver observer = new DownloadObserver() {
        @Override public void onSnapshot(DownloadSnapshot snapshot) {}
        @Override public void onMessage(int message) {}
        @Override public void onCompleted(SavedDownload file) { completed.add(file); }
    };

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        int resultCode = Activity.RESULT_CANCELED;
        Application app = (Application) getTargetContext().getApplicationContext();
        Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStopped(Activity value) {
                if (value == activity) stopped.countDown();
            }
            @Override public void onActivityCreated(Activity value, Bundle state) {}
            @Override public void onActivityStarted(Activity value) {}
            @Override public void onActivityResumed(Activity value) {}
            @Override public void onActivityPaused(Activity value) {}
            @Override public void onActivitySaveInstanceState(Activity value, Bundle state) {}
            @Override public void onActivityDestroyed(Activity value) {}
        };
        app.registerActivityLifecycleCallbacks(lifecycle);
        try {
            app.getSharedPreferences("eymen_browser_setup", Context.MODE_PRIVATE).edit()
                .putBoolean("setup_completed", true).commit();
            activity = startActivitySync(new Intent(app, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            downloads = DownloadCoordinator.Companion.get(app);
            registry = new DownloadRegistry(app);
            runOnMainSync(() -> downloads.observe(observer));

            // Cancellation closes the actual blocking source; the next file runs.
            GateStream cancelled = new GateStream(new byte[4096]);
            enqueue("cancel.bin", cancelled, 4096);
            await(() -> snapshot().getActive() != null, "active foreground transfer");
            require(isForegroundServiceRunning(), "dataSync service must be in foreground");
            require(cancelled.entered.await(10, TimeUnit.SECONDS), "real output opened before cancellation");
            runOnMainSync(() -> downloads.cancelActive());
            await(cancelled.closed::get, "active input stream close");
            await(() -> !hasWork(), "cancelled job settlement");
            require(registry.items().stream().noneMatch(f -> f.getName().equals(prefix + "cancel.bin")),
                "cancelled file must not enter completed history");
            await(() -> noMediaRow(prefix + "cancel.bin"), "partial MediaStore cleanup");

            byte[] bytes = new byte[200123];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 251);
            GateStream first = new GateStream(bytes);
            GateStream overflow = new GateStream(new byte[1]);
            runOnMainSync(() -> {
                offer("first.bin", first, bytes.length);
                offer("remove.bin", new ByteArrayInputStream(bytes), bytes.length);
                offer("third.bin", new ByteArrayInputStream(bytes), bytes.length);
                offer("fourth.bin", new ByteArrayInputStream(bytes), bytes.length);
                offer("overflow.bin", overflow, 1);
            });
            await(() -> snapshot().getActive() != null && snapshot().getQueued().size() == 3,
                "bounded active plus three waiting");
            await(overflow.closed::get, "overflow response disposal");
            QueuedDownload remove = snapshot().getQueued().stream()
                .filter(f -> f.getName().equals(prefix + "remove.bin")).findFirst().get();
            runOnMainSync(() -> downloads.cancel(remove.getId()));
            require(snapshot().getQueued().size() == 2, "queued cancellation must release its slot");

            runOnMainSync(() -> require(activity.moveTaskToBack(true), "move real Activity to background"));
            require(stopped.await(10, TimeUnit.SECONDS), "real Activity.onStop must occur");
            require(isForegroundServiceRunning(), "service must survive Activity.onStop");
            require(snapshot().getActive() != null, "backgrounding must not cancel the active file");
            first.release.countDown();
            await(() -> !hasWork(), "background FIFO completion");
            List<SavedDownload> files = new ArrayList<>();
            runOnMainSync(() -> files.addAll(completed));
            require(files.size() == 3, "exactly three completed files expected");
            require(files.get(0).getName().equals(prefix + "first.bin") &&
                    files.get(1).getName().equals(prefix + "third.bin") &&
                    files.get(2).getName().equals(prefix + "fourth.bin"), "FIFO order after cancellation");
            for (SavedDownload file : files) {
                require(file.getBytes() == bytes.length, "actual saved byte count");
                try (InputStream input = app.getContentResolver().openInputStream(Uri.parse(file.getUri()))) {
                    require(input != null, "completed content URI must open");
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                    require(Arrays.equals(digest.digest(), MessageDigest.getInstance("SHA-256").digest(bytes)),
                        "saved file checksum must match the source");
                }
            }
            await(() -> !isForegroundServiceRunning(), "idle foreground service shutdown");
            result.putString("eymen_download_audit", "passed");
            result.putString("checks", "foreground,active_cancel,partial_cleanup,queue_limit,queued_cancel,real_onStop,background_FIFO,SHA256,idle_stop");
            resultCode = Activity.RESULT_OK;
        } catch (Throwable error) {
            result.putString("eymen_download_audit", "failed");
            result.putString("stack", android.util.Log.getStackTraceString(error));
        } finally {
            app.unregisterActivityLifecycleCallbacks(lifecycle);
            if (downloads != null) runOnMainSync(() -> {
                downloads.unobserve(observer);
                downloads.cancelAll();
            });
            if (registry != null) {
                for (SavedDownload file : registry.items()) if (file.getName().startsWith(prefix))
                    app.getContentResolver().delete(Uri.parse(file.getUri()), null, null);
                registry.clearListOnly();
            }
        }
        finish(resultCode, result);
    }
    private void enqueue(String name, InputStream body, long bytes) {
        runOnMainSync(() -> offer(name, body, bytes));
    }
    private void offer(String name, InputStream body, long bytes) {
        downloads.enqueue(prefix + name, "application/octet-stream", body, bytes, null);
    }
    private DownloadSnapshot snapshot() {
        AtomicReference<DownloadSnapshot> value = new AtomicReference<>();
        runOnMainSync(() -> value.set(downloads.snapshot()));
        return value.get();
    }
    private boolean hasWork() {
        AtomicBoolean value = new AtomicBoolean();
        runOnMainSync(() -> value.set(downloads.hasWork()));
        return value.get();
    }
    @SuppressWarnings("deprecation") private boolean isForegroundServiceRunning() {
        for (ActivityManager.RunningServiceInfo service : getTargetContext()
                .getSystemService(ActivityManager.class).getRunningServices(100)) {
            if (service.service.getClassName().equals(EymenDownloadService.class.getName()) && service.foreground)
                return true;
        }
        return false;
    }
    private boolean noMediaRow(String name) {
        try (android.database.Cursor cursor = getTargetContext().getContentResolver().query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[] {"_id"},
                "_display_name = ?", new String[] {name}, null)) {
            return cursor != null && cursor.getCount() == 0;
        }
    }
    private static void await(BooleanSupplier condition, String name) throws Exception {
        long until = android.os.SystemClock.elapsedRealtime() + 20000;
        while (!condition.getAsBoolean()) {
            if (android.os.SystemClock.elapsedRealtime() > until) throw new AssertionError("Timed out: " + name);
            Thread.sleep(50);
        }
    }
    private static void require(boolean value, String name) { if (!value) throw new AssertionError(name); }
    private static final class GateStream extends ByteArrayInputStream {
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicBoolean closed = new AtomicBoolean();
        GateStream(byte[] bytes) { super(bytes); }
        @Override public synchronized int read(byte[] buffer, int offset, int length) {
            entered.countDown();
            try {
                if (!release.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("gate timeout");
            } catch (InterruptedException error) { throw new IllegalStateException(error); }
            if (closed.get()) throw new IllegalStateException("closed");
            return super.read(buffer, offset, length);
        }
        @Override public void close() throws IOException { closed.set(true); release.countDown(); super.close(); }
    }
}
