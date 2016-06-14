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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;

/**
 * Processor for ReplicationControllerResourceProcessor. Scale ReplicationController to 0 and delete it.
 * 
 * @author Jeff Maury
 *
 */
public class ReplicationControllerResourceProcessor extends BaseResourceProcessor {

    private static final long REPLICATION_CONTROLLER_UPDATE_DELAY = 500;
    private static final long REPLICATION_CONTROLLER_UPDATE_MAX_DELAY = 5000;

    public ReplicationControllerResourceProcessor() {
        super(true);
    }
    
    @Override
    public void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource,
                             boolean willDeleteSubResources, IProgressMonitor monitor)
            throws OpenShiftException {
        IReplicationController rc = (IReplicationController)resource;

            if (willDeleteSubResources && !monitor.isCanceled()) {
                List<IReplicationController>[] overlappingRCs = ResourceUtils.getOverlappingReplicationControllers(rc);
                if (!overlappingRCs[1].isEmpty()) {
                    String list = overlappingRCs[1].stream().map(res -> res.getName()).collect(Collectors.joining(","));
                    throw new OpenShiftException(
                            "Found overlapping replication controllers for replication controller %s: %s", rc.getName(),
                            list);
                } else if ((overlappingRCs[0].size() == 1) && !monitor.isCanceled()) {
                    rc.setDesiredReplicaCount(0);
                    rc = connection.updateResource(rc);
                    long elapsed = 0;
                    while ((rc.getCurrentReplicaCount() > 0) && !monitor.isCanceled() && (elapsed < REPLICATION_CONTROLLER_UPDATE_MAX_DELAY)) {
                        try {
                            Thread.sleep(REPLICATION_CONTROLLER_UPDATE_DELAY);
                            elapsed += REPLICATION_CONTROLLER_UPDATE_DELAY;
                        } catch (InterruptedException e) {
                        } finally {
                            rc = connection.getResource(rc);
                        }
                    }
                } 
            }
            super.handleDelete(registry, connection, resource, willDeleteSubResources, monitor);
    }
}
