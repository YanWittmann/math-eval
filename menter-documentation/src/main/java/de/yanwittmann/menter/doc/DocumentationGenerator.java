package de.yanwittmann.menter.doc;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import j2html.tags.specialized.ATag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DocumentationGenerator {

    public static void main(String[] args) throws IOException {
        final File baseDir = new File("doc");
        final File guideBaseDir = new File(baseDir, "guide");
        final File targetBaseDir = new File(baseDir, "target/site");
        final File markdownBaseDir = new File(guideBaseDir, "md");

        final File structureFile = new File(markdownBaseDir, "structure.txt");
        final File templateFile = new File(guideBaseDir, "template.html");

        if (args.length == 0 || args[0].equals("development")) {
            developmentAccess(guideBaseDir, targetBaseDir, structureFile, templateFile);
        } else if (args[0].equals("deploy")) {
            deploy(guideBaseDir, targetBaseDir, structureFile, templateFile);
        } else {
            System.out.println("Unknown argument: " + args[0]);
        }
    }

    public static void fileChangeListener(File directory, String[] fileTypes, Consumer<File> consumer) {
        new Thread(() -> {
            final Map<File, Long> lastModified = FileUtils.listFiles(directory, fileTypes, true).stream().collect(Collectors.toMap(file -> file, File::lastModified));
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                lastModified.entrySet().removeIf(entry -> !entry.getKey().exists());

                for (File file : lastModified.keySet()) {
                    if (file.lastModified() != lastModified.get(file)) {
                        lastModified.put(file, file.lastModified());
                        consumer.accept(file);
                    }
                }

                // check for new files
                for (File file : FileUtils.listFiles(directory, fileTypes, true)) {
                    if (!lastModified.containsKey(file)) {
                        lastModified.put(file, file.lastModified());
                        consumer.accept(file);
                    }
                }
            }
        }).start();
    }

    private static void deploy(File guideBaseDir, File targetBaseDir, File structureFile, File templateFile) throws IOException {
        generate(guideBaseDir, targetBaseDir, templateFile, structureFile);

        final Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("doc/credentials.properties")));
        final String remoteBaseDir = properties.getProperty("remoteBaseDir");
        final String remoteHost = properties.getProperty("remoteHost");
        final String remoteUser = properties.getProperty("remoteUser");
        final String remotePassword = properties.getProperty("remotePassword");

        upload(targetBaseDir, remoteBaseDir, remoteHost, remoteUser, remotePassword);
    }

    private static void developmentAccess(File guideBaseDir, File targetBaseDir, File structureFile, File templateFile) throws IOException {
        try {
            generate(guideBaseDir, targetBaseDir, templateFile, structureFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileChangeListener(guideBaseDir, new String[]{"md", "html", "js", "css", "png", "txt"}, file -> {
            try {
                generate(guideBaseDir, targetBaseDir, templateFile, structureFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void generate(File guideBaseDir, File targetBaseDir, File templateFile, File structureFile) throws IOException {
        final MutableDataSet options = new MutableDataSet();
        final Parser parser = Parser.builder(options).build();
        final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        if (!targetBaseDir.exists()) {
            targetBaseDir.mkdirs();
        }
        try {
            FileUtils.cleanDirectory(targetBaseDir);
        } catch (IOException e) {
            System.out.println("Could not clean target directory:" + e.getMessage());
        }

        // copy files to output directory
        Arrays.stream(new File[]{
                new File(guideBaseDir, "css"),
                new File(guideBaseDir, "js"),
                new File(guideBaseDir, "fonts"),
                new File(guideBaseDir, "img"),
        }).forEach(file -> {
            try {
                FileUtils.copyDirectoryToDirectory(file, targetBaseDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        final List<DocumentationPage> documentationPages = parseStructure(structureFile);

        for (DocumentationPage documentationPage : documentationPages) {
            documentationPage.parseContent(parser);
        }

        final List<String> template = FileUtils.readLines(templateFile, StandardCharsets.UTF_8);

        final String sidebarContent = renderSidebarContent(documentationPages);

        for (DocumentationPage documentationPage : documentationPages) {
            final String additionalJsContent = "let activePageFilename = '" + documentationPage.getOutFileName() + "';";

            final File outFile = documentationPage.getOutFile(targetBaseDir);
            final List<String> outLines = new ArrayList<>(template);

            for (int i = 0; i < outLines.size(); i++) {
                final String line = outLines.get(i);
                if (line.contains("{{ content.main }}")) {
                    try {
                        outLines.set(i, line.replace("{{ content.main }}", formatSpecialCharacters(documentationPage.renderPageContent(renderer).render())));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (line.contains("{{ content.sidebar }}")) {
                    outLines.set(i, line.replace("{{ content.sidebar }}", sidebarContent));
                } else if (line.contains("{{ script.js }}")) {
                    outLines.set(i, line.replace("{{ script.js }}", additionalJsContent));
                }
            }
            FileUtils.write(outFile, String.join("\n", outLines), StandardCharsets.UTF_8);
        }

        // generate an index.json file for the search
        final JSONArray indexArray = new JSONArray();
        for (DocumentationPage documentationPage : documentationPages) {
            indexArray.put(documentationPage.toIndexObject());
        }
        FileUtils.write(new File(targetBaseDir, "index.json"), indexArray.toString(), StandardCharsets.UTF_8);
    }

    private static String formatSpecialCharacters(String str) {
        return str.replace("--&gt;", "→")
                .replace("(c)", "©")
                .replace("(r)", "®")
                .replace("(tm)", "™");
    }

    private static String renderSidebarContent(List<DocumentationPage> documentationPages) {
        final List<ATag> sidebarItems = new ArrayList<>();
        for (DocumentationPage documentationPage : documentationPages) {
            sidebarItems.add(documentationPage.renderSidebarItem());
        }
        return sidebarItems.stream()
                .map(ATag::render)
                .collect(Collectors.joining("\n")) + "<a class=\"sidebar-menu-item\">&nbsp;</a>";
    }

    private static List<DocumentationPage> parseStructure(File file) throws IOException {
        final List<DocumentationPage> pages = new ArrayList<>();
        final List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);

        DocumentationPage currentTopLevelPage = null;

        for (String line : lines) {
            if (line.trim().length() == 0) continue;

            final String[] split = line.trim().split(">>");
            final String title = split[0].trim();

            final String fileName = split[1].trim();
            final File originFile = new File(file.getParentFile(), fileName);

            final DocumentationPage page = new DocumentationPage(originFile);
            page.setTitle(title);

            if (line.startsWith(" ") && currentTopLevelPage != null) {
                page.setParent(currentTopLevelPage);
                currentTopLevelPage.addSubPage(page);
            } else {
                currentTopLevelPage = page;
            }

            pages.add(page);
        }

        return pages;
    }

    public static void upload(File localBaseDir, String remoteBaseDir, String remoteHost, String remoteUser, String remotePassword) throws IOException {
        final FTPClient ftpClient = new FTPClient();

        ftpClient.connect(remoteHost);
        ftpClient.login(remoteUser, remotePassword);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

        if (!ftpClient.changeWorkingDirectory(remoteBaseDir)) {
            System.out.println("Creating remote directory " + remoteBaseDir);
            mkdirs(ftpClient, remoteBaseDir);
        }

        ftpClient.changeWorkingDirectory(remoteBaseDir);
        ftpClient.makeDirectory(remoteBaseDir);
        ftpClient.changeWorkingDirectory(remoteBaseDir);

        final FTPFile[] existingFtpFiles = ftpClient.listFiles();
        for (FTPFile existingFtpFile : existingFtpFiles) {
            System.out.println("Deleting remote file " + existingFtpFile.getName());
            ftpClient.deleteFile(existingFtpFile.getName());
        }

        uploadDirectory(ftpClient, localBaseDir, remoteBaseDir);
    }

    private static void mkdirs(FTPClient ftpClient, String remoteBaseDir) throws IOException {
        final String[] split = remoteBaseDir.split("/");
        String currentDir = "";
        for (String dir : split) {
            currentDir += "/" + dir;
            if (!ftpClient.changeWorkingDirectory(currentDir)) {
                ftpClient.makeDirectory(currentDir);
                ftpClient.changeWorkingDirectory(currentDir);
            }
        }
    }

    private static void uploadDirectory(FTPClient ftpClient, File localBaseDir, String remoteBaseDir) throws IOException {
        final File[] files = localBaseDir.listFiles();
        for (File file : files) {
            System.out.println("Uploading " + file.getAbsolutePath());
            if (file.isDirectory()) {
                ftpClient.makeDirectory(remoteBaseDir + "/" + file.getName());
                uploadDirectory(ftpClient, file, remoteBaseDir + "/" + file.getName());
            } else {
                final String remoteFilePath = remoteBaseDir + "/" + file.getName();
                final boolean uploaded = ftpClient.storeFile(remoteFilePath, FileUtils.openInputStream(file));
                if (!uploaded) {
                    System.out.println("Failed to upload file: " + remoteFilePath);
                }
            }
        }
    }
}