package com.lineagebot;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;

public class LicenseManager {
    private static final String LICENSE_FILE = "license.dat";
    private static final String USED_KEYS_FILE = "used_keys.dat";
    private static final String PUBLIC_KEY = "123456";

    private String licenseKey;
    private Date activationDate;
    private Date expirationDate;
    private boolean isValid;
    private Set<String> usedKeys;

    public LicenseManager() {
        loadUsedKeys();
        loadLicense();
    }

    private void loadUsedKeys() {
        usedKeys = new HashSet<>();
        try {
            if (Files.exists(Paths.get(USED_KEYS_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(USED_KEYS_FILE)));
                String[] keys = content.split("\n");
                for (String key : keys) {
                    if (!key.trim().isEmpty()) {
                        usedKeys.add(key.trim());
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки при загрузке used keys
        }
    }

    private void saveUsedKeys() {
        try {
            StringBuilder content = new StringBuilder();
            for (String key : usedKeys) {
                content.append(key).append("\n");
            }
            Files.write(Paths.get(USED_KEYS_FILE), content.toString().getBytes());
        } catch (Exception e) {
            // Игнорируем ошибки при сохранении used keys
        }
    }

    private void addUsedKey(String key) {
        usedKeys.add(key);
        saveUsedKeys();
    }

    public boolean isKeyUsed(String key) {
        return usedKeys.contains(key);
    }

    public boolean isValid() {
        return isValid && new Date().before(expirationDate);
    }

    public int getDaysRemaining() {
        if (!isValid) return 0;
        long diff = expirationDate.getTime() - new Date().getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    private void loadLicense() {
        try {
            if (Files.exists(Paths.get(LICENSE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(LICENSE_FILE)));
                JSONObject json = new JSONObject(content);

                licenseKey = json.getString("key");
                activationDate = new Date(json.getLong("activationDate"));
                expirationDate = new Date(json.getLong("expirationDate"));

                isValid = validateLicense(licenseKey);
            }
        } catch (Exception e) {
            isValid = false;
        }
    }

    public boolean activateLicense(String key) {
        try {
            // Проверяем, не использовался ли уже этот ключ
            if (isKeyUsed(key)) {
                System.out.println("Ключ уже использовался: " + key);
                return false;
            }

            if (!validateLicense(key)) {
                return false;
            }

            // Декодируем для получения дат
            String decoded = new String(Base64.getDecoder().decode(key));
            String[] parts = decoded.split("\\|");

            long actDate = Long.parseLong(parts[1]);
            long expDate = Long.parseLong(parts[2]);

            // Сохраняем лицензию
            JSONObject json = new JSONObject();
            json.put("key", key);
            json.put("activationDate", actDate);
            json.put("expirationDate", expDate);

            Files.write(Paths.get(LICENSE_FILE), json.toString().getBytes());

            // Добавляем ключ в использованные
            addUsedKey(key);

            // Обновляем состояние
            licenseKey = key;
            activationDate = new Date(actDate);
            expirationDate = new Date(expDate);
            isValid = true;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateLicense(String key) {
        try {
            String decoded = new String(Base64.getDecoder().decode(key));
            String[] parts = decoded.split("\\|");

            if (parts.length != 3) return false;

            String signature = parts[0];
            long actDate = Long.parseLong(parts[1]);
            long expDate = Long.parseLong(parts[2]);

            // Проверяем подпись с помощью PUBLIC_KEY
            String expectedSignature = generateSignature(actDate, expDate);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateSignature(long activationDate, long expirationDate) {
        try {
            String data = PUBLIC_KEY + activationDate + "|" + expirationDate;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 20);
        } catch (Exception e) {
            return "";
        }
    }

    public boolean deactivate() {
        try {
            Files.deleteIfExists(Paths.get(LICENSE_FILE));
            isValid = false;
            licenseKey = null;
            activationDate = null;
            expirationDate = null;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Метод для полной очистки (только для админа)
    public boolean clearUsedKeys() {
        try {
            Files.deleteIfExists(Paths.get(USED_KEYS_FILE));
            usedKeys.clear();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getLicenseInfo() {
        if (!isValid) return "Лицензия не активирована";

        return String.format("Лицензия активна. Осталось дней: %d. Истекает: %s",
                getDaysRemaining(), getFormattedExpirationDate());
    }

    public Date getActivationDate() {
        return activationDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public String getFormattedActivationDate() {
        if (activationDate == null) return "Не активирована";
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(activationDate);
    }

    public String getFormattedExpirationDate() {
        if (expirationDate == null) return "Не активирована";
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(expirationDate);
    }

    public int getUsedKeysCount() {
        return usedKeys.size();
    }
}