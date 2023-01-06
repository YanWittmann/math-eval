package de.yanwittmann.menter.interpreter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.yanwittmann.menter.interpreter.structure.Value;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MenterGuideServer {

    private static final Logger LOG = LogManager.getLogger(MenterGuideServer.class);

    private final static String REMOTE_GUIDE_URL = "https://yanwittmann.de/projects/menter/guide/introduction.html";

    public MenterGuideServer(MenterInterpreter interpreter, boolean safeMode, int port) throws IOException {
        System.out.println("Starting MenterGuideServer...");

        HttpServer server;
        final int serverPort = port != -1 ? port : 26045;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        } catch (IOException e) {
            throw new IOException("Could not start MenterGuideServer on port " + serverPort, e);
        }

        final String[] printBuffer = {""};
        MenterDebugger.printer = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                printBuffer[0] += (char) b;
            }
        });

        if (safeMode) {
            interpreter.getModuleOptions().addForbiddenImport("io");
            interpreter.getModuleOptions().addForbiddenImport("system");
            interpreter.getModuleOptions().addForbiddenImport("debug");
        }

        server.createContext("/api/guide", (exchange -> {
            LOG.info("Received request from " + exchange.getRemoteAddress().getAddress().getHostAddress());
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Length");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Origin");

            final JSONObject responseJson = new JSONObject();
            try {
                final String requestBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);

                if (requestBody.trim().isEmpty()) {
                    responseJson.put("error", "No request body");
                    exchange.sendResponseHeaders(200, responseJson.toString().getBytes().length);

                } else {
                    final JSONObject requestJson = new JSONObject(requestBody);

                    final Value result;
                    try {
                        final String code = requestJson.getString("code");
                        final String context = requestJson.getString("context");
                        LOG.info("Executing code [{}]: {}", context, code);
                        result = interpreter.evaluateInContextOf(code, context);

                        responseJson.put("result", result.toDisplayString());
                        responseJson.put("print", printBuffer[0]);
                        printBuffer[0] = "";
                        exchange.sendResponseHeaders(200, responseJson.toString().getBytes().length);
                    } catch (JSONException e) {
                        responseJson.put("error", "Invalid request body.");
                        exchange.sendResponseHeaders(200, responseJson.toString().getBytes().length);
                    } catch (Exception e) {
                        responseJson.put("result", Value.empty().toDisplayString());
                        responseJson.put("print", e.getMessage());
                        exchange.sendResponseHeaders(200, responseJson.toString().getBytes().length);
                    }
                }

            } catch (JSONException e) {
                responseJson.put("error", "Invalid JSON.");
                exchange.sendResponseHeaders(400, responseJson.toString().getBytes().length);
            } catch (Exception e) {
                responseJson.put("error", e.getMessage());
                exchange.sendResponseHeaders(500, responseJson.toString().getBytes().length);
            }

            final OutputStream output = exchange.getResponseBody();
            output.write(responseJson.toString().getBytes());
            output.flush();
            exchange.close();
        }));
        server.createContext("/api/ping", (exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Origin");

            final JSONObject responseJson = new JSONObject();
            responseJson.put("status", "ok");
            responseJson.put("version", MenterInterpreter.VERSION);
            responseJson.put("safeMode", safeMode);

            setRequestResponseAndClose(exchange, 200, responseJson);
        }));
        server.setExecutor(null); // creates a default executor
        server.start();

        System.out.println("MenterGuideServer started on " + REMOTE_GUIDE_URL + "?host=" + getInternalIp() + "&port=" + serverPort);
    }

    private String getInternalIp() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private void setRequestResponseAndClose(HttpExchange exchange, int code, JSONObject responseJson) throws IOException {
        exchange.sendResponseHeaders(code, responseJson.toString().getBytes().length);
        final OutputStream output = exchange.getResponseBody();
        output.write(responseJson.toString().getBytes());
        output.flush();
        exchange.close();
    }
}
