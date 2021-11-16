package gui.controllers;

import gui.main.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignUpController {

    @FXML
    private Button cancel;
    @FXML
    private Button signUp;
    @FXML
    private TextField usernameLabel;
    @FXML
    private PasswordField password;
    @FXML
    private PasswordField passwordConfirm;
    @FXML
    private Label signInLabel;

    public SignUpController() {
    }

    @FXML
    public void cancelAction() {
        Stage stage = (Stage) cancel.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void signInAction() {
        System.out.println("login");
        try {
            signInLabel.getScene().getWindow().hide();
            Stage s = new Stage();
            s.setTitle("Login");
            s.getIcons().add(new Image("/resources/images/chat-icon.jpg"));
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(getClass().getResource("/resources/view/loginForm.fxml"));
            Scene scene = new Scene(root);
            s.setScene(scene);
            s.show();
            root.requestFocus();
            s.setResizable(false);
        } catch (Exception ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.WARNING, null, ex);
            ex.printStackTrace();
        }
    }

    @FXML
    public void signUpAction() throws NoSuchAlgorithmException {
        if (usernameLabel.getText().isEmpty() || password.getText().isEmpty() || passwordConfirm.getText().isEmpty()) {
            Main.showAlert("Empty fields", true);
            //  return;
        }
        if (!password.getText().equals(passwordConfirm.getText())) {
            Main.showAlert("Password doesn't match", true);
            password.setText("");
            passwordConfirm.setText("");
            //  return;
        }

        String username = usernameLabel.getText();
        String pass = password.getText();
        String passwordHash = LoginController.hash(pass,"");

        //unos u sistem
        try {
            PrintWriter br = new PrintWriter(new FileWriter(Main.usersFile, true));
            br.println(username + "#" + passwordHash);
            br.close();
            new File(Main.communicationPath + File.separator + "user_" + username + File.separator + "Inbox").mkdirs();
            new File(Main.communicationPath + File.separator + "user_" + username + File.separator + "tmp").mkdir();
            System.out.println("Kreiran je inbox za " + username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Main.showAlert("You have signed up successfully", false);

        try {
            signInLabel.getScene().getWindow().hide();
            Stage s = new Stage();
            s.setTitle("Login");
            s.getIcons().add(new Image("/resources/images/chat-icon.jpg"));
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(getClass().getResource("/resources/view/loginForm.fxml"));
            Scene scene = new Scene(root);
            s.setScene(scene);
            s.show();
            root.requestFocus();
            s.setResizable(false);
        } catch (Exception ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.WARNING, null, ex);
            ex.printStackTrace();
        }
    }
}
