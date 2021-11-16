package model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.util.Pair;

import java.util.stream.IntStream;

public class SteganographyModel {

    public String username;

    public SteganographyModel(String username) {
        this.username = username;
    }

    public Image encode(Image image, String message) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        WritableImage copy = new WritableImage(image.getPixelReader(), width, height);
        PixelWriter writer = copy.getPixelWriter();
        PixelReader reader = copy.getPixelReader();

        boolean[] bits = messageToBits(message);
        IntStream.range(0, bits.length)
                .mapToObj(i -> new Pair<>(i, reader.getArgb(i % width, i / width)))
                .map(pair -> new Pair<>(pair.getKey(), bits[pair.getKey()] ? pair.getValue() | 1 : pair.getValue() & ~1))
                .forEach(pair -> {
                    int x = pair.getKey() % width;
                    int y = pair.getKey() / width;

                    writer.setArgb(x, y, pair.getValue());
                });
        return copy;
    }

    private boolean[] messageToBits(String message) {
        byte[] data = message.getBytes();

        boolean[] bits = new boolean[32 + data.length * 8];

        String binary = Integer.toBinaryString(data.length);

        while (binary.length() < 32) {
            binary = "0" + binary;
        }

        for (int i = 0; i < 32; i++) {
            bits[i] = binary.charAt(i) == '1';
        }
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];

            for (int j = 0; j < 8; j++) {
                bits[32 + i * 8 + j] = ((b >> (7 - j)) & 1) == 1;
            }
        }
        return bits;
    }

    public String decode(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();


        System.out.println(width + "            " + height);
        PixelReader reader = image.getPixelReader();

        boolean[] bits = new boolean[width * height];

        IntStream.range(0, width * height)
                .mapToObj(i -> new Pair<>(i, reader.getArgb(i % width, i / width)))
                .forEach(pair -> {
                    String binary = Integer.toBinaryString(pair.getValue());
                    bits[pair.getKey()] = binary.charAt(binary.length() - 1) == '1';
                });

        // decode length
        int length = 0;
        for (int i = 0; i < 32; i++) {
            if (bits[i]) {
                length |= (1 << (31 - i));
            }
        }

        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < 8; j++) {
                if (bits[32 + i * 8 + j]) {
                    data[i] |= (1 << (7 - j));
                }
            }
        }

        return new String(data);
    }
}
