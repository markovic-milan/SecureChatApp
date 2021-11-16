package watcher;

import gui.main.Main;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import gui.controllers.MyChatController;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineWatcher extends Thread {
    private final File file;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private VBox userBox;

    public OnlineWatcher(File file, VBox vbox) {
        this.file = file;
        this.userBox = vbox;
    }

    public boolean isStopped() {
        return stop.get();
    }

    public void stopThread() {
        stop.set(true);
    }

    @Override
    public void run() {
        pratiPromjene();
    }

    public void refreshOnlineUsers() {
        try {
            VBox vBox = MyChatController.readOnlineUsersHBox();
            if (vBox != null) {
                userBox.getChildren().clear();
                userBox.getChildren().add(vBox);
            }else{
                userBox.getChildren().clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pratiPromjene() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = file.toPath().getParent();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!isStopped()) {
                WatchKey key;
                try {
                    key = watcher.poll(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }

                Thread.sleep(50);
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(file.getName())) {
                        Platform.runLater(() -> {
                            if (Main.user.isOnline())
                                refreshOnlineUsers();
                        });
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                Thread.yield();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger(OnlineWatcher.class.getName()).log(Level.WARNING, null, e);
            // Log or rethrow the error
        }
    }
}