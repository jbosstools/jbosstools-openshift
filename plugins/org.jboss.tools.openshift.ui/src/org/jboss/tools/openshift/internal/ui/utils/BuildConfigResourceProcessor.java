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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IResource;

/**
 * Processor for BuildConfig. Delete BuildConfig and related Builds.
 * 
 * @author Jeff Maury
 *
 */
public class BuildConfigResourceProcessor extends BaseResourceProcessor {

    public BuildConfigResourceProcessor() {
        super(true);
    }

    @Override
    public void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource,
                             boolean willDeleteSubResources, IProgressMonitor monitor)
            throws OpenShiftException {
        IBuildConfig bc = (IBuildConfig)resource;

        if (willDeleteSubResources && !monitor.isCanceled()) {
            String paused = bc.getAnnotation(OpenShiftAPIAnnotations.BUILD_CONFIG_PAUSED);
            if (!Boolean.TRUE.toString().equalsIgnoreCase(paused)) {
                bc.setAnnotation(OpenShiftAPIAnnotations.BUILD_CONFIG_PAUSED, Boolean.TRUE.toString());
                bc = connection.updateResource(bc);
            }
            /*
             * get matching builds
             */
            processBuilds(registry, connection, bc.getNamespace(), OpenShiftAPIAnnotations.BUILD_CONFIG_NAME,
                    bc.getName(), willDeleteSubResources, monitor);
            processBuilds(registry, connection, bc.getNamespace(), OpenShiftAPIAnnotations.BUILD_CONFIG_NAME_DEPRECATED,
                    bc.getName(), willDeleteSubResources, monitor);
        }
        super.handleDelete(registry, connection, resource, willDeleteSubResources, monitor);
    }

    /**
     * Process linked builds.
     * 
     * @param registry the connection registry to be notified
     * @param connection the connection
     * @param namespace the namespace to look for
     * @param annotation the annotation to search for
     * @param name the name to match
     * @param willDeleteSubResources if cascade delete
     * @param monitor the progress monitor for the operation
     */
    protected void processBuilds(ConnectionsRegistry registry, Connection connection, String namespace, String annotation, String name,
                                 boolean willDeleteSubResources, IProgressMonitor monitor) {
        if (!monitor.isCanceled()) {
            Map<String, String> selector = new HashMap<String, String>() {
                {
                    put(annotation, name);
                }
            };
            List<IResource> builds = connection.getResources(ResourceKind.BUILD, namespace, selector);
            for (IResource build : builds) {
                ResourceProcessor process = Platform.getAdapterManager().getAdapter(build, ResourceProcessor.class);
                process.handleDelete(registry, connection, build, willDeleteSubResources, monitor);
            } 
        }
    }
}
