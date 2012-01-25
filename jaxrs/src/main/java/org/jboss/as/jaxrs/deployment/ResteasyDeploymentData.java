package org.jboss.as.jaxrs.deployment;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class ResteasyDeploymentData {
    private boolean scanAll;
    private boolean scanResources;
    private boolean scanProviders;
    private final Set<String> scannedResourceClasses = new LinkedHashSet<String>();
    private final Set<String> scannedProviderClasses = new LinkedHashSet<String>();
    private final Set<Class<? extends Application>> scannedApplicationClasses = new HashSet<Class<? extends Application>>();
    private final Set<Class<? extends Application>> webXmlApplicationClasses = new HashSet<Class<? extends Application>>();
    private boolean bootClasses;
    private boolean unwrappedExceptionsParameterSet;
    private final Set<String> scannedJndiComponentResources = new LinkedHashSet<String>();

    /**
     * Merges a list of additional JAX-RS deployment data with this lot of deployment data.
     *
     * @param deploymentData
     */
    public void merge(final List<ResteasyDeploymentData> deploymentData) throws DeploymentUnitProcessingException {

        for (ResteasyDeploymentData data : deploymentData) {
            scannedApplicationClasses.addAll(data.getScannedApplicationClasses());
            webXmlApplicationClasses.addAll(data.getWebXmlApplicationClasses());
            if (scanResources) {
                scannedResourceClasses.addAll(data.getScannedResourceClasses());
                scannedJndiComponentResources.addAll(data.getScannedJndiComponentResources());
            }
            if (scanProviders) {
                scannedProviderClasses.addAll(data.getScannedProviderClasses());
            }
        }
    }

    public Set<String> getScannedJndiComponentResources() {
        return scannedJndiComponentResources;
    }

    public boolean hasBootClasses() {
        return bootClasses;
    }

    public void setBootClasses(boolean bootClasses) {
        this.bootClasses = bootClasses;
    }

    public boolean shouldScan() {
        return scanAll || scanResources || scanProviders;
    }

    public boolean isScanAll() {
        return scanAll;
    }

    public void setScanAll(boolean scanAll) {
        if (scanAll) {
            scanResources = true;
            scanProviders = true;
        }
        this.scanAll = scanAll;
    }

    public boolean isScanResources() {
        return scanResources;
    }

    public void setScanResources(boolean scanResources) {
        this.scanResources = scanResources;
    }

    public boolean isScanProviders() {
        return scanProviders;
    }

    public void setScanProviders(boolean scanProviders) {
        this.scanProviders = scanProviders;
    }

    public Set<String> getScannedResourceClasses() {
        return scannedResourceClasses;
    }

    public Set<String> getScannedProviderClasses() {
        return scannedProviderClasses;
    }

    public boolean isUnwrappedExceptionsParameterSet() {
        return unwrappedExceptionsParameterSet;
    }

    public void setUnwrappedExceptionsParameterSet(boolean unwrappedExceptionsParameterSet) {
        this.unwrappedExceptionsParameterSet = unwrappedExceptionsParameterSet;
    }

    public Set<Class<? extends Application>> getScannedApplicationClasses() {
        return scannedApplicationClasses;
    }

    public Set<Class<? extends Application>> getWebXmlApplicationClasses() {
        return webXmlApplicationClasses;
    }
}
