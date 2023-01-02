package de.yanwittmann.menter.interpreter;

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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MenterGuideServer {

    private static final Logger LOG = LogManager.getLogger(MenterGuideServer.class);

    public MenterGuideServer(MenterInterpreter interpreter, boolean safeMode) throws IOException {
        LOG.info("Starting MenterGuideServer...");

        HttpServer server;
        final int serverPort = 26045;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        } catch (IOException e) {
            throw new IOException("Could not start MenterGuideServer on port " + serverPort, e);
        }

        final String[] printBuffer = {""};
        MenterDebugger.printer = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                printBuffer[0] += (char) b;
            }
        });

        if (!safeMode) {
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
                    responseJson.put("error", "No request body.");
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

            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        server.start();

        LOG.info("MenterGuideServer started on port [{}]", serverPort);
    }
}
