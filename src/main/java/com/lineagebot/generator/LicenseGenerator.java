package com.lineagebot.generator; // Обратите внимание на другой пакет!

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;

public class LicenseGenerator {
    // ЗАМЕНИТЕ ЭТОТ КЛЮЧ НА СВОЙ СЕКРЕТНЫЙ!
    private static final String PRIVATE_KEY = "123456";
    private static final int LICENSE_DAYS = 30;

    public static String generateLicenseKey() {
        try {
            long activationDate = new Date().getTime();
            long expirationDate = activationDate + (LICENSE_DAYS * 24 * 60 * 60 * 1000L);

            String signature = generateSignature(activationDate, expirationDate);
            String licenseData = signature + "|" + activationDate + "|" + expirationDate;

            return Base64.getEncoder().encodeToString(licenseData.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String generateSignature(long activationDate, long expirationDate) {
        try {
            String data = PRIVATE_KEY + activationDate + "|" + expirationDate;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 20);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Использование: java com.lineagebot.generator.LicenseGenerator [количество_ключей]");
            System.out.println("Пример: java com.lineagebot.generator.LicenseGenerator 5");
            return;
        }

        try {
            int count = Integer.parseInt(args[0]);
            System.out.println("Сгенерированные ключи лицензии:");
            System.out.println("=================================");

            for (int i = 0; i < count; i++) {
                String key = generateLicenseKey();
                System.out.println((i + 1) + ". " + key);

                // Небольшая задержка для разных временных меток
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (NumberFormatException e) {
            System.out.println("Ошибка: укажите число ключей");
        }
    }
}