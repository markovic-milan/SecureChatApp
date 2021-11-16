package gui.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import model.User;

import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {
    public static String communicationPath = "C:\\Users\\milan\\Desktop\\KriptografijaProjektni\\src\\communication";
    public static String usersFile = "C:\\Users\\milan\\Desktop\\KriptografijaProjektni\\src\\data\\users.txt";
    public static String onlineUsersFile = "C:\\Users\\milan\\Desktop\\KriptografijaProjektni\\src\\data\\onlineUsers.txt";
    public static String resourcesPath = "C:\\Users\\milan\\Desktop\\KriptografijaProjektni\\src\\resources";
    public static User user;
    public static List<String> onlineUsers = new ArrayList<>();
    public static String key = "MilanMarkovicProjektniKRZ";
    public static SecretKeySpec KS;
    public static byte[] aesKey = new byte[]{0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    @Override
    public void start(Stage primaryStage) throws Exception {
        File dir = new File(communicationPath);
        deleteFiles(dir);

        byte[] KeyData = key.getBytes();
        KS = new SecretKeySpec(KeyData, "Blowfish");

        Parent root = FXMLLoader.load(getClass().getResource("/resources/view/loginForm.fxml"));
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root, 400, 350));
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/images/chat-icon.jpg")));
        primaryStage.show();
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void showAlert(String message, boolean error) {
        Alert alert = null;
        if (error) {
            alert = new Alert(Alert.AlertType.ERROR);
        } else {
            alert = new Alert(Alert.AlertType.INFORMATION);
        }
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void deleteFiles(File dirPath) {
        File files[] = null;
        if (dirPath.isDirectory()) {
            files = dirPath.listFiles();
            for (File dirFiles : files) {
                if (dirFiles.isDirectory()) {
                    deleteFiles(dirFiles);
                } else {
                    if (dirFiles.getName().endsWith(".txt")) {
                        System.out.println("Brisanje fajla " + dirFiles.getName());
                        dirFiles.delete();
                    }
                }
            }
        } else {
            if (dirPath.getName().endsWith(".txt")) {
                System.out.println("Brisanje fajla " + dirPath.getName());
                dirPath.delete();
            }
        }
    }
}
