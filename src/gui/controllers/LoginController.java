package gui.controllers;

import gui.main.Main;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoginController {
    @FXML
    private Button cancelButton;
    @FXML
    public TextField usernameTF;
    @FXML
    private PasswordField password;
    @FXML
    private Label signUpLabel;

    //  public String username;

    private User user = Main.user;

    public LoginController() {
    }

    @FXML
    public void cancelAction() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void signUpAction() {
        try {
            signUpLabel.getScene().getWindow().hide();
            Stage s = new Stage();
            s.setTitle("SignUp");
            s.getIcons().add(new Image("/resources/images/chat-icon.jpg"));
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(getClass().getResource("/resources/view/signUpForm.fxml"));
            Scene scene = new Scene(root);
            s.setScene(scene);
            s.show();
            root.requestFocus();
            s.setResizable(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void loginAction(ActionEvent e) {
        if (usernameTF.getText().isEmpty() || password.getText().isEmpty()) {
            Main.showAlert("Empty fields", true);
            return;
        }
        if (checkAuthentication(usernameTF.getText(), password.getText())) {
            System.out.println("Passed authentication");

            Main.user = new User(usernameTF.getText());
            Main.user.setOnline(true);
            //prvo ucitaj sve online korisnike
            try {
                Main.onlineUsers = Files.lines(Paths.get(Main.onlineUsersFile)).filter(line -> !(line.contains(Main.user.getUsername())) && !(line.startsWith("="))).collect(Collectors.toList());
                for (String online : Main.onlineUsers) {
                    System.out.println("Online je: " + online);
                }
                //upis korisnika u online korisnike

                PrintWriter writer = new PrintWriter(new FileWriter(Main.onlineUsersFile, true));
                System.out.println("Prijavljen kao " + Main.user.getUsername());
                Main.onlineUsers.add(Main.user.getUsername());
                writer.println(Main.user.getUsername());
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            //prikaz glavne forme
            try {
                cancelButton.getScene().getWindow().hide();

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/view/myChatForm.fxml"));
                Parent root = loader.load();
                MyChatController chatController = loader.getController();
                chatController.readOnlineUsers();

                Stage stage = new Stage();
                stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);
                stage.setTitle("MyChat");
                stage.getIcons().add(new Image("/resources/images/chat-icon.jpg"));
                Scene scene = new Scene(root);

                VBox userVBox = (VBox) scene.lookup("#liveUsersBox");
                TextArea textArea = (TextArea) scene.lookup("#textSpace");
                Button disconnectButton = (Button) scene.lookup("#disconnectButton");
                chatController.showChanges(userVBox, Main.user.getUsername(), textArea, disconnectButton);

                stage.setScene(scene);
                stage.show();
                root.requestFocus();
                stage.setResizable(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            Main.showAlert("Username or password are incorrect!", true);
            return;
        }
    }

    private boolean checkAuthentication(String username, String password) {
        try {
            String passwordHash;
            BufferedReader br = new BufferedReader(new FileReader(Main.usersFile));
            String line = br.readLine();
            while (line != null) {
                if (line.split("#")[1].length() == 32) {
                    passwordHash = hash(password, "MD5");
                } else if (line.split("#")[1].length() == 40) {
                    passwordHash = hash(password, "SHA-1");
                } else {
                    passwordHash = hash(password, "SHA-256");
                }
                if (line.split("#")[0].equals(username) && line.split("#")[1].equals(passwordHash)) {
                    br.close();
                    return true;
                } else {
                    line = br.readLine();
                }
            }
            br.close();
            return false;
        } catch (Exception ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.WARNING, null, ex);
            ex.printStackTrace();
        }
        return false;
    }

    public static String hash(String value, String alg) throws NoSuchAlgorithmException {
        //md5 -32byte
        //sha1 - 20byte
        //sha512 -64byte
        if ("".equals(alg) || alg == null) {
            Random rand = new Random();
            int slucaj = rand.nextInt(3);
            System.out.println(slucaj);
            if (slucaj == 1) {
                alg = "SHA-1";
            } else if (slucaj == 2) {
                alg = "SHA-256";
            } else {
                alg = "MD5";
            }
        }
        MessageDigest digest = MessageDigest.getInstance(alg);
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void closeWindowEvent(WindowEvent event) {
        Main.user.setOnline(false);
        System.out.println("User " + Main.user.getUsername() + " is going offline ...");
        //uklonimo liniju iz fajla
        try {
            File file = new File(Main.onlineUsersFile);
            List<String> lines = Files.lines(file.toPath()).filter(line -> !(line.contains(usernameTF.getText()) && !line.startsWith("="))).collect(Collectors.toList());
            PrintWriter pw = new PrintWriter(file);
            for (String l : lines) {
                pw.write(l + "\n");
            }
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //odjavimo korisnika
        if (Main.user.isConnected())
            Main.user.setConnected(false);
        System.out.println(Main.user.getUsername() + " is offline");
        Main.onlineUsers.remove(Main.user.getUsername());
        Main.deleteFiles(new File(Main.communicationPath + File.separator + "user_" + Main.user.getUsername()));
    }
}