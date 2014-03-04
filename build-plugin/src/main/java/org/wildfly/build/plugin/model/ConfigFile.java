package org.wildfly.build.plugin.model;

/**
 * @author Stuart Douglas
 */
public class ConfigFile {

    private final String templateFile;
    private final String subsystemFile;
    private final String outputFile;


    public ConfigFile(String templateFile, String subsystemFile, String outputFile) {
        this.templateFile = templateFile;
        this.subsystemFile = subsystemFile;
        this.outputFile = outputFile;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public String getSubsystemFile() {
        return subsystemFile;
    }

    public String getOutputFile() {
        return outputFile;
    }
}
