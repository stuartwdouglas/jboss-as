/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.cmp.processors;

import javax.sql.DataSource;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.component.CmpEntityBeanComponentCreateService;
import org.jboss.as.cmp.component.CmpEntityBeanComponentDescription;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCStoreManager;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCRelationshipRoleMetaData;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DependencyConfigurator;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class CmpStoreManagerProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        final Catalog catalog = new Catalog();  // One per deployment

        for (final ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            if (component instanceof CmpEntityBeanComponentDescription) {

                component.getConfigurators().add(new ComponentConfigurator() {
                    public void configure(final DeploymentPhaseContext context, final ComponentDescription description, final ComponentConfiguration configuration) throws DeploymentUnitProcessingException {
                        final CmpEntityBeanComponentDescription componentDescription = (CmpEntityBeanComponentDescription) description;

                        final JDBCEntityMetaData entityMetaData = componentDescription.getEntityMetaData();

                        final JDBCStoreManager storeManager = new JDBCStoreManager(context.getDeploymentUnit(), entityMetaData, new CmpConfig(), catalog);
                        final ServiceName serviceName = component.getServiceName().append("jdbc", "store-manager");
                        final ServiceBuilder<?> builder = context.getServiceTarget().addService(serviceName, storeManager);
                        addDataSourceDependency(builder, storeManager, entityMetaData.getDataSourceName());
                        for(JDBCRelationshipRoleMetaData roleMetaData : entityMetaData.getRelationshipRoles()) {
                            addDataSourceDependency(builder, storeManager, roleMetaData.getRelationMetaData().getDataSourceName());
                        }
                        builder.install();

                        configuration.getCreateDependencies().add(new DependencyConfigurator<Service<Component>>() {
                            public void configureDependency(final ServiceBuilder<?> serviceBuilder, final Service<Component> service) throws DeploymentUnitProcessingException {
                                final CmpEntityBeanComponentCreateService createService = (CmpEntityBeanComponentCreateService)service;
                                serviceBuilder.addDependency(serviceName, JDBCEntityPersistenceStore.class, createService.getStoreManagerInjector());
                            }
                        });
                    }
                });

            }
        }
    }

    private void addDataSourceDependency(final ServiceBuilder<?> builder, final JDBCStoreManager storeManager, final String dataSourceName) {
        builder.addDependency(AbstractDataSourceService.SERVICE_NAME_BASE.append(dataSourceName), DataSource.class, storeManager.getDataSourceInjector(dataSourceName));
    }

    public void undeploy(DeploymentUnit context) {
    }
}
