package com.lineagebot;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class ArduinoInterface {
    private SerialPort serialPort;
    private final String portName;
    private final Consumer<String> logCallback;
    private static final int MAX_REOPEN_ATTEMPTS = 3;
    private static final long REOPEN_DELAY_MS = 1000;
    private static final long OPEN_PORT_DELAY_MS = 500;

    public ArduinoInterface(String portName, Consumer<String> logCallback) {
        this.portName = portName;
        this.logCallback = logCallback;
        openPort();
    }

    private boolean openPort() {
        String[] availablePorts = Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
        log("Доступные порты: " + String.join(", ", availablePorts));
        if (!Arrays.asList(availablePorts).contains(portName)) {
            log("Порт " + portName + " не найден среди доступных");
            return false;
        }

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
        if (serialPort.openPort()) {
            log("Порт " + portName + " открыт");
            try {
                Thread.sleep(OPEN_PORT_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        } else {
            log("Не удалось открыть порт: " + portName);
            return false;
        }
    }

    public boolean sendCommand(String command) {
        int attempts = 0;
        while (attempts < MAX_REOPEN_ATTEMPTS) {
            try {
                if (!serialPort.isOpen()) {
                    log("Порт " + portName + " закрыт, попытка переоткрытия " + (attempts + 1) + "/" + MAX_REOPEN_ATTEMPTS);
                    if (!openPort()) {
                        attempts++;
                        if (attempts < MAX_REOPEN_ATTEMPTS) {
                            try {
                                Thread.sleep(REOPEN_DELAY_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return false;
                            }
                            continue;
                        } else {
                            log("Не удалось переоткрыть порт " + portName + " после " + MAX_REOPEN_ATTEMPTS + " попыток");
                            return false;
                        }
                    }
                }
                log("Отправлена команда: " + command);
                serialPort.getOutputStream().write((command + "\n").getBytes("UTF-8"));
                serialPort.getOutputStream().flush();
                byte[] buffer = new byte[1024];
                int bytesRead = serialPort.getInputStream().read(buffer, 0, buffer.length);
                String response = new String(buffer, 0, bytesRead, "UTF-8").trim();
                log("Ответ от Arduino: " + response);
                return response.contains("OK");
            } catch (IOException e) {
                log("Ошибка отправки команды: " + e.getMessage());
                if (!serialPort.isOpen()) {
                    attempts++;
                    continue;
                }
                return false;
            }
        }
        return false;
    }

    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            log("Порт " + portName + " закрыт");
        }
    }

    public boolean isPortOpen() {
        return serialPort != null && serialPort.isOpen();
    }

    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        } else {
            System.out.println(message);
        }
    }
}