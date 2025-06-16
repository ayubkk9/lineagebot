package com.lineagebot;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class ScreenReader {
    private final Robot robot;
    private final Java2DFrameConverter converter;
    private final OpenCVFrameConverter.ToMat matConverter;
    private Mat cachedMat;

    static {
        try {
            Loader.load(org.bytedeco.opencv.global.opencv_core.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить OpenCV: " + e.getMessage());
        }
    }

    public ScreenReader() {
        try {
            robot = new Robot();
            converter = new Java2DFrameConverter();
            matConverter = new OpenCVFrameConverter.ToMat();
        } catch (AWTException e) {
            throw new RuntimeException("Не удалось инициализировать Robot: " + e.getMessage());
        }
    }

    public double readBarLevel(int x, int y, int width, int height) throws Exception {
        BufferedImage image = robot.createScreenCapture(new Rectangle(x, y, width, height));
        org.bytedeco.javacv.Frame frame = converter.convert(image);
        if (frame == null) {
            throw new Exception("Не удалось конвертировать изображение в Frame");
        }
        if (cachedMat == null || cachedMat.rows() != height || cachedMat.cols() != width) {
            cachedMat = new Mat(height, width, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
        }
        Mat mat = matConverter.convert(frame);
        opencv_imgproc.cvtColor(mat, cachedMat, opencv_imgproc.COLOR_BGR2GRAY);
        opencv_imgproc.threshold(cachedMat, cachedMat, 100, 255, opencv_imgproc.THRESH_BINARY);

        int filledPixels = 0;
        int totalPixels = width * height;
        byte[] pixels = new byte[totalPixels];
        cachedMat.data().get(pixels);
        for (byte pixel : pixels) {
            if (pixel == -1) { // Белый пиксель (255 в unsigned byte)
                filledPixels++;
            }
        }
        return (double) filledPixels / totalPixels;
    }
}