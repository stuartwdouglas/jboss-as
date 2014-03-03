package org.wildfly.build.plugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class Build {

    private final List<Server> servers = new ArrayList<>();
    private final List<CopyArtifact> copyArtifacts = new ArrayList<>();

    public List<Server> getServers() {
        return servers;
    }

    public List<CopyArtifact> getCopyArtifacts() {
        return copyArtifacts;
    }
}
