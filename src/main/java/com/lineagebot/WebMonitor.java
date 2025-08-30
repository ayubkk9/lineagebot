package com.lineagebot;

import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class WebMonitor {
    private final BotController botController;
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private Thread serverThread;

    public WebMonitor(BotController botController) {
        this.botController = botController;
    }

    public void startWebServer(int port) {
        if (isRunning) return;

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // Таймаут для accept
            isRunning = true;

            serverThread = new Thread(() -> {
                System.out.println("Web server thread started");
                while (isRunning && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("Client connected: " + clientSocket.getInetAddress());
                        handleClient(clientSocket);
                    } catch (SocketTimeoutException e) {
                        // Таймаут - нормальная ситуация, продолжаем цикл
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

            System.out.println("Web monitor started on port " + port);

        } catch (IOException e) {
            System.out.println("Failed to start web server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket; BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            try {

                String requestLine = in.readLine();
                if (requestLine == null) {
                    System.out.println("Empty request");
                    return;
                }

                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 2) {
                    System.out.println("Invalid request: " + requestLine);
                    return;
                }

                String method = requestParts[0];
                String path = requestParts[1];

                System.out.println("Request: " + method + " " + path);

                // Читаем все заголовки
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
                    // Обработка CORS preflight
                    sendOptionsResponse(out);
                } else {
                    System.out.println("Unsupported method: " + method);
                }

            } catch (IOException e) {
                System.out.println("Client handling error: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    private String getStatsJson() {
        try {
            if (botController == null) {
                return "{\"status\":\"STOPPED\",\"mobsKilled\":0}";
            }

            BotStats stats = botController.getBotStats();
            if (stats == null) {
                return "{\"status\":\"STOPPED\",\"mobsKilled\":0}";
            }

            // Только статус и счетчик мобов
            return String.format(
                    "{\"status\":\"%s\",\"mobsKilled\":%d}",
                    stats.getStatus(),
                    stats.getMobsKilled()
            );

        } catch (Exception e) {
            System.out.println("Error in getStatsJson: " + e.getMessage());
            return "{\"status\":\"ERROR\",\"mobsKilled\":0}";
        }
    }

    private void sendJsonResponse(PrintWriter out, String json) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json"); // Убираем charset=UTF-8
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Connection: close");
        out.println(); // Пустая строка перед телом
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
                "                    document.getElementById('mobsKilled').textContent = data.mobsKilled;" +
                "                    document.getElementById('lastUpdate').textContent = 'Последнее обновление: ' + new Date().toLocaleTimeString();" +
                "                })" +
                "                .catch(error => {" +
                "                    console.error('Ошибка:', error);" +
                "                    document.getElementById('status').textContent = 'ОШИБКА';" +
                "                    document.getElementById('status').className = 'stat-value status-stopped';" +
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

    public void stopWebServer() {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server: " + e.getMessage());
            }
        }

        // Ждем завершения потока
        if (serverThread != null && serverThread.isAlive()) {
            try {
                serverThread.join(2000); // Ждем 2 секунды
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Web monitor stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }
}