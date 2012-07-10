/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.deployment;

import java.util.jar.Manifest;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.spi.BundleInfo;
import org.osgi.framework.BundleContext;

/**
 * @author Stuart Douglas
 */
public class OSGIAttachments {

    /**
     * Attachment key for the {@link BundleInfo} when an OSGi bundle deployment is detected.
     */
    public static final AttachmentKey<BundleInfo> BUNDLE_INFO = AttachmentKey.create(BundleInfo.class);
    /**
     * Attachment key for the OSGi system context.
     */
    public static final AttachmentKey<BundleContext> SYSTEM_CONTEXT = AttachmentKey.create(BundleContext.class);

    /**
     * Attachment key for the installed {@link XBundle}.
     */
    public static final AttachmentKey<XBundle> INSTALLED_BUNDLE = AttachmentKey.create(XBundle.class);


    /** Attachment key for {@link OSGiMetaData} */
    public static final AttachmentKey<OSGiMetaData> OSGI_METADATA = AttachmentKey.create(OSGiMetaData.class);

    /**
     * Available when the deployment contains a valid OSGi manifest
     */
    public static final AttachmentKey<Manifest> OSGI_MANIFEST = AttachmentKey.create(Manifest.class);
}
