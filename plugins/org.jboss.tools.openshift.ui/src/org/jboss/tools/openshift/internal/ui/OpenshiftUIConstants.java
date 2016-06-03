/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui;

import org.apache.commons.validator.routines.UrlValidator;
import org.jboss.tools.foundation.core.ecf.URLTransportUtility;

/**
 * @author Jeff Maury
 *
 */
public final class OpenshiftUIConstants {
    /*
     * An UrlValidator that recognize strings as urls with http, https, ftp and file schemes.
     */
    public static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[] {"http", "https", "ftp", "file"});
    
    /**
     * A bundle common transport utility for easier ECF use for downloads.
     */
    public static final URLTransportUtility TRANSPORT_UTILITY = new URLTransportUtility();
    
    /**
     * Name of the annotation used to link a ReplicationController to its DeploymentConfig.
     */
    public static final String DEPLOYMENT_CONFIG_ANNOTATION = "openshift.io/deployment-config.name";
    
    /**
     * Name of the annotation used to mark a BuildConfig under deletion
     */
    public static final String  BUILD_CONFIG_PAUSED_ANNOTATION = "openshift.io/build-config.paused";
    
    /**
     * Label used to stored the name of the build config in a build.
     */
    public static final String BUILD_CONFIG_LABEL = "openshift.io/build-config.name";
    
    /**
     * Label used to stored the name of the build config in a build. Deprecated one.
     */
    public static final String BUILD_CONFIG_LABEL_DEPRECATED = "buildconfig";
}
