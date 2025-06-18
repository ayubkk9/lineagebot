package com.lineagebot;

import com.fazecast.jSerialComm.SerialPort;
import java.util.function.Consumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ArduinoInterface {
    private SerialPort serialPort;
    private final String portName;
    private final Consumer<String> logger;
    private volatile boolean isOpen = false;
    private static final int READ_TIMEOUT_MS = 1000; // Таймаут чтения ответа
    private static final int BAUD_RATE = 9600; // Скорость передачи данных

    public ArduinoInterface(String portName, Consumer<String> logger) {
        this.portName = portName;
        this.logger = logger;
        openPort();
    }

    private void openPort() {
        try {
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(BAUD_RATE);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);

            if (serialPort.openPort()) {
                isOpen = true;
                logger.accept("✅ Порт " + portName + " успешно открыт");
                // Даём Arduino время на инициализацию
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                isOpen = false;
                logger.accept("❌ Не удалось открыть порт " + portName);
            }
        } catch (Exception e) {
            isOpen = false;
            logger.accept("❌ Ошибка открытия порта " + portName + ": " + e.getMessage());
        }
    }

    public void sendCommand(String command) {
        if (!isOpen || serialPort == null) {
            logger.accept("❌ Порт не открыт, команда '" + command + "' не отправлена");
            return;
        }

        // Проверяем, что команда не является текстовым вводом через чат
        if (command.startsWith("CHAT:")) {
            logger.accept("⚠️ Команды CHAT: не поддерживаются");
            return;
        }

        try {
            OutputStream output = serialPort.getOutputStream();
            output.write((command + "\n").getBytes());
            output.flush();
            logger.accept("📤 Отправлена команда: " + command);

            // Читаем ответ от Arduino с таймаутом
            InputStream input = serialPort.getInputStream();
            StringBuilder response = new StringBuilder();
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < READ_TIMEOUT_MS) {
                if (input.available() > 0) {
                    int data = input.read();
                    if (data == -1) break; // Конец потока
                    char c = (char) data;
                    response.append(c);
                    if (c == '\n') break; // Завершение ответа
                }
                Thread.sleep(10);
            }

            if (response.length() > 0) {
                logger.accept("📥 Ответ от Arduino: " + response.toString().trim());
            } else {
                logger.accept("⚠️ Ответ от Arduino не получен");
            }

        } catch (IOException e) {
            logger.accept("❌ Ошибка отправки команды '" + command + "': " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.accept("❌ Прервано ожидание ответа для команды '" + command + "'");
        }
    }

    public boolean isPortOpen() {
        return isOpen && serialPort != null && serialPort.isOpen();
    }

    public void close() {
        if (serialPort != null && isOpen) {
            try {
                serialPort.closePort();
                isOpen = false;
                logger.accept("✅ Порт " + portName + " закрыт");
            } catch (Exception e) {
                logger.accept("❌ Ошибка закрытия порта " + portName + ": " + e.getMessage());
            }
        }
    }
}