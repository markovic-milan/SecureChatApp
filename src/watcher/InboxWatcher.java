package watcher;

import gui.controllers.MyChatController;
import gui.controllers.StegController;
import gui.main.Main;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import model.SteganographyModel;
import model.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static gui.controllers.MyChatController.encryptString;
import static gui.controllers.MyChatController.messageReciever;
import static java.nio.file.StandardWatchEventKinds.*;

public class InboxWatcher extends Thread {
    private static ProgressTimeWatcher myProgress;
    private File file;
    private TextArea messageArea;
    private AtomicBoolean stop = new AtomicBoolean(false);
    private User user = Main.user;
    private MyChatController chatController = new MyChatController();
    private Button disconnectButton;

    public InboxWatcher(File file, TextArea messageArea, Button disconnectBuutton) {
        this.file = file;
        this.messageArea = messageArea;
        this.disconnectButton = disconnectBuutton;
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

    public void refreshMessages() {
        String message = "";
        if (user.isConnected()) {
            chatController.decryptFileSymmetric();
            try {
                File decryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedMessage.txt");
                BufferedReader read = new BufferedReader(new FileReader(decryptedFile));
                message = read.readLine();
                messageArea.appendText(message.split("#")[0] + ": " + message.split("#")[1] + "\n");
                read.close();
                decryptedFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Thread dekriptNit = new Thread(() -> chatController.decrypt());
            dekriptNit.setDaemon(true);
            dekriptNit.start();
            // chatController.decrypt();
        }
    }

    private void decodeMessage() {
        String decoded = "";
        String decrypted = "";
        StegController controller = new StegController(new SteganographyModel(user.getUsername()));
        try {
            Thread.sleep(200);

            File file = new File(Main.communicationPath + File.separator + "user_" + Main.user.getUsername() + File.separator + "Inbox\\person.png");
            Image image = new Image(file.toURI().toString());

            Thread.sleep(200);

            decoded = controller.onDecode(image);

            decrypted = chatController.decryptString(decoded);

            System.out.println("Dekriptovano u slici: " + decrypted);
            if (file.exists()) {
                file.delete();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //dekriptuj sadrzaj decoded
        String parametri[] = decrypted.split(":");

        if (parametri[1].equals("START")) {
            String otherUsername = parametri[0];
            ButtonType yes = new ButtonType("YES", ButtonBar.ButtonData.OK_DONE);
            ButtonType no = new ButtonType("NO", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alertInformation = new Alert(Alert.AlertType.INFORMATION, "Do you want to chat with " + otherUsername, yes, no);
            alertInformation.setHeaderText(null);
            alertInformation.setTitle("Chat request");
            Optional<ButtonType> result = alertInformation.showAndWait();
            MyChatController.requestList.add(parametri[0]);

            String message;
            if (result.get().getText() == "YES") {
                disconnectButton.setDisable(false);
                message = user.getUsername() + ":" + "CONFIRMED";
                MyChatController.messageReciever = parametri[0];
                System.out.println("YES");
            } else {
                message = user.getUsername() + ":" + "NOT_CONFIRMED";
                Main.showAlert("Korisnik " + user.getUsername() + " ne zeli komunikaciju!", false);
                System.out.println("NO");
            }

            String encryted = encryptString(message);
            controller.onEncode(encryted, parametri[0]);

        } else if (parametri[1].equals("END")) {
            System.out.println("END");
            Main.showAlert("Korisnik " + MyChatController.messageReciever + " je prekinuo komunikaciju.", false);
            Main.deleteFiles(new File(Main.communicationPath + File.separator + "user_" + user.getUsername()));
            user.setConnected(false);
            disconnectButton.setDisable(true);
            Main.deleteFiles(new File(Main.communicationPath + File.separator + "user_" + user.getUsername()));
            messageArea.clear();
        } else if (parametri[1].equals("CONFIRMED") && parametri[0].equals(MyChatController.messageReciever)) {
            disconnectButton.setDisable(false);
            timer();
            Thread kript = new Thread(() -> chatController.crypt());
            kript.setDaemon(true);
            kript.start();
            System.out.println("CONFIRMED" + " od korisnika " + MyChatController.messageReciever);
        } else if (parametri[1].equals("NOT_CONFIRMED") && parametri[0].equals(MyChatController.messageReciever)) {
            System.out.println("NOT_CONFIRMED" + " od korisnika " + MyChatController.messageReciever);
        } else {
            System.out.println("Greska pri dekodovanju!");
        }
    }

    private void pratiPromjene() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = file.toPath();
            path.register(watcher, ENTRY_CREATE);
            while (!isStopped()) {
                WatchKey key;
                try {
                    key = watcher.poll(25, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    System.out.println(kind + "    " + filename);

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if ((kind == ENTRY_CREATE && filename.toString().startsWith("encryptedInfoAsymmetric.txt")) || (kind == ENTRY_CREATE && filename.toString().startsWith("encryptedMessage.txt") && user.isConnected())) {
                        Platform.runLater(() -> refreshMessages());
                    } else if (kind == ENTRY_CREATE && filename.toString().equals("person.png")) {
                        Platform.runLater(() -> decodeMessage());
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
            Logger.getLogger(InboxWatcher.class.getName()).log(Level.WARNING, null, e);
            // Log or rethrow the error
        }
    }

    private static void timer() {
        Runnable zadatakEvents = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        File fajl = new File(Main.communicationPath);
                        try {
                            myProgress = new ProgressTimeWatcher(fajl.toPath(), true, messageReciever);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (fajl.exists()) {
                            Thread progressThread = new Thread(myProgress);
                            progressThread.setDaemon(true);
                            progressThread.start();
                        }
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (
                        InterruptedException izuzetak) {
                    izuzetak.printStackTrace();
                }
            }
        };
        Thread showProgressThread = new Thread(zadatakEvents);
        showProgressThread.setDaemon(true);
        showProgressThread.start();
    }
}