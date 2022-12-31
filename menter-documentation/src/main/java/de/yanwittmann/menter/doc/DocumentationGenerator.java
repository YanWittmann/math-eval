package de.yanwittmann.menter.doc;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import j2html.tags.specialized.ATag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DocumentationGenerator {

    public static void main(String[] args) throws IOException {
        final MutableDataSet options = new MutableDataSet();
        final Parser parser = Parser.builder(options).build();
        final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        final File guideBaseDir = new File("doc/guide");
        final File markdownBaseDir = new File(guideBaseDir, "md");
        final File targetBaseDir = new File("menter-documentation/target/site");

        final File structureFile = new File(markdownBaseDir, "structure.txt");
        final File templateFile = new File(guideBaseDir, "template.html");

        FileUtils.cleanDirectory(targetBaseDir);

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

        if (!targetBaseDir.exists()) {
            targetBaseDir.mkdirs();
        }

        final List<String> template = FileUtils.readLines(templateFile, StandardCharsets.UTF_8);

        final String sidebarContent = renderSidebarContent(documentationPages);

        for (DocumentationPage documentationPage : documentationPages) {
            final File outFile = documentationPage.getOutFile(targetBaseDir);

            final List<String> outLines = new ArrayList<>(template);
            for (int i = 0; i < outLines.size(); i++) {
                final String line = outLines.get(i);
                if (line.contains("{{ content.main }}")) {
                    outLines.set(i, line.replace("{{ content.main }}", documentationPage.renderPageContent(renderer).render()));
                } else if (line.contains("content.sidebar")) {
                    outLines.set(i, line.replace("{{ content.sidebar }}", sidebarContent));
                }
            }
            FileUtils.write(outFile, String.join("\n", outLines), StandardCharsets.UTF_8);
        }
    }

    private static String renderSidebarContent(List<DocumentationPage> documentationPages) {
        final List<ATag> sidebarItems = new ArrayList<>();
        for (DocumentationPage documentationPage : documentationPages) {
            sidebarItems.add(documentationPage.renderSidebarItem());
        }
        return sidebarItems.stream().map(ATag::render).collect(Collectors.joining("\n"));
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
}