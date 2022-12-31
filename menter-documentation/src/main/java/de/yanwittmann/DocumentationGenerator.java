package de.yanwittmann;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DocumentationGenerator {

    public static void main(String[] args) throws IOException {
        final MutableDataSet options = new MutableDataSet();

        final Parser parser = Parser.builder(options).build();
        final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        renderFile(
                new File("D:\\files\\create\\programming\\projects\\menter-lang-project\\doc\\guide\\md\\index.md"),
                new File("D:\\files\\create\\programming\\projects\\menter-lang-project\\doc\\guide\\md\\index.html"),
                parser, renderer
        );
    }

    private static void renderFile(File file, File target, Parser parser, HtmlRenderer renderer) throws IOException {
        final List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        final Node document = parser.parse(String.join("\n", lines));
        final String html = renderer.render(document);
        FileUtils.write(target, html, StandardCharsets.UTF_8);
    }
}