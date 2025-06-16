package com.lineagebot;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.Arrays;

public class ArduinoInterface {
    private SerialPort serialPort;
    private final String portName;
    private static final int MAX_REOPEN_ATTEMPTS = 3;
    private static final long REOPEN_DELAY_MS = 2000;

    public ArduinoInterface(String portName) {
        this.portName = portName;
        openPort();
    }

    private boolean openPort() {
        // Проверяем доступные порты
        String[] availablePorts = Arrays.stream(SerialPort.getCommPorts())
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
        System.out.println("Доступные порты: " + String.join(", ", availablePorts));
        if (!Arrays.asList(availablePorts).contains(portName)) {
            System.err.println("Порт " + portName + " не найден среди доступных");
            return false;
        }

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 0);
        if (serialPort.openPort()) {
            System.out.println("Порт " + portName + " открыт");
            try {
                Thread.sleep(2000); // Задержка для инициализации
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        } else {
            System.err.println("Не удалось открыть порт: " + portName);
            return false;
        }
    }

    public void sendCommand(String command) {
        int attempts = 0;
        while (attempts < MAX_REOPEN_ATTEMPTS) {
            try {
                if (!serialPort.isOpen()) {
                    System.out.println("Порт " + portName + " закрыт, попытка переоткрытия " + (attempts + 1) + "/" + MAX_REOPEN_ATTEMPTS);
                    if (!openPort()) {
                        attempts++;
                        if (attempts < MAX_REOPEN_ATTEMPTS) {
                            try {
                                Thread.sleep(REOPEN_DELAY_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            continue;
                        } else {
                            System.err.println("Не удалось переоткрыть порт " + portName + " после " + MAX_REOPEN_ATTEMPTS + " попыток");
                            return;
                        }
                    }
                }
                System.out.println("Отправлена команда: " + command);
                serialPort.getOutputStream().write((command + "\n").getBytes("UTF-8"));
                serialPort.getOutputStream().flush();
                Thread.sleep(50); // Задержка между командами
                return;
            } catch (IOException | InterruptedException e) {
                System.err.println("Ошибка отправки команды: " + e.getMessage());
                if (!serialPort.isOpen()) {
                    attempts++;
                    continue;
                }
                return;
            }
        }
    }

    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            System.out.println("Порт " + portName + " закрыт");
        }
    }

    public boolean isPortOpen() {
        return serialPort != null && serialPort.isOpen();
    }
}