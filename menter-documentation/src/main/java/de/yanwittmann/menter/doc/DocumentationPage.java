package de.yanwittmann.menter.doc;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static j2html.TagCreator.*;

public class DocumentationPage {

    private final File originFile;
    private DocumentationPage parent;
    private final List<DocumentationPage> subPages = new ArrayList<>();
    private String title;
    private Node content;

    public DocumentationPage(File originFile) {
        this.originFile = originFile;
    }

    void parseContent(Parser parser) throws IOException {
        if (content == null) {
            content = parser.parse(String.join("\n", FileUtils.readLines(originFile, StandardCharsets.UTF_8)));
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setParent(DocumentationPage parent) {
        this.parent = parent;
    }

    public void addSubPage(DocumentationPage subPage) {
        subPage.setParent(this);
        subPages.add(subPage);
    }

    public DomContent renderPageContent(HtmlRenderer renderer) {
        final DivTag div = div();

        for (Node child : content.getChildren()) {
            if (child instanceof FencedCodeBlock) {
                final String codeBlockType = ((FencedCodeBlock) child).getInfo().toString();
                final List<String> codeBlockLines = ((FencedCodeBlock) child).getContentLines().stream()
                        .map(BasedSequence::toString)
                        .map(e -> e.replace("\n", ""))
                        .collect(Collectors.toList());

                // trim empty lines from start and end
                while (codeBlockLines.get(0).isEmpty()) codeBlockLines.remove(0);
                while (codeBlockLines.get(codeBlockLines.size() - 1).isEmpty())
                    codeBlockLines.remove(codeBlockLines.size() - 1);

                final boolean isStatic = codeBlockType.startsWith("static");
                final String[] presetResult = codeBlockType.contains("=") ? codeBlockType.split("=", 2) : null;

                div.with(
                        div().withClass("codebox-container")
                                .attr("initialContent", String.join(":NEWLINE:", codeBlockLines))
                                .attr("interactive", !isStatic)
                                .attr("result", presetResult != null ? presetResult[1] : "")
                );
            } else {
                div.with(rawHtml(renderer.render(child)));
            }
        }

        return div;
    }

    public File getOutFile(File targetBaseDir) {
        return new File(targetBaseDir, getOutFileName());
    }

    public String getOutFileName() {
        return (parent != null ? parent.title + "_" : "") + originFile.getName().replace(".md", ".html");
    }

    public ATag renderSidebarItem() {
        return a(iff(parent != null, rawHtml("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")), text(title))
                .withHref(getOutFileName())
                .withClass("sidebar-menu-item");
    }

    @Override
    public String toString() {
        return (parent != null ? parent.title + " >> " : "") + title;
    }
}
