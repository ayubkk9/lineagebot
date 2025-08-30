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
            System.out.println("Getting stats from bot controller...");

            if (botController == null) {
                System.out.println("BotController is NULL!");
                JSONObject error = new JSONObject();
                error.put("status", "ERROR");
                error.put("error", "BotController is null");
                return error.toString();
            }

            // Принудительно обновляем статистику
            System.out.println("Calling forceStatsUpdate...");
            botController.forceStatsUpdate();

            System.out.println("Getting bot stats...");
            BotStats stats = botController.getBotStats();

            if (stats == null) {
                System.out.println("BotStats is NULL!");
                JSONObject error = new JSONObject();
                error.put("status", "ERROR");
                error.put("error", "BotStats is null");
                return error.toString();
            }

            System.out.println("Creating JSON response...");
            JSONObject json = new JSONObject();
            json.put("mobsKilled", stats.getMobsKilled());
            json.put("isAlive", stats.isAlive());
            json.put("currentHp", Math.round(stats.getCurrentHp() * 10) / 10.0);
            json.put("currentMp", Math.round(stats.getCurrentMp() * 10) / 10.0);
            json.put("status", stats.getStatus());
            json.put("uptime", stats.getUptime());
            json.put("deaths", stats.getDeaths());

            String jsonString = json.toString();
            System.out.println("Sending stats: " + jsonString);
            return jsonString;

        } catch (Exception e) {
            System.out.println("Error getting stats: " + e.getMessage());
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("status", "ERROR");
            error.put("error", e.getMessage());
            return error.toString();
        }
    }

    private void sendJsonResponse(PrintWriter out, String json) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
        out.println("Connection: close");
        out.println();
        out.println(json);
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
        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Lineage II Bot Monitor</title>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 10px; }" +
                "        h1 { text-align: center; color: #2c3e50; }" +
                "        .stat { margin: 10px 0; padding: 10px; background: #f8f9fa; border-radius: 5px; }" +
                "        .status-running { color: green; }" +
                "        .status-stopped { color: red; }" +
                "        .alive { color: green; }" +
                "        .dead { color: red; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <h1>🤖 Lineage II Bot Monitor</h1>" +
                "        " +
                "        <div class='stat'><strong>Status:</strong> <span id='status' class='status-stopped'>STOPPED</span></div>" +
                "        <div class='stat'><strong>Mobs Killed:</strong> <span id='mobsKilled'>0</span></div>" +
                "        <div class='stat'><strong>Deaths:</strong> <span id='deaths'>0</span></div>" +
                "        <div class='stat'><strong>HP:</strong> <span id='currentHp'>0.0</span>%</div>" +
                "        <div class='stat'><strong>MP:</strong> <span id='currentMp'>0.0</span>%</div>" +
                "        <div class='stat'><strong>Alive:</strong> <span id='isAlive' class='dead'>No</span></div>" +
                "        <div class='stat'><strong>Uptime:</strong> <span id='uptime'>00:00:00</span></div>" +
                "        <div class='stat'><strong>Last Update:</strong> <span id='lastUpdate'>never</span></div>" +
                "    </div>" +
                "" +
                "    <script>" +
                "        function updateStats() {" +
                "            fetch('/stats')" +
                "                .then(response => response.json())" +
                "                .then(data => {" +
                "                    console.log('Data received:', data);" +
                "                    " +
                "                    document.getElementById('mobsKilled').textContent = data.mobsKilled;" +
                "                    document.getElementById('deaths').textContent = data.deaths;" +
                "                    document.getElementById('currentHp').textContent = data.currentHp.toFixed(1);" +
                "                    document.getElementById('currentMp').textContent = data.currentMp.toFixed(1);" +
                "                    document.getElementById('uptime').textContent = formatTime(data.uptime);" +
                "                    " +
                "                    const statusElement = document.getElementById('status');" +
                "                    statusElement.textContent = data.status;" +
                "                    statusElement.className = data.status === 'RUNNING' ? 'status-running' : 'status-stopped';" +
                "                    " +
                "                    const aliveElement = document.getElementById('isAlive');" +
                "                    aliveElement.textContent = data.isAlive ? 'Yes' : 'No';" +
                "                    aliveElement.className = data.isAlive ? 'alive' : 'dead';" +
                "                    " +
                "                    document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString();" +
                "                })" +
                "                .catch(error => {" +
                "                    console.error('Error:', error);" +
                "                    document.getElementById('status').textContent = 'ERROR';" +
                "                });" +
                "        }" +
                "" +
                "        function formatTime(ms) {" +
                "            if (!ms) return '00:00:00';" +
                "            const totalSeconds = Math.floor(ms / 1000);" +
                "            const hours = Math.floor(totalSeconds / 3600);" +
                "            const minutes = Math.floor((totalSeconds % 3600) / 60);" +
                "            const seconds = totalSeconds % 60;" +
                "            " +
                "            return hours.toString().padStart(2, '0') + ':' + " +
                "                   minutes.toString().padStart(2, '0') + ':' + " +
                "                   seconds.toString().padStart(2, '0');" +
                "        }" +
                "" +
                "        setInterval(updateStats, 2000);" +
                "        updateStats();" +
                "    </script>" +
                "</body>" +
                "</html>";
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