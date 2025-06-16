package com.lineagebot;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ScreenReader {
    private Robot robot;
    private Java2DFrameConverter converter;
    private OpenCVFrameConverter.ToMat matConverter;

//    static {
//        System.loadLibrary(org.bytedeco.opencv.global.opencv_core.NATIVE_LIBRARY_NAME);
//    }

    public ScreenReader() {
        try {
            robot = new Robot();
            converter = new Java2DFrameConverter();
            matConverter = new OpenCVFrameConverter.ToMat();
        } catch (AWTException e) {
            throw new RuntimeException("Не удалось инициализировать Robot: " + e.getMessage());
        }
    }

    public double readBarLevel(int x, int y, int width, int height) {
        try {
            // Захват области полосы
            BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));
            org.bytedeco.javacv.Frame frame = converter.convert(image);
            Mat mat = matConverter.convert(frame);
            opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.threshold(mat, mat, 100, 255, opencv_imgproc.THRESH_BINARY);

            // Подсчёт заполненных пикселей
            int filledPixels = 0;
            int totalPixels = width * height;
            byte[] pixels = new byte[totalPixels];
            mat.data().get(pixels);
            for (byte pixel : pixels) {
                if (pixel == -1) { // Белый пиксель (255 в unsigned byte)
                    filledPixels++;
                }
            }
            return (double) filledPixels / totalPixels; // Доля заполненной полосы (0.0–1.0)
        } catch (Exception e) {
            System.err.println("Ошибка чтения полосы: " + e.getMessage());
            return -1.0;
        }
    }

    public String readMobOwner(int x, int y, int width, int height) {
        return ""; // Заглушка для анти-KS
    }

    public List<Rect> findMobLocations() {
        List<Rect> mobLocations = new ArrayList<>();
        mobLocations.add(new Rect(200, 100, 50, 50)); // Заглушка
        return mobLocations;
    }
}