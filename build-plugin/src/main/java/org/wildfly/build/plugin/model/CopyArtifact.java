package org.wildfly.build.plugin.model;

/**
 * @author Stuart Douglas
 */
public class CopyArtifact {

    private final String artifact;
    private final String toLocation;

    public CopyArtifact(String artifact, String toLocation) {
        this.artifact = artifact;
        this.toLocation = toLocation;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getToLocation() {
        return toLocation;
    }
}
