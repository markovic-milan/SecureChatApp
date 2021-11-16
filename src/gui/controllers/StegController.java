package gui.controllers;

import gui.main.Main;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import model.SteganographyModel;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class StegController {
    private SteganographyModel model;
    File file = new File("C:\\Users\\milan\\Desktop\\KriptografijaProjektni\\src\\resources\\images\\person.png");
    public Image image = new Image(file.toURI().toString());
    public Image embedded;

    public StegController(SteganographyModel model) {
        this.model = model;
    }

    public void onEncode(String message, String user) {
        //System.out.println(" visina slike je " + image.getHeight() + " duzina je " + image.getWidth());
        embedded = model.encode(image, message);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(embedded, null), "png", new File(
                    Main.communicationPath + "\\user_" + user + "\\Inbox\\person.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String onDecode(Image imageF) {
        return model.decode(imageF);
    }
}
