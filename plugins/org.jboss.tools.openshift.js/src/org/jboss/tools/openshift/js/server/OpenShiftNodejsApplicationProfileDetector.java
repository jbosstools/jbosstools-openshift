/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.js.server;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.ProjectUtils;
import org.jboss.tools.openshift.core.server.adapter.IOpenshiftServerAdapterProfileDetector;
import org.jboss.tools.openshift.core.server.adapter.ProjectBuilderTypeDetector;

import com.openshift.restclient.model.IResource;

public class OpenShiftNodejsApplicationProfileDetector implements IOpenshiftServerAdapterProfileDetector {

    public static final String PROFILE = "openshift3.nodejs";
    private static final String TEMPLATES_TAG_NODEJS = "node";

    public OpenShiftNodejsApplicationProfileDetector() {
        super();
    }

    @Override
    public String getProfile() {
        return PROFILE;
    }

    @Override
    public boolean detect(IConnection connection, IResource resource, IProject eclipseProject) {
        return ProjectUtils.isAccessible(eclipseProject)
                && TEMPLATES_TAG_NODEJS.equals(new ProjectBuilderTypeDetector().findTemplateFilter(eclipseProject));
    }

}
