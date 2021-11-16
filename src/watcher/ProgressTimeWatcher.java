package watcher;

import gui.main.Main;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;

import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class ProgressTimeWatcher extends Thread {
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private boolean trace = false;

    private Label[] textLabels = new Label[2];
    private Label[] timeLabels = new Label[2];
    private HBox hbs[] = new HBox[2];
    private String messageReciever;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public ProgressTimeWatcher(Path dir, boolean recursive, String messageReciever) throws IOException {
        this.messageReciever = messageReciever;
        setUI();
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }
        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    @Override
    public void run() {
        for (; ; ) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);
                String putanja = child.toString();
                // print out event

                if (event.kind() == ENTRY_CREATE && putanja.equals(Main.communicationPath + File.separator + "user_" + messageReciever + File.separator + "Inbox" + File.separator + "encryptedInfoAsymmetric.txt")) {
                    timer0 = false;
                    timer1 = true;
                } else if (event.kind() == ENTRY_CREATE && putanja.equals(Main.communicationPath + File.separator + "user_" + messageReciever + File.separator + "tmp" + File.separator + "decryptedInfo.txt")) {
                    timer1 = false;
                    timerStop = true;
                }
                //if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }
            }
            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private boolean timerStop = false, timer0 = true, timer1 = false, timer2 = false;

    private void setUI() {
        for (int i = 0; i < 2; i++) {
            timeLabels[i] = new Label("0.00s");
        }

        textLabels[0] = new Label("Encryption...");
        textLabels[1] = new Label("Decryption...");
        VBox vBox = new VBox();
        Label totalTime = new Label();
        DoubleProperty time = new SimpleDoubleProperty();
        totalTime.textProperty().bind(time.asString("%.2f seconds"));
        AnimationTimer timer = new AnimationTimer() {
            long time0, time1;
            private long startTime;

            @Override
            public void start() {
                startTime = System.currentTimeMillis();
                super.start();
            }

            @Override
            public void handle(long timestamp) {
                long now = System.currentTimeMillis();
                time.set((now - startTime) / 1000.0);
                if (timer0) {
                    timeLabels[0].setText(String.format("%.2f seconds", (now - startTime) / 1000.0));
                    time0 = now - startTime;
                }
                if (timer1) {
                    timeLabels[1].setText(String.format("%.2f seconds", ((now - startTime) - time0) / 1000.0));
                    time1 = time0 - (long) time.get();
                }
                if (timerStop) {
                    this.stop();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Stage stage = (Stage) totalTime.getScene().getWindow();
                    stage.close();
                }
            }
        };
        Region[] regions = new Region[3];

        for (int i = 0; i < 2; i++) {
            regions[i] = new Region();
            hbs[i] = new HBox();
            hbs[i].setHgrow(regions[i], Priority.ALWAYS);
            hbs[i].getChildren().addAll(textLabels[i], regions[i], timeLabels[i]);
            vBox.getChildren().add(hbs[i]);
        }

        regions[2] = new Region();
        HBox totalHb = new HBox(new Label("Total time: "), regions[2], totalTime);
        totalHb.setHgrow(regions[2], Priority.ALWAYS);

        vBox.getChildren().add(totalHb);
        vBox.setSpacing(2);
        vBox.setPadding(new Insets(5, 5, 5, 5));
        Scene scene = new Scene(vBox, 360, 196);
        Stage stage = new Stage();
        stage.setTitle("Connecting...");
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
        timer.start();
    }
}