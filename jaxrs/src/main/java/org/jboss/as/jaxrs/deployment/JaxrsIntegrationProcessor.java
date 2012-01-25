package org.jboss.as.jaxrs.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import static org.jboss.as.jaxrs.JaxrsLogger.JAXRS_LOGGER;
import static org.jboss.as.jaxrs.JaxrsMessages.MESSAGES;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stuart Douglas
 */
public class JaxrsIntegrationProcessor implements DeploymentUnitProcessor {
    private static final String JAX_RS_SERVLET_NAME = "javax.ws.rs.core.Application";
    private static final String SERVLET_INIT_PARAM = "javax.ws.rs.Application";
    public static final String RESTEASY_SCAN = "resteasy.scan";
    public static final String RESTEASY_SCAN_RESOURCES = "resteasy.scan.resources";
    public static final String RESTEASY_SCAN_PROVIDERS = "resteasy.scan.providers";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final DeploymentUnit parent = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);

        final Map<String, String> initParameters = new HashMap<String, String>();

        if (resteasy == null)
            return;

        //remove the resteasy.scan parameter
        //because it is not needed
        final List<ParamValueMetaData> params = webdata.getContextParams();
        if (params != null) {
            Iterator<ParamValueMetaData> it = params.iterator();
            while (it.hasNext()) {
                final ParamValueMetaData param = it.next();
                if (param.getParamName().equals(RESTEASY_SCAN)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN);
                } else if (param.getParamName().equals(RESTEASY_SCAN_RESOURCES)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN_RESOURCES);
                } else if (param.getParamName().equals(RESTEASY_SCAN_PROVIDERS)) {
                    it.remove();
                    JAXRS_LOGGER.resteasyScanWarning(RESTEASY_SCAN_PROVIDERS);
                }
            }
        }


        final Map<ModuleIdentifier, ResteasyDeploymentData> attachmentMap = parent.getAttachment(JaxrsAttachments.ADDITIONAL_RESTEASY_DEPLOYMENT_DATA);
        final List<ResteasyDeploymentData> additionalData = new ArrayList<ResteasyDeploymentData>();
        final ModuleSpecification moduleSpec = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        if (moduleSpec != null && attachmentMap != null) {
            for (ModuleDependency dep : moduleSpec.getAllDependencies()) {
                if (attachmentMap.containsKey(dep.getIdentifier())) {
                    additionalData.add(attachmentMap.get(dep.getIdentifier()));
                }
            }
            resteasy.merge(additionalData);
        }
        if (!resteasy.getScannedResourceClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedResourceClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String resources = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS resource classes: %s", resources);
            initParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, resources);
        }
        if (!resteasy.getScannedProviderClasses().isEmpty()) {
            StringBuffer buf = null;
            for (String provider : resteasy.getScannedProviderClasses()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(provider);
                } else {
                    buf.append(",").append(provider);
                }
            }
            String providers = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS provider classes: %s", providers);
            initParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_PROVIDERS, providers);
        }

        if (!resteasy.getScannedJndiComponentResources().isEmpty()) {
            StringBuffer buf = null;
            for (String resource : resteasy.getScannedJndiComponentResources()) {
                if (buf == null) {
                    buf = new StringBuffer();
                    buf.append(resource);
                } else {
                    buf.append(",").append(resource);
                }
            }
            String providers = buf.toString();
            JAXRS_LOGGER.debugf("Adding JAX-RS jndi component resource classes: %s", providers);
            initParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_JNDI_RESOURCES, providers);
        }

        if (!resteasy.isUnwrappedExceptionsParameterSet()) {
            initParameters.put(ResteasyContextParameters.RESTEASY_UNWRAPPED_EXCEPTIONS, "javax.ejb.EJBException");
        }

        if (resteasy.hasBootClasses())
            return;


        //if there are no JAX-RS classes in the app just return
        if (resteasy.getScannedApplicationClasses().isEmpty()
                && resteasy.getScannedJndiComponentResources().isEmpty()
                && resteasy.getScannedProviderClasses().isEmpty()
                && resteasy.getScannedResourceClasses().isEmpty()) {
            return;
        }

        boolean mappingSet = false;
        if (resteasy.getScannedApplicationClasses().isEmpty()) {
            //if there is no scanned application we must add a servlet with a name of
            //javax.ws.rs.core.Application
            JBossServletMetaData servlet = new JBossServletMetaData();
            servlet.setName(JAX_RS_SERVLET_NAME);
            servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
            servlet.setAsyncSupported(true);
            for (final Map.Entry<String, String> entry : initParameters.entrySet()) {
                setInitParameter(servlet, entry.getKey(), entry.getValue());
            }
            setupMapping(webdata, JAX_RS_SERVLET_NAME, servlet);
            addServlet(webdata, servlet);

        } else {
            if (servletMappingsExist(webdata, JAX_RS_SERVLET_NAME)) {
                throw new DeploymentUnitProcessingException(MESSAGES.conflictUrlMapping());

            }

            for (final Class<? extends Application> application : resteasy.getScannedApplicationClasses()) {

                //now there are two options.
                //if there is already a servlet defined with an init param
                //we just add our init params
                //otherwise we add our filter


                JBossServletMetaData servlet = findServletWithInitParam(webdata, SERVLET_INIT_PARAM, application);
                if (servlet == null) {

                    //add a servlet named after the application class
                    servlet = new JBossServletMetaData();
                    servlet.setName(application.getName());
                    servlet.setServletClass(HttpServlet30Dispatcher.class.getName());
                    servlet.setAsyncSupported(true);
                    setInitParameter(servlet, SERVLET_INIT_PARAM, application.getName());
                    addServlet(webdata, servlet);

                }

                for (final Map.Entry<String, String> entry : initParameters.entrySet()) {
                    setInitParameter(servlet, entry.getKey(), entry.getValue());
                }

                //look for servlet mappings
                if (!servletMappingsExist(webdata, application.getName())) {
                    //no mappings, add our own
                    List<String> patterns = new ArrayList<String>();
                    if (application.isAnnotationPresent(ApplicationPath.class)) {
                        final ApplicationPath path = application.getAnnotation(ApplicationPath.class);
                        String pathValue = path.value().trim();
                        if (!pathValue.startsWith("/")) {
                            pathValue = "/" + pathValue;
                        }
                        String prefix = pathValue;
                        if (pathValue.endsWith("/")) {
                            pathValue += "*";
                        } else {
                            pathValue += "/*";
                        }
                        patterns.add(pathValue);

                        setInitParameter(servlet, "resteasy.servlet.mapping.prefix", prefix);
                        mappingSet = true;
                    } else {
                        JAXRS_LOGGER.noServletMappingFound(application.getName());
                        continue;
                    }
                    ServletMappingMetaData mapping = new ServletMappingMetaData();
                    mapping.setServletName(application.getName());
                    mapping.setUrlPatterns(patterns);
                    if (webdata.getServletMappings() == null) {
                        webdata.setServletMappings(new ArrayList<ServletMappingMetaData>());
                    }
                    webdata.getServletMappings().add(mapping);
                }

                if (!mappingSet) {
                    //now we need tell resteasy it's relative path
                    setupMapping(webdata, application.getName(), servlet);
                }
            }
        }
    }

    private void setupMapping(final JBossWebMetaData webdata,  final String servletName, final ServletMetaData servlet) {
        boolean mappingSet = false;
        final List<ServletMappingMetaData> mappings = webdata.getServletMappings();
        if (mappings != null) {
            for (final ServletMappingMetaData mapping : mappings) {
                if (mapping.getServletName().equals(servletName)) {
                    if (mapping.getUrlPatterns() != null) {
                        for (String pattern : mapping.getUrlPatterns()) {
                            if (mappingSet) {
                                JAXRS_LOGGER.moreThanOneServletMapping(servletName, pattern);
                            } else {
                                mappingSet = true;
                                String realPattern = pattern;
                                if (realPattern.endsWith("*")) {
                                    realPattern = realPattern.substring(0, realPattern.length() - 1);
                                }
                                setInitParameter(servlet, "resteasy.servlet.mapping.prefix", realPattern);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addServlet(JBossWebMetaData webdata, JBossServletMetaData servlet) {
        if (webdata.getServlets() == null) {
            webdata.setServlets(new JBossServletsMetaData());
        }
        webdata.getServlets().add(servlet);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    protected void setFilterInitParam(FilterMetaData filter, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = filter.getInitParam();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            filter.setInitParam(params);
        }
        params.add(param);

    }

    public static ParamValueMetaData findContextParam(JBossWebMetaData webdata, String name) {
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null)
            return null;
        for (ParamValueMetaData param : params) {
            if (param.getParamName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    public static JBossServletMetaData findServletWithInitParam(JBossWebMetaData webdata, String name, Class<?> applicationClass) {
        JBossServletsMetaData servlets = webdata.getServlets();
        if (servlets == null)
            return null;
        for (JBossServletMetaData servlet : servlets) {
            List<ParamValueMetaData> initParams = servlet.getInitParam();
            if (initParams != null) {
                for (ParamValueMetaData param : initParams) {
                    if (param.getParamName().equals(name)) {
                        if (param.getParamValue() != null &&
                                param.getParamValue().equals(applicationClass.getName()))
                            return servlet;
                    }
                }
            }
        }
        return null;
    }

    public static boolean servletMappingsExist(JBossWebMetaData webdata, String servletName) {
        List<ServletMappingMetaData> mappings = webdata.getServletMappings();
        if (mappings == null)
            return false;
        for (ServletMappingMetaData mapping : mappings) {
            if (mapping.getServletName().equals(servletName)) {
                return true;
            }
        }
        return false;
    }

    public static void setInitParameter(ServletMetaData servlet, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = servlet.getInitParam();
        if (params == null) {
            params = new ArrayList<ParamValueMetaData>();
            servlet.setInitParam(params);
        }
        params.add(param);
    }
}