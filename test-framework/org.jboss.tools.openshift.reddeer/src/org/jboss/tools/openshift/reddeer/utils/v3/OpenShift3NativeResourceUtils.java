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
package org.jboss.tools.openshift.reddeer.utils.v3;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.reddeer.common.logging.Logger;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;

import com.openshift.restclient.NotFoundException;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.authorization.ResourceForbiddenException;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * @author adietish@redhat.com
 */
public class OpenShift3NativeResourceUtils {
	
	private static final Logger LOGGER = new Logger(OpenShift3NativeResourceUtils.class);

	/**
	 * Returns the project with the given name that exists on the server that
	 * the given connection points to. Returns {@code null} if none was found.
	 * 
	 * @param name
	 * @param connection
	 * @return
	 */
	public static IProject getProject(String name, Connection connection) {
		assertNotNull(connection);

		if (StringUtils.isBlank(name)) {
			return null;
		}

		return safeGetResource(ResourceKind.PROJECT, name, null, connection);
	}

	/**
	 * Returns {@code true} if the given connection has a project with the given name.
	 * 
	 * @param name the name of the project that is looked for
	 * @param connection the connection to use to look up the project
	 * @return
	 */
	public static boolean hasProject(String name, Connection connection) {
		assertNotNull(connection);

		return getProject(name, connection) != null;
	}

	/**
	 * Returns {@code true} if the resource of the given kind and name exists in the given project.
	 * 
	 * @param kind the resource kind
	 * @param name the name of the resource
	 * @param projectName the name of the project for the resource
	 * @param connection the connection to use for the lookup
	 * @return
	 */
	public static boolean resourceExists(String kind, String name, String projectName, Connection connection) {
		return safeGetResource(kind, name, projectName, connection) != null;
	}
	
	/**
	 * Returns the resource that matches the given kind, name and given namespace using the given connection. 
	 * If it doesnt exist [{@code null} is returned.
	 * 
	 * @param kind the kind of resource 
	 * @param name the resource name
	 * @param projectName the namespace that it's living in
	 * @param connection the connection to use for the lookup
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IResource> T safeGetResource(String kind, String name, String projectName, Connection connection) {
		assertNotNull(connection);

		try {
			return (T) connection.getResource(kind, projectName, name);
		} catch(NotFoundException | ResourceForbiddenException e) {
			LOGGER.debug(NLS.bind("No {0} named {1} was found in project {2} on server {3}",
					new Object[] { kind, name, projectName, connection.getHost() } ));
			return null;
		}
	}
	
	/**
	 * Returns the pods for the given service using the given connection.
	 * 
	 * @param service the service to get the pods for
	 * @param connection the connection to use to look up the pods
	 * @return
	 */
	public static Collection<IPod> getPods(IService service, Connection connection) {
		assertNotNull(connection);
		
		List<IPod> allPods = connection.getResources(ResourceKind.POD, service.getNamespace());
		return ResourceUtils.getPodsFor(service, allPods);
	}
 }
