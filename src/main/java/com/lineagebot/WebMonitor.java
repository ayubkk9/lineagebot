package com.lineagebot;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class WebMonitor {
    private BotController botController;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread serverThread;
    private int port;

    public WebMonitor() {
        this.botController = null;
        this.port = 8080;
        startWebServer(8080);
    }

    public WebMonitor(BotController botController) {
        this.botController = botController;
        this.port = 8080;
        startWebServer(8080);
    }

    public void startWebServer(int i) {
        if (isRunning) {
            System.out.println("Web server is already running");
            return;
        }

        try {
            // Пытаемся использовать порт 8080, если занят - ищем свободный
            int tryPort = port;
            int maxAttempts = 10;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                try {
                    serverSocket = new ServerSocket(tryPort);
                    this.port = tryPort;
                    System.out.println("Web monitor started on port " + tryPort);
                    break;
                } catch (IOException e) {
                    if (attempt == maxAttempts - 1) {
                        System.out.println("Failed to find free port after " + maxAttempts + " attempts");
                        return;
                    }
                    tryPort++; // Пробуем следующий порт
                }
            }

            serverSocket.setSoTimeout(1000);
            isRunning = true;

            serverThread = new Thread(() -> {
                System.out.println("Web server thread started on port " + port);
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        if (isRunning) {
                            System.out.println("Web server error: " + e.getMessage());
                            break;
                        }
                    }
                }
                System.out.println("Web server thread stopped");
            });
            serverThread.setDaemon(true);
            serverThread.start();

        } catch (IOException e) {
            System.out.println("Failed to start web server: " + e.getMessage());
        }
    }

    public void stopWebServer() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Web server socket closed");
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }

        // Ждем завершения потока
        if (serverThread != null && serverThread.isAlive()) {
            try {
                serverThread.join(2000);
                System.out.println("Web server thread stopped successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for server thread");
            }
        }
        System.out.println("Web monitor stopped");
    }

    public int getPort() {
        return port;
    }

    // Метод для обновления контроллера бота
    public void setBotController(BotController botController) {
        this.botController = botController;
        System.out.println("WebMonitor: BotController updated");
    }

    // Метод для очистки контроллера бота
    public void clearBotController() {
        this.botController = null;
        System.out.println("WebMonitor: BotController cleared");
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket; BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) return;

            String method = requestParts[0];
            String path = requestParts[1];

            // Читаем заголовки
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Пропускаем заголовки
            }

            if ("GET".equals(method)) {
                if ("/stats".equals(path)) {
                    sendJsonResponse(out, getStatsJson());
                } else {
                    sendHtmlResponse(out, getHtmlPage());
                }
            } else if ("OPTIONS".equals(method)) {
                sendOptionsResponse(out);
            }

        } catch (IOException e) {
            System.out.println("Client handling error: " + e.getMessage());
        }
    }

    private String getStatsJson() {
        try {
            if (botController == null) {
                return "{\"status\":\"STOPPED\",\"mobsKilled\":0,\"botActive\":false}";
            }

            BotStats stats = botController.getBotStats();
            if (stats == null) {
                return "{\"status\":\"STOPPED\",\"mobsKilled\":0,\"botActive\":false}";
            }

            boolean isBotActive = botController.isBotRunning();

            return String.format(
                    "{\"status\":\"%s\",\"mobsKilled\":%d,\"botActive\":%b}",
                    stats.getStatus(),
                    stats.getMobsKilled(),
                    isBotActive
            );

        } catch (Exception e) {
            return "{\"status\":\"ERROR\",\"mobsKilled\":0,\"botActive\":false}";
        }
    }

    private void sendJsonResponse(PrintWriter out, String json) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json");
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Connection: close");
        out.println();
        out.println(json);
        out.flush();
    }

    private void sendHtmlResponse(PrintWriter out, String html) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println("Connection: close");
        out.println();
        out.println(html);
    }

    private void sendOptionsResponse(PrintWriter out) {
        out.println("HTTP/1.1 200 OK");
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Connection: close");
        out.println();
    }

    private String getHtmlPage() {
        String html = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Lineage II Bot Monitor</title>" +
                "    <style>" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body { font-family: Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; }" +
                "        .dashboard { background: rgba(255, 255, 255, 0.15); backdrop-filter: blur(20px); border-radius: 25px; padding: 40px; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3); border: 1px solid rgba(255, 255, 255, 0.2); max-width: 500px; width: 100%; }" +
                "        .title { text-align: center; color: white; font-size: 32px; font-weight: bold; margin-bottom: 40px; text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5); }" +
                "        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }" +
                "        .stat-card { background: rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 30px; text-align: center; border: 1px solid rgba(255, 255, 255, 0.1); transition: all 0.3s ease; }" +
                "        .stat-card:hover { transform: translateY(-5px); background: rgba(255, 255, 255, 0.15); }" +
                "        .stat-label { color: rgba(255, 255, 255, 0.8); font-size: 18px; margin-bottom: 15px; font-weight: 500; }" +
                "        .stat-value { color: white; font-size: 42px; font-weight: bold; text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.6); }" +
                "        .status-running { color: #4CAF50 !important; }" +
                "        .status-stopped { color: #F44336 !important; }" +
                "        .bot-active { color: #4CAF50 !important; }" +
                "        .bot-inactive { color: #FF9800 !important; }" +
                "        .last-update { text-align: center; color: rgba(255, 255, 255, 0.6); font-size: 14px; margin-top: 30px; }" +
                "        @media (max-width: 600px) { .grid { grid-template-columns: 1fr; gap: 20px; } .dashboard { padding: 30px 20px; } .title { font-size: 28px; } }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='dashboard'>" +
                "        <div class='title'>🤖 Lineage II Bot</div>" +
                "        <div class='grid'>" +
                "            <div class='stat-card'>" +
                "                <div class='stat-label'>СТАТУС</div>" +
                "                <div class='stat-value' id='status'>—</div>" +
                "            </div>" +
                "            <div class='stat-card'>" +
                "                <div class='stat-label'>УБИТО МОБОВ</div>" +
                "                <div class='stat-value' id='mobsKilled'>0</div>" +
                "            </div>" +
                "            <div class='stat-card'>" +
                "                <div class='stat-label'>БОТ АКТИВЕН</div>" +
                "                <div class='stat-value' id='botActive'>—</div>" +
                "            </div>" +
                "        </div>" +
                "        <div class='last-update' id='lastUpdate'>Последнее обновление: —</div>" +
                "    </div>" +
                "    <script>" +
                "        function updateStats() {" +
                "            fetch('/stats')" +
                "                .then(response => response.json())" +
                "                .then(data => {" +
                "                    const statusElement = document.getElementById('status');" +
                "                    statusElement.textContent = data.status === 'RUNNING' ? 'РАБОТАЕТ' : 'ОСТАНОВЛЕН';" +
                "                    statusElement.className = 'stat-value ' + (data.status === 'RUNNING' ? 'status-running' : 'status-stopped');" +
                "                    " +
                "                    document.getElementById('mobsKilled').textContent = data.mobsKilled;" +
                "                    " +
                "                    const botActiveElement = document.getElementById('botActive');" +
                "                    botActiveElement.textContent = data.botActive ? 'ДА' : 'НЕТ';" +
                "                    botActiveElement.className = 'stat-value ' + (data.botActive ? 'bot-active' : 'bot-inactive');" +
                "                    " +
                "                    document.getElementById('lastUpdate').textContent = 'Последнее обновление: ' + new Date().toLocaleTimeString();" +
                "                })" +
                "                .catch(error => {" +
                "                    console.error('Ошибка:', error);" +
                "                    document.getElementById('status').textContent = 'ОШИБКА';" +
                "                    document.getElementById('status').className = 'stat-value status-stopped';" +
                "                    document.getElementById('botActive').textContent = 'ОШИБКА';" +
                "                    document.getElementById('botActive').className = 'stat-value bot-inactive';" +
                "                });" +
                "        }" +
                "        setInterval(updateStats, 2000);" +
                "        updateStats();" +
                "    </script>" +
                "</body>" +
                "</html>";

        System.out.println("HTML length: " + html.length());
        return html;
    }

    public boolean isRunning() {
        return isRunning;
    }
}