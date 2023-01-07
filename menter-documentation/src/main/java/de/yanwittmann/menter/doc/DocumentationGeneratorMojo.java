package de.yanwittmann.menter.doc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generate-documentation")
public class DocumentationGeneratorMojo extends AbstractMojo {

    @Parameter(required = true)
    private File guideBaseDir;

    @Parameter(required = true)
    private File targetBaseDir;

    @Parameter(required = true)
    private File templateFile;

    @Parameter(required = true)
    private File structureFile;

    public void execute() throws MojoExecutionException {
        try {
            getLog().info("guideBaseDir: " + guideBaseDir.getAbsolutePath());
            getLog().info("targetBaseDir: " + targetBaseDir.getAbsolutePath());
            getLog().info("templateFile: " + templateFile.getAbsolutePath());
            getLog().info("structureFile: " + structureFile.getAbsolutePath());

            DocumentationGenerator.generate(guideBaseDir, targetBaseDir, templateFile, structureFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate the documentation for Menter: " + e.getMessage(), e);
        }
    }
}
