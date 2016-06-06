/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.jboss.tools.openshift.internal.ui.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.internal.core.WatchManager;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.http.IHttpConstants;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * Processor for Projet. Delete Project and wait to be deleted.
 * 
 * @author Jeff Maury
 *
 */
public class ProjectResourceProcessor extends BaseResourceProcessor {

    private static final long PROJECT_DELETE_DELAY = 500;
    private static final long MAX_PROJECT_DELETE_DELAY = 5000;

    public ProjectResourceProcessor() {
        super(false);
    }

    @Override
    public void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource,
            boolean willDeleteSubResources, IProgressMonitor monitor) throws OpenShiftException {
        IProject project = (IProject) resource;

        WatchManager.getInstance().stopWatch(project);
        if (!monitor.isCanceled()) {
            List<IProject> oldProjects = connection.getResources(ResourceKind.PROJECT);
            if (!monitor.isCanceled()) {
                connection.deleteResource(project);
                if (waitForServerToReconcileProjectDelete(connection, project, monitor)) {
                    List<IProject> newProjects = new ArrayList<>(oldProjects);
                    newProjects.remove(project);
                    registry.fireConnectionChanged(connection, ConnectionProperties.PROPERTY_PROJECTS, oldProjects,
                            newProjects);
                } 
            } 
        }
    }

    /**
     * Checks if the Openshift project is deleted.
     * 
     * @param conn the Openshift connection
     * @param project the Openshift project to check deletion
     * @param monitor the operation monitor
     * @return true if the project does not exist anymore
     */
    private boolean waitForServerToReconcileProjectDelete(Connection conn, IProject project, IProgressMonitor monitor) {
        boolean deleted = false;
        long sleep = 0;
        do {
            try {
                conn.getResource(project);
                Thread.sleep(PROJECT_DELETE_DELAY);
            } catch (InterruptedException e1) {
            } catch (OpenShiftException ex) {
                if (ex.getStatus() != null) {
                    final int code = ex.getStatus().getCode();
                    if (code == IHttpConstants.STATUS_NOT_FOUND || code == IHttpConstants.STATUS_FORBIDDEN) {
                        deleted = true;
                    }
                }
            } finally {
                sleep = sleep + PROJECT_DELETE_DELAY;
            }
        } while (!deleted && sleep < MAX_PROJECT_DELETE_DELAY && !monitor.isCanceled());
        return deleted;
    }
}
