package gui.controllers;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import model.*;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import gui.main.*;
import watcher.InboxWatcher;
import watcher.OnlineWatcher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;

import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MyChatController implements Initializable {
    @FXML
    private Button disconnectButton;
    @FXML
    private VBox liveUsersBox;
    @FXML
    private TextArea messageTA;
    @FXML
    private Label sendMessage;
    @FXML
    private ImageView profileImage;
    @FXML
    private Circle onlineCircle;
    @FXML
    private Label onlineLabel;
    @FXML
    private Label usernameLabel;
    @FXML
    private TextArea textSpace;

    public static String messageReciever;
    private User user = Main.user;
    public static List<String> requestList = new ArrayList<>();
    private static OnlineWatcher onlineWatcher;
    public static InboxWatcher inboxWatcher;

    public MyChatController() {
    }

    @FXML
    public void disconnectAction(ActionEvent e) {
        disconnectButton.setDisable(true);
        textSpace.clear();
        StegController controller = new StegController(new SteganographyModel(messageReciever));
        String message = Main.user.getUsername() + ":" + "END";
        String encryted = encryptString(message);
        System.out.println("Enkriptovano u slici: " + encryted);
        controller.onEncode(encryted, messageReciever);
        user.setConnected(false);
        Main.deleteFiles(new File(Main.communicationPath + File.separator + "user_" + user.getUsername()));
    }

    @FXML
    public void sendMessageEnterAction() {
        messageTA.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)
                    if (user.isConnected()) {
                        sendFunction();
                    } else {
                        messageTA.setText("");
                        Main.showAlert("Niste uspostavili konekciju!", true);
                    }
            }
        });
    }

    @FXML
    public void sendMessageClickAction() {
        sendMessage.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (user.isConnected()) {
                    sendFunction();
                } else {
                    messageTA.setText("");
                    Main.showAlert("Niste uspostavili konekciju!", true);
                }
            }
        });
    }

    public void sendFunction() {
        textSpace.appendText(user.getUsername() + ": " + messageTA.getText());
        //upisi u inbox primaoca!
        try {
            File symmetricKeyFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "symmetricKey.txt");
            File file = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "message.txt");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);
            writer.write(user.getUsername() + "#" + messageTA.getText());
            writer.flush();
            writer.close();
            if (!checkReceiverCertificate(messageReciever)) {
                Main.showAlert("Sertifikat (" + messageReciever + ") je povučen!", true);
                return;
            } else if (!checkSenderCertificate(user.getUsername())) {
                Main.showAlert("Sertifikat (" + user.getUsername() + ") je povučen!", true);
                return;
            }
            if (user.isConnected()) {//slanje enkriptovane poruke
                Thread kript = new Thread(() -> cryptFileSymmetric());
                kript.setDaemon(true);
                kript.start();
            } else {//slanje digitalne envelope
                Thread kript = new Thread(() -> crypt());
                kript.setDaemon(true);
                kript.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.messageTA.setText("");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        disconnectButton.setDisable(true);
        textSpace.setWrapText(true);
        textSpace.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        textSpace.setEditable(false);
        final Circle circle = new Circle(32.5, 32.5, 23.5);
        profileImage.setClip(circle);
        onlineLabel.setText("Online");
        usernameLabel.setText(Main.user.getUsername());
        System.out.println("Username: " + user.getUsername());
        //     user.setUsername(usernameLabel.getText());
        if (onlineLabel.getText() == "Online") {
            onlineCircle.setFill(Color.GREEN);
        } else if (onlineLabel.getText() == "Offline") {
            onlineCircle.setFill(Color.GRAY);
        }
    }

    private static void watchInbox(String user, TextArea textArea, Button disconnectButton) {
        File dir = new File(Main.communicationPath + File.separator + "user_" + user + File.separator + "Inbox");
        inboxWatcher = new InboxWatcher(dir, textArea, disconnectButton);
        if (dir.exists()) {
            Thread nit = new Thread(inboxWatcher);
            nit.setDaemon(true);
            nit.start();
        }
    }

    private static void watchOnlineUsers(VBox liveUsersBox) {
        File fajl = new File(Main.onlineUsersFile);
        onlineWatcher = new OnlineWatcher(fajl, liveUsersBox);
        if (fajl.exists()) {
            Thread nit = new Thread(onlineWatcher);
            nit.setDaemon(true);
            nit.start();
        }
    }

    public static void showChanges(VBox liveUserBox, String username, TextArea textSpace, Button disconnectButton) {
        Runnable zadatakEvents = new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        watchOnlineUsers(liveUserBox);
                        watchInbox(username, textSpace, disconnectButton);
                    }
                });
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException izuzetak) {
                    izuzetak.printStackTrace();
                }
            }
        };
        Thread prikaziPromjene = new Thread(zadatakEvents);
        prikaziPromjene.setDaemon(true);
        prikaziPromjene.start();
    }

    public void readOnlineUsers() {
        try {
            VBox vBox = readOnlineUsersHBox();
            if (vBox != null) {
                liveUsersBox.getChildren().clear();
                liveUsersBox.getChildren().add(vBox);
            } else {
                liveUsersBox.getChildren().clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static VBox readOnlineUsersHBox() throws IOException {
        VBox vBox = new VBox();
        File file = new File(Main.onlineUsersFile);
        Main.onlineUsers = Files.lines(file.toPath()).filter(line -> !(line.contains(Main.user.getUsername())) && !line.startsWith("=")).collect(Collectors.toList());
        System.out.println("Svi onlajn useri: ");
        for (String name : Main.onlineUsers) {
            System.out.println(name);
        }
        for (String name : Main.onlineUsers) {
            HBox hBox = new HBox(7);
            hBox.setPrefWidth(190);
            hBox.setPrefHeight(20);

            Label label = new Label(name);
            label.setTextFill(Paint.valueOf("#46a2d7"));
            label.setFont(Font.font(14));

            label.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    // System.out.println(name);
                    //steganografija prve poruke
                    StegController controller = new StegController(new SteganographyModel(name));
                    messageReciever = name;

                    String message = Main.user.getUsername() + ":" + "START";
                    String encryted = encryptString(message);
                    System.out.println("Enkriptovano u slici: " + encryted);
                    controller.onEncode(encryted, messageReciever);
                }
            });
            Circle circle = new Circle(4);
            circle.setTranslateY(5);
            circle.setFill(Color.GREEN);
            circle.setStroke(Color.BLACK);

            hBox.getChildren().addAll(label, circle);
            vBox.getChildren().add(hBox);
        }
        return vBox;
    }

    public static String encryptString(String strClearText) {
        String strData = "";
        try {
            SecretKeySpec skeyspec = Main.KS;
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, skeyspec);
            byte[] encrypted = cipher.doFinal(strClearText.getBytes());
            strData = new String(Base64.getEncoder().encode(encrypted));
        } catch (Exception e) {
            e.printStackTrace();
            Main.showAlert("Enkripcija kod steganografije nije uspjela", true);
            return null;
        }
        return strData;
    }

    public static String decryptString(String strEncrypted) {
        String strData = "";
        try {
            SecretKeySpec skeyspec = Main.KS;
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, skeyspec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(strEncrypted));
            strData = new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            Main.showAlert("Dekripcija kod steganografije nije uspjela", true);
        }
        return strData;
    }

    public void cryptFileSymmetric() {
        //INFO
        String cipherName, sessionKey, keyName;
        System.out.println("Crypt Symmetric:");
        File decryptedInfoFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedInfo.txt");
        try {
            BufferedReader infoReader = new BufferedReader(new FileReader(decryptedInfoFile));
            String info = infoReader.readLine();
            sessionKey = infoReader.readLine();
            infoReader.close();

            keyName = info.split("#")[1];
            cipherName = info.split("#")[2];
            SecretKey symmetricKeyOfFile = CryptController.convertSymmetricKeyFromB64(sessionKey, keyName);
            File choosenFileForEncryption = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "message.txt");
            File encryptedFile = new File(Main.communicationPath + File.separator + "user_" + messageReciever + File.separator + "Inbox" + File.separator + "encryptedMessage.txt");
            CryptController.cryptFileSymmetric(choosenFileForEncryption, encryptedFile, symmetricKeyOfFile, cipherName);
            choosenFileForEncryption.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void decryptFileSymmetric() {
        String cipherName, sessionKey, keyName, linija = "";
        File decryptedInfoFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedInfo.txt");
        try {
            BufferedReader infoReader = new BufferedReader(new FileReader(decryptedInfoFile));
            String info = infoReader.readLine();
            sessionKey = infoReader.readLine();
            infoReader.close();

            keyName = info.split("#")[1];
            cipherName = info.split("#")[2];
            SecretKey symmetricKeyOfFile = CryptController.convertSymmetricKeyFromB64(sessionKey, keyName);

            File encryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "Inbox" + File.separator + "encryptedMessage.txt");
            File decryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedMessage.txt");
            CryptController.decryptFileSymmetric(encryptedFile, decryptedFile, symmetricKeyOfFile, cipherName);
            encryptedFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void crypt() {
        try {
            //HEADER
            String symmetricKey = "DESede";
            String aesCipherName = "AES/CBC/PKCS5Padding";
            String desCipherName = "DESede/CBC/PKCS5Padding";
            String hashAlgorithmForSign = "SHA512withRSA";
            String info = user.getUsername() + "#" + symmetricKey + "#" + desCipherName + "#" + hashAlgorithmForSign;

            //KRIPTOVANJE FAJLA SA SIMETRICNIM KLJUCEM
            PrivateKey loggedUserPrivateKey = getLoggedUserPrivateKey();//za potpis
            File choosenFileForEncryption = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedInfo.txt");
            SecretKey symmetricSessionKey = CryptController.genSymetricKey(symmetricKey);
            SecretKey secret = new SecretKeySpec(Main.aesKey, "AES");
            String encodedSessionKey = Base64.getEncoder().encodeToString(symmetricSessionKey.getEncoded());
            File encryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "encryptedInfo.txt");

            //session key kod posiljaoca
            PrintWriter writeSessionKey = new PrintWriter(new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "sessionKey.txt"));
            writeSessionKey.println(encodedSessionKey);
            writeSessionKey.flush();
            writeSessionKey.close();

            //info o komunikaciji!
            PrintWriter writer = new PrintWriter(choosenFileForEncryption);
            writer.println(info);
            writer.println(encodedSessionKey);
            writer.flush();
            writer.close();

            long pocetak = System.currentTimeMillis();
            CryptController.cryptFileSymmetric(choosenFileForEncryption, encryptedFile, secret, aesCipherName);

            //DIGITALNI POTPIS
            File certificateFile = new File(getPathOfRecieverCertificate(messageReciever)); //asimetricno kriptovanje sa javnim kljucem primaoca
            PublicKey receiverPublicKey = CryptController.getCertificate(certificateFile).getPublicKey();

            String sign = CryptController.sign(choosenFileForEncryption, loggedUserPrivateKey, hashAlgorithmForSign);
            File signature = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "signatureFile.txt");
            signature.createNewFile();
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(signature)));
            pw.println(sign); //upis potpisa prijavljenog korisnika u signatureFile.txt
            pw.flush();
            pw.close();

            //KRIPTOVANJE SIMETRICNIH KLJUCEVA ASIMETRICNIM - DIGITALNA ENVELOPA
            sendSymmetricKey(receiverPublicKey, messageReciever);
            System.out.println("Trajanje asimetricne enkripcije :" + (double) (System.currentTimeMillis() - pocetak) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
            Main.showAlert(e.getMessage(), true);
        }
        user.setConnected(true);
    }

    public void decrypt() {
        long pocetak = System.currentTimeMillis();
        File symmetricInfoFile;
        File decryptedSymmetricInfoFile;
        try {
            /*Decrypt file*/
            symmetricInfoFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "Inbox" + File.separator + "encryptedInfoAsymmetric.txt");
            decryptedSymmetricInfoFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "encryptedInfo.txt");
            PrivateKey loggedUserPrivateKey = getLoggedUserPrivateKey();
            CryptController.decryptAsymmetric(loggedUserPrivateKey, CryptController.getFileInBytes(symmetricInfoFile), decryptedSymmetricInfoFile);

            //dekriptovanje aesom info fajl
            SecretKey secret = new SecretKeySpec(Main.aesKey, "AES");
            File encryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "encryptedInfo.txt");
            File decryptedFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "decryptedInfo.txt");
            CryptController.decryptFileSymmetric(encryptedFile, decryptedFile, secret, "AES/CBC/PKCS5Padding");

            /*HEADER INFO*/
            BufferedReader br = new BufferedReader(new FileReader(decryptedFile));
            String[] messageInfo = br.readLine().split("#");
            String sender = messageInfo[0];
            String symmetricKeyName = messageInfo[1];
            String cipherName = messageInfo[2];
            String hashAlgorithmForSign = messageInfo[3];
            /*HEADER INFO*/

            File sessionKeyFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "sessionKey.txt");

            /*provjera sertifikata pošiljaoca*/
            if (checkSenderCertificate(sender)) {
                /*simetricni kljucevi*/
                String encodedSymmetricKeyOfFile = br.readLine();
                br.close();
                //SecretKey symmetricSesionKey = CryptController.convertSymmetricKeyFromB64(encodedSymmetricKeyOfFile, symmetricKeyName);
                PrintWriter writer = new PrintWriter(sessionKeyFile);
                writer.println(encodedSymmetricKeyOfFile);
                writer.flush();
                writer.close();

                File signatureFile = new File(Main.communicationPath + File.separator + "user_" + messageReciever + File.separator + "tmp" + File.separator + "signatureFile.txt");
                //ovdje ide prave poruke            CryptController.decryptFileSymmetric(encryptedFile, decryptedFile, symmetricKeyOfFile, cipherName);

                String pathToRecieverCertificate = Main.resourcesPath + File.separator + "certificates" + File.separator + sender + ".pem";
                File certificateFile = new File(new File(pathToRecieverCertificate).getAbsolutePath());
                PublicKey senderPublicKey = CryptController.getCertificate(certificateFile).getPublicKey();

                /*verifikovanje potpisa, nakon sto znamo da je validan sertifikat posiljaoca*/
                if (CryptController.verifySignature(decryptedFile, senderPublicKey, signatureFile, hashAlgorithmForSign)) {
                    System.out.println("Verifikovano");

                    System.out.println("\nSadrzaj info fajla:");
                    String line;
                    BufferedReader read = new BufferedReader(new FileReader(decryptedFile));
                    while ((line = read.readLine()) != null) {
                        System.out.println(line);
                    }
                    read.close();
                } else {
                    Main.showAlert("Digitalni potpis nije verifikovan!", true);
                    return;
                }
            } else {
                Main.showAlert("Sertifikat " + "( " + messageReciever + " )" + " nije validan!", true);
                br.close();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Trajanje dekripcije :" + (double) (System.currentTimeMillis() - pocetak) / 1000);
        user.setConnected(true);
    }

    private String getPathOfRecieverCertificate(String reciever) {
        return Main.resourcesPath + File.separator + "certificates" + File.separator + reciever + ".pem";
    }

    private PrivateKey getLoggedUserPrivateKey() throws Exception {
        return CryptController.getPemPrivateKey(Main.resourcesPath + File.separator + "certificates" + File.separator + "private" + File.separator + user.getUsername() + ".key");
    }

    private void sendSymmetricKey(PublicKey receiverPublicKey, String reciever) {
        long pocetak = System.currentTimeMillis();
        try {
            File symmetricKeyFile = new File(Main.communicationPath + File.separator + "user_" + user.getUsername() + File.separator + "tmp" + File.separator + "encryptedInfo.txt");
            File symmetricKeyEncryptedAsymmetric = new File(Main.communicationPath + File.separator + "user_" + reciever + File.separator + "Inbox" + File.separator + "encryptedInfoAsymmetric.txt");
            CryptController.encryptAsymmetric(receiverPublicKey, CryptController.getFileInBytes(symmetricKeyFile), symmetricKeyEncryptedAsymmetric);
            //  symmetricKeyFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("sendSymmetricKey " + (double) (System.currentTimeMillis() - pocetak) / 1000);
    }

    private boolean checkReceiverCertificate(String username) {
        File crlFile = new File(Main.resourcesPath + File.separator + "certificates" + File.separator + "rootcrl.pem");
        X509CRL crl;
        FileInputStream fis;
        File f = new File(Main.resourcesPath + File.separator + "certificates" + File.separator + username + ".pem");
        X509Certificate receiverCertificate = CryptController.getCertificate(f);
        X509Certificate caCertificate = CryptController.getCertificate(new File(Main.resourcesPath + File.separator + "certificates" + File.separator + "rootca.pem"));
        X509CRLEntry revokedCertificate;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            fis = new FileInputStream(crlFile);
            crl = (X509CRL) cf.generateCRL(fis);
            BigInteger certificateSerialNumber;
            if (crl != null) {
                certificateSerialNumber = receiverCertificate.getSerialNumber();
                revokedCertificate = crl.getRevokedCertificate(certificateSerialNumber);
                if (revokedCertificate != null) {
                    Main.showAlert("Sertifikat primaoca (" + username + ") je povučen!", true);
                    return false;
                }
            }
        } catch (Exception e) {
        }

        PublicKey caPublicKey = caCertificate.getPublicKey();
        try {
            receiverCertificate.verify(caPublicKey);
            receiverCertificate.checkValidity();
            System.out.println("Sertifikat od primaoca (" + username + ") je validan");
        } catch (NoSuchAlgorithmException
                | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
            certificateNotValid("Sertifikat od primaoca (" + username + ") nije validan!");
            return false;
        } catch (CertificateExpiredException ex) {
            certificateNotValid("Sertifikat od primaoca (" + username + ") je istekao!");
            return false;
        } catch (CertificateNotYetValidException ex) {
            certificateNotValid("Sertifikat od primaoca (" + username + ") nije još važeći!");
            return false;
        } catch (CertificateException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean checkSenderCertificate(String username) {
        File crlFile = new File(Main.resourcesPath + File.separator + "certificates" + File.separator + "rootcrl.pem");
        X509CRL crl;
        FileInputStream fis;
        File f = new File(Main.resourcesPath + File.separator + "certificates" + File.separator + username + ".pem");
        X509Certificate senderCertificate = CryptController.getCertificate(f);
        X509Certificate caCertificate = CryptController.getCertificate(new File(Main.resourcesPath + File.separator + "certificates" + File.separator + "rootca.pem"));
        X509CRLEntry revokedCertificate;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            fis = new FileInputStream(crlFile);
            crl = (X509CRL) cf.generateCRL(fis);
            BigInteger certificateSerialNumber;
            if (crl != null) {
                certificateSerialNumber = senderCertificate.getSerialNumber();
                revokedCertificate = crl.getRevokedCertificate(certificateSerialNumber);
                if (revokedCertificate != null) {
                    // Main.showAlert("Sertifikat pošiljaoca (" + username + ") je povučen!", true);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        PublicKey caPublicKey = caCertificate.getPublicKey();
        try {
            senderCertificate.verify(caPublicKey);
            senderCertificate.checkValidity();
            System.out.println("Sertifikat od pošiljaoca (" + username + ") je validan");
        } catch (NoSuchAlgorithmException
                | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
            certificateNotValid("Sertifikat od pošiljaoca (" + username + ") nije validan!");
            return false;
        } catch (CertificateExpiredException ex) {
            certificateNotValid("Sertifikat od pošiljaoca (" + username + ") je istekao!");
            return false;
        } catch (CertificateNotYetValidException ex) {
            certificateNotValid("Sertifikat od pošiljaoca (" + username + ") nije još važeći!");
            return false;
        } catch (CertificateException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private void certificateNotValid(String errorInfo) {
        Main.showAlert(errorInfo, true);
    }

}