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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistry;
import org.jboss.tools.openshift.core.connection.Connection;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;

/**
 * Processor for ReplicationControllerResourceProcessor. Scale ReplicationController to 0 and delete it.
 * 
 * @author Jeff Maury
 *
 */
public class ReplicationControllerResourceProcessor extends BaseResourceProcessor {

    public ReplicationControllerResourceProcessor() {
        super(true);
    }
    
    @Override
    public void handleDelete(ConnectionsRegistry registry, Connection connection, IResource resource, boolean willDeleteSubResources)
            throws OpenShiftException {
        IReplicationController rc = (IReplicationController)resource;

        try {
            if (willDeleteSubResources) {
                List<IResource>[] overlappingRCs = findOverlappingReplicationControllers(connection, rc);
                if (!overlappingRCs[1].isEmpty()) {
                    String list = overlappingRCs[1].stream().map(res -> res.getName()).collect(Collectors.joining(","));
                    throw new OpenShiftException(
                            "Found overlapping replication controllers for replication controller %s: %s", rc.getName(),
                            list);
                } else if (overlappingRCs[0].size() == 1) {
                    rc.setDesiredReplicaCount(0);
                    rc = connection.updateResource(rc);
                    while (rc.getCurrentReplicaCount() > 0) {
                        Thread.sleep(1000);
                        rc = connection.getResource(rc);
                    }
                } 
            }
            super.handleDelete(registry, connection, resource, willDeleteSubResources);
        } catch (InterruptedException e) {
            throw new OpenShiftException(e, e.getLocalizedMessage());
        }
    }

    private List<IResource>[] findOverlappingReplicationControllers(Connection connection, IReplicationController replicationController) {
        List[] overlappingRCS = new List[2];
        overlappingRCS[0] = new ArrayList<>();
        overlappingRCS[1] = new ArrayList<>();
        List<IResource> rcs = connection.getResources(ResourceKind.REPLICATION_CONTROLLER, replicationController.getNamespace());
        for(IResource rc : rcs) {
            if (match((IReplicationController)rc, replicationController)) {
                if (((IReplicationController)rc).getReplicaSelector().size() == replicationController.getReplicaSelector().size()) {
                    overlappingRCS[0].add(rc);
                } else {
                    overlappingRCS[1].add(rc);
                }
            }
        }
        return overlappingRCS;
    }
    
    private boolean match(IReplicationController rc, IReplicationController replicationController) {
        return match(rc.getReplicaSelector(), replicationController.getReplicaSelector()) |
               match(replicationController.getReplicaSelector(), rc.getReplicaSelector());
    }

    /**
     * Find if maps matches. Maps matches if map1 is contained in map2
     * @param map1 the first map
     * @param map2 the second map
     * @return the match state
     */
    private boolean match(Map<String, String> map1, Map<String, String> map2) {
        for(Map.Entry<String, String> entry : map1.entrySet()) {
            String value = map2.get(entry.getKey());
            if (!entry.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }
}
