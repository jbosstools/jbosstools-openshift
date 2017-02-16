/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition.core;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IService;

/**
 * Condition that is fullfilled if pods for the given service (within the given project dont exist.
 * 
 * @author adietish@redhat.com
 *
 */
public class ServicePodsExist extends AbstractWaitCondition {

	private final String serviceName;
	private String projectName;
	private Connection connection;
	
	public ServicePodsExist(String serviceName, String projectName, Connection connection) {
		assertNotNull(serviceName);
		assertNotNull(projectName);
		assertNotNull(connection);

		this.serviceName = serviceName;
		this.projectName = projectName;
		this.connection = connection;
	}

	@Override
	public boolean test() {
		IService service = connection.getResource(ResourceKind.SERVICE, projectName, serviceName);
		if (service == null) {
			return true;
		}

		if (!hasDesiredReplicas(service)) {
			return true;
		}

		List<IPod> pods = connection.getResources(ResourceKind.POD, projectName);
		return !ResourceUtils.getPodsFor(service, pods).isEmpty();
	}

	private boolean hasDesiredReplicas(IService service) {
		List<IReplicationController> allReplicationControllers = 
				connection.getResources(ResourceKind.REPLICATION_CONTROLLER, service.getNamespace());
		IReplicationController rc = ResourceUtils.getReplicationControllerFor(service, allReplicationControllers);
		return rc != null 
				&& rc.getDesiredReplicaCount() > 0;
	}

	@Override
	public String description() {
		return "pods for service " + serviceName + " in project " + projectName + " exist.";
	}
}
