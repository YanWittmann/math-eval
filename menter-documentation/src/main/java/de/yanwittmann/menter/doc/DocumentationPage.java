package de.yanwittmann.menter.doc;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            content = parser.parse(
                    String.join("\n", replaceVariables(
                            FileUtils.readLines(originFile, StandardCharsets.UTF_8)
                    ))
            );
        }
    }

    private final static Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s[^ ]+:\\s[^ ]+\\s}}");

    private List<String> replaceVariables(List<String> input) {
        // embedded files: {{ file: menter-lang/src/main/resources/src/system.mtr }}

        final List<String> output = new ArrayList<>();

        for (String line : input) {
            final String trimmedLine = line.trim();
            final Matcher matcher = VARIABLE_PATTERN.matcher(trimmedLine);

            while (matcher.find()) {
                final String variable = matcher.group();
                final String[] variableParts = variable.substring(3, variable.length() - 3).split(":", 2);
                final String variableType = variableParts[0].trim();
                final String variableName = variableParts[1].trim();

                if (variableType.equals("file")) {
                    final File file = new File(variableName);
                    if (!file.exists()) {
                        throw new IllegalArgumentException("File " + file.getAbsolutePath() + " does not exist!");
                    }
                    try {
                        final List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
                        line = line.replace(variable, String.join("\n", lines));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    throw new IllegalArgumentException("Unknown variable type " + variableType + "!");
                }
            }

            output.add(line);
        }

        return output;
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

        System.out.println("Rendering " + getOutFileName());

        for (Node child : content.getChildren()) {
            // div.with(rawHtml(renderer.render(child)));
            div.with(render(renderer, child));
        }

        return div;
    }

    public DomContent render(HtmlRenderer renderer, Object input) {
        if (input instanceof Node) {
            final Node node = (Node) input;

            final DomContent tag;

            if (node instanceof Paragraph) {
                tag = p();
            } else if (node instanceof Heading) {
                final int level = ((Heading) node).getLevel();
                if (level == 1) {
                    tag = h1();
                } else if (level == 2) {
                    tag = h2();
                } else if (level == 3) {
                    tag = h3();
                } else if (level == 4) {
                    tag = h4();
                } else if (level == 5) {
                    tag = h5();
                } else if (level == 6) {
                    tag = h6();
                } else {
                    tag = h1();
                }
            } else if (node instanceof Text) {
                tag = text(node.getChars().toString());
            } else if (node instanceof HardLineBreak) {
                tag = br();
            } else if (node instanceof SoftLineBreak) {
                tag = text(" ");
            } else if (node instanceof BulletList) {
                tag = ul();
            } else if (node instanceof BulletListItem) {
                tag = li();
            } else if (node instanceof OrderedList) {
                tag = ol();
            } else if (node instanceof OrderedListItem) {
                tag = li();
            } else if (node instanceof Link) {
                tag = a().withHref(((Link) node).getUrl().toString());
            } else if (node instanceof LinkRef) {
                throw new UnsupportedOperationException("LinkRef not supported, do not include spaces in links!");
            } else if (node instanceof Code) {
                tag = code();
            } else if (node instanceof Emphasis) {
                tag = em();
            } else if (node instanceof StrongEmphasis) {
                tag = strong();
            } else if (node instanceof BlockQuote) {
                tag = blockquote();
            } else if (node instanceof Image) {
                final String imageUrl = ((Image) node).getUrl().toString();
                final String alt = ((Image) node).getText().toString();

                if (imageUrl.startsWith("guide/")) {
                    final String guideImagePath = "img/" + imageUrl.substring("guide/".length());
                    tag = img().withSrc(guideImagePath).withAlt(alt);
                } else {
                    tag = img().withSrc(imageUrl).withAlt(alt);
                }
            } else if (node instanceof FencedCodeBlock) {
                final String codeBlockType = ((FencedCodeBlock) node).getInfo().toString();
                final List<String> codeBlockLines = ((FencedCodeBlock) node).getContentLines().stream()
                        .map(BasedSequence::toString)
                        .map(e -> e.replace("\n", ""))
                        .collect(Collectors.toList());

                // trim empty lines from start and end
                while (codeBlockLines.get(0).isEmpty()) codeBlockLines.remove(0);
                while (codeBlockLines.get(codeBlockLines.size() - 1).isEmpty())
                    codeBlockLines.remove(codeBlockLines.size() - 1);

                final String[] typeArguments = codeBlockType.split("---");
                final Map<String, String> typeArgumentsMap = Arrays.stream(typeArguments)
                        .map(e -> e.split("=", 2))
                        .collect(Collectors.toMap(e -> e[0], e -> e.length > 1 ? e[1] : ""));

                final boolean isStatic = typeArgumentsMap.containsKey("static");
                final String presetResult = typeArgumentsMap.getOrDefault("result", null);
                final String id = typeArgumentsMap.getOrDefault("id", null);
                final String after = typeArgumentsMap.getOrDefault("after", null);
                final String lang = typeArgumentsMap.getOrDefault("lang", null);

                final DivTag actualCodeboxTag = div().withClasses("codebox-container", "codebox-fake")
                        .attr("initialContent", String.join(":NEWLINE:", codeBlockLines))
                        .attr("interactive", !isStatic);

                if (id != null) actualCodeboxTag.attr("codebox-id", id);
                if (after != null) actualCodeboxTag.attr("after", after);
                if (presetResult != null) actualCodeboxTag.attr("result", presetResult);
                if (lang != null) actualCodeboxTag.attr("lang", lang);

                tag = actualCodeboxTag;
            } else {
                System.err.println("Unknown input: " + input.getClass().getSimpleName() + ", using default renderer");
                return rawHtml(renderer.render(node));
            }

            if (tag != null) {
                if (tag instanceof ContainerTag) {
                    for (Node child : node.getChildren()) {
                        ((ContainerTag<?>) tag).with(render(renderer, child));
                    }
                }

                return tag;
            }
        }

        return null;
    }

    public File getOutFile(File targetBaseDir) {
        return new File(targetBaseDir, getOutFileName());
    }

    public String getOutFileName() {
        return (parent != null ? parent.title.replace(" ", "_") + "_" : "") + originFile.getName().replace(".md", ".html");
    }

    public String getTitle() {
        return title;
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

    public JSONObject toIndexObject() {
        final JSONObject object = new JSONObject();
        object.put("title", title);
        object.put("file", getOutFileName());
        if (subPages.size() > 0) {
            final List<JSONObject> subPagesObjects = subPages.stream()
                    .map(DocumentationPage::toIndexObject)
                    .collect(Collectors.toList());
            object.put("subPages", subPagesObjects);
        }
        return object;
    }
}
