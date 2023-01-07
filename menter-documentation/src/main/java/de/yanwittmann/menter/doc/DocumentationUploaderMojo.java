package de.yanwittmann.menter.doc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "upload-documentation")
public class DocumentationUploaderMojo extends AbstractMojo {

    @Parameter(required = true)
    private File localBaseDir;

    @Parameter(required = true)
    private String remoteBaseDir;

    @Parameter(required = true)
    private String remoteHost;

    @Parameter(required = true)
    private String remoteUser;

    @Parameter(required = true)
    private String remotePassword;

    public void execute() throws MojoExecutionException {
        try {
            getLog().info("localBaseDir: " + localBaseDir.getAbsolutePath());
            getLog().info("remoteBaseDir: " + remoteBaseDir);
            getLog().info("remoteHost: " + remoteHost);

            DocumentationGenerator.upload(localBaseDir, remoteBaseDir, remoteHost, remoteUser, remotePassword);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to upload the documentation for Menter: " + e.getMessage(), e);
        }
    }
}
