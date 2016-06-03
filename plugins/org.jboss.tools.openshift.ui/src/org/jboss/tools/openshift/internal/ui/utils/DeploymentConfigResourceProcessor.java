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

import org.eclipse.core.runtime.Platform;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenshiftUIConstants;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IResource;

/**
 * Processor for DeploymentConfig. Delete DeploymentConfig and related ReplicationControllers.
 * 
 * @author Jeff Maury
 *
 */
public class DeploymentConfigResourceProcessor extends BaseResourceProcessor {

    public DeploymentConfigResourceProcessor() {
        super(true);
    }

    @Override
    public void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource,
                             boolean willDeleteSubResources)
            throws OpenShiftException {
        IDeploymentConfig dc = (IDeploymentConfig)resource;

        super.handleDelete(registry, connection, resource, willDeleteSubResources);
        if (willDeleteSubResources) {
            Map<String, String> selector = new HashMap<String, String>() {
                {
                    put(OpenshiftUIConstants.DEPLOYMENT_CONFIG_ANNOTATION, resource.getName());
                }
            };
            List<IResource> replicationControllers = connection.getResources(ResourceKind.REPLICATION_CONTROLLER,
                    dc.getNamespace(), selector);
            for (IResource rc : replicationControllers) {
                ResourceProcessor process = Platform.getAdapterManager().getAdapter(rc, ResourceProcessor.class);
                process.handleDelete(registry, connection, rc, willDeleteSubResources);
            } 
        }
    }
}
