package de.yanwittmann.menter.interpreter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.yanwittmann.menter.interpreter.structure.value.Value;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class MenterGuideServer {

    private static final Logger LOG = LogManager.getLogger(MenterGuideServer.class);

    private final static String REMOTE_GUIDE_BASE_DIR_URL = "https://yanwittmann.de/projects/menter/guide";
    private final static String REMOTE_GUIDE_INTRODUCTION_URL = REMOTE_GUIDE_BASE_DIR_URL + "/introduction.html";

    private final Map<String, File> mirroredGuideDocFiles = new LinkedHashMap<>();

    public MenterGuideServer(MenterInterpreter interpreter, boolean safeMode, int port) throws IOException {
        System.out.println("Starting MenterGuideServer...");

        mirrorDocumentationIntoTempDir();

        HttpServer server;
        final int serverPort = port != -1 ? port : 26045;
        try {
            server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        } catch (IOException e) {
            throw new IOException("Could not start MenterGuideServer on port " + serverPort, e);
        }

        /*try {
            // keytool -genkey -keyalg RSA -alias localhost -keystore localhost.jks -storepass menter-cert-pass -validity 365 -keysize 2048 -ext SAN=DNS:localhost -deststoretype pkcs12
            // keytool -genkey -keyalg RSA -alias localhost -keystore localhost.jks -storepass menter-cert-pass -validity 365 -keysize 2048 -deststoretype pkcs12

            // Load the SSL certificate and private key from the keystore file
            KeyStore keyStore = KeyStore.getInstance("JKS");

            keyStore.load(getResourceAsStream("/res/cert/localhost.jks"), "menter-cert-pass".toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, "menter-cert-pass".toCharArray());

            // Initialize the SSL context with the key manager and trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            // Configure the HTTPS connector with the SSL context
            HttpsConfigurator configurator = new HttpsConfigurator(sslContext);
            server.setHttpsConfigurator(configurator);

        } catch (Exception e) {
            throw new IOException("Could not start MenterGuideServer on port " + serverPort, e);
        }*/

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
            LOG.info("Received ping from " + exchange.getRemoteAddress().getAddress().getHostAddress());
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Length");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Origin");

            final JSONObject responseJson = new JSONObject();
            responseJson.put("status", "ok");
            responseJson.put("version", MenterInterpreter.VERSION);
            responseJson.put("safeMode", safeMode);

            setRequestResponseAndClose(exchange, 200, responseJson.toString());
        }));

        server.createContext("/docs", (exchange -> {
            LOG.info("Received docs request from " + exchange.getRemoteAddress().getAddress().getHostAddress() + " on " + exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Length");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Access-Control-Allow-Origin");

            try {
                final String path = exchange.getRequestURI().getPath().substring(6);
                final String pathHtml = path + ".html";
                if (mirroredGuideDocFiles.containsKey(path) || mirroredGuideDocFiles.containsKey(pathHtml)) {
                    final File file = mirroredGuideDocFiles.getOrDefault(path, mirroredGuideDocFiles.get(pathHtml));
                    final String content = String.join("\n", FileUtils.readLines(file, StandardCharsets.UTF_8));
                    setRequestResponseAndClose(exchange, 200, content);
                    return;
                }

            } catch (Exception ignored) {
            }

            setRequestResponseAndClose(exchange, 404, "404 Not found");
        }));

        server.setExecutor(null); // creates a default executor
        server.start();

        final String internalIp = getInternalIp();
        final String externalIp = getExternalIp();
        System.out.printf("\n" +
                          "                               Server started on: http://%s:%d/docs/introduction?host=%s&port=%d\n" +
                          "              ... using your external IP address: http://%s:%d/docs/introduction?host=%s&port=%d\n" +
                          "   ... or on the source page if you disabled SSL: %s?host=%s&port=%d\n" +
                          "%n",
                internalIp, serverPort, internalIp, serverPort, externalIp, serverPort, internalIp, serverPort, REMOTE_GUIDE_INTRODUCTION_URL, internalIp, serverPort);
    }

    private void mirrorDocumentationIntoTempDir() {
        final File tempDir = getTempDirectory();
        final File docsDir = new File(tempDir, "docs");
        createDirOrThrow(docsDir);
        System.out.println("Mirroring documentation into " + docsDir.getAbsolutePath());

        final JSONArray docsIndex = new JSONArray(String.join("", getRemoteFileContent(REMOTE_GUIDE_BASE_DIR_URL + "/index.json")));

        for (int i = 0; i < docsIndex.length(); i++) {
            final JSONObject doc = docsIndex.getJSONObject(i);
            final String docFileName = doc.getString("file");
            final File targetFile = new File(docsDir, docFileName);

            downloadFileAndTransform(docFileName, targetFile, line -> {
                return line
                        .replace("js/interactive-codeboxes.js", REMOTE_GUIDE_BASE_DIR_URL + "/js/interactive-codeboxes.js")
                        .replace("js/documentation.js", REMOTE_GUIDE_BASE_DIR_URL + "/js/documentation.js")
                        .replaceAll("<img src=\"img/([^\"]+)\"", "<img src=\"" + REMOTE_GUIDE_BASE_DIR_URL + "/img/$1\"");
            });
        }


        downloadFileAndTransform("css/index.css", new File(docsDir, "css/index.css"), line -> {
            return line
                    .replace("font-size: 20px;", "font-size: 17px;")
                    .replace("src: url('../fonts/linux-biolinum-g-webfont.woff2') format('woff2'),", "")
                    .replace("url('../fonts/linux-biolinum-g-webfont.woff') format('woff');", "");
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private void downloadFileAndTransform(String docFileName, File targetFile, UnaryOperator<String> transformer) {
        final List<String> lines = getRemoteFileContent(REMOTE_GUIDE_BASE_DIR_URL + "/" + docFileName);
        lines.replaceAll(transformer);
        try {
            FileUtils.writeLines(targetFile, lines, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mirroredGuideDocFiles.put(docFileName, targetFile);
    }

    private File getTempDirectory() {
        final String tempDir = System.getProperty("java.io.tmpdir");
        final File menterTempDir = new File(tempDir, "menter");
        createDirOrThrow(menterTempDir);
        return menterTempDir;
    }

    private List<String> getRemoteFileContent(String url) {
        try {
            final URL website = new URL(url);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(website.openStream()))) {
                return in.lines().collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not download file " + url, e);
        }
    }

    private String getInternalIp() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            return "localhost";
        }
    }

    private String getExternalIp() {
        try {
            return new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream())).readLine();
        } catch (IOException e) {
            return "localhost";
        }
    }

    private void setRequestResponseAndClose(HttpExchange exchange, int code, String response) throws IOException {
        exchange.sendResponseHeaders(code, response.toString().getBytes().length);
        final OutputStream output = exchange.getResponseBody();
        output.write(response.toString().getBytes());
        output.flush();
        exchange.close();
    }

    private static void createDirOrThrow(File menterTempDir) {
        if (!menterTempDir.exists()) {
            if (!menterTempDir.mkdir()) {
                throw new RuntimeException("Could not create directory " + menterTempDir.getAbsolutePath());
            }
        }
    }

}
