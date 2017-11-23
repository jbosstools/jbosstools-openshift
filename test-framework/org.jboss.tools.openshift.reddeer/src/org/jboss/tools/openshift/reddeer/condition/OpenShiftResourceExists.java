/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.condition;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.enums.ResourceState;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;

/**
 * Wait condition to wait for existence of an OpenShift resource.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShiftResourceExists extends AbstractWaitCondition {

	private final OpenShiftExplorerView explorer;
	private OpenShiftProject project;
	private final Matcher<String> resourceNameMatcher;
	private final ResourceState resourceState;
	private final Resource resource;
	private final Connection connection;
	
	/**
	 * Creates new ResourceExists to wait for existence of any resource of specified type for
	 * default connection and project.
	 * 
	 * @param resource resource type
	 * @param connection connection to OpenShift
	 */
	public OpenShiftResourceExists(Resource resource, Connection connection) {
		this(resource, (Matcher<String>) null, ResourceState.UNSPECIFIED, connection);
	}
	
	/**
	 * Creates new ResourceExists to wait for existence of a resource of specified type
	 * matching specified resource name for default connection and project
	 * 
	 * @param resource resource type
	 * @param resourceName resource name
	 * @param connection connection to OpenShift
	 */
	public OpenShiftResourceExists(Resource resource, String resourceName, Connection connection) {
		this(resource, new WithTextMatcher(resourceName), ResourceState.UNSPECIFIED, connection);
	}
	
	/**
	 * Creates new ResourceExists to wait for existence of a resource of specified type
	 * matching specified resource name and in specified state for default connection
	 * and project.
	 * 
	 * @param resource resource type
	 * @param resourceName resource name
	 * @param resourceState state of a resource
	 * @param connection connection to OpenShift
	 */
	public OpenShiftResourceExists(Resource resource, String resourceName, ResourceState resourceState, Connection connection) {
		this(resource, new WithTextMatcher(resourceName), resourceState, null, connection);
	}
	
	public OpenShiftResourceExists(Resource resource, String resourceName, ResourceState resourceState, String projectName, Connection connection) {
		this(resource, new WithTextMatcher(resourceName), resourceState, projectName, connection);
	}

	/**
	 * Creates new ResourceExists to wait for existence of a resource of specified type
	 * matching specified resource name matcher for default connection and project.
	 * 
	 * @param resource resource type
	 * @param nameMatcher resource name matcher
	 * @param connection connection to OpenShift
	 */
	public OpenShiftResourceExists(Resource resource, Matcher<String> nameMatcher, Connection connection) {
		this(resource, nameMatcher, ResourceState.UNSPECIFIED, connection);
	}
		
	/**
	 * Creates new ResourceExists to wait for existence of a resource of specified type
	 * matching specified resource name matcher and in specified state for 
	 * default connection and project.
	 *
	 * @param resource resource type
	 * @param nameMatcher resource name matcher
	 * @param resourceState state of a resource
	 * @param connection connection to OpenShift
	 */
	public OpenShiftResourceExists(Resource resource, Matcher<String> nameMatcher, ResourceState resourceState, Connection connection) {
		this(resource, nameMatcher, resourceState, null, connection);
	}

	public OpenShiftResourceExists(Resource resource, Matcher<String> nameMatcher, ResourceState resourceState, String projectName, Connection connection) {
		this.explorer = new OpenShiftExplorerView();
		this.project = getProjectOrDefault(projectName, explorer, connection);
		this.resourceNameMatcher = nameMatcher;
		this.resourceState = resourceState;
		this.resource = resource;
		this.connection = connection;
	}

	private OpenShiftProject getProjectOrDefault(String projectName, OpenShiftExplorerView explorer, Connection connection) {
		if (StringUtils.isEmpty(projectName)) {
			return explorer.getOpenShift3Connection(connection).getProject();
		} else {
			return explorer.getOpenShift3Connection(connection).getProject(projectName);
		}
	}

		@Override
	public boolean test() {
		// workaround for disposed widget
		if (project.getTreeItem().isDisposed()) {
			this.project = explorer.getOpenShift3Connection(connection).getProject(project.getName());
		}
		
		this.project.refresh();

		List<OpenShiftResource> resources = getResources();
		for (OpenShiftResource rsrc: resources) {
			if (resourceNameMatcher == null) {
				return true;
			}
			if (resourceNameMatcher.matches(rsrc.getName())) {
				if (!resourceState.equals(ResourceState.UNSPECIFIED)) {
					return resourceState.toString().equals(rsrc.getStatus());
				}
				return true;
			}
		}
		return false;
	}

		private List<OpenShiftResource> getResources() {
			List<OpenShiftResource> resources;
			try {
				resources = project.getOpenShiftResources(resource);
			} catch (CoreLayerException ex) {
				// In case widget is still disposed... what the heck?!
				OpenShift3Connection openshiftConnection = explorer.getOpenShift3Connection(this.connection);
				openshiftConnection.refresh();
				resources = openshiftConnection.getProject(project.getName()).getOpenShiftResources(resource);
			}
			return resources;
		}

	@Override
	public String description() {
		String matcherDescription = resourceNameMatcher == null ? "" : " matching resource name matcher " + 
			resourceNameMatcher.toString(); 
		return "Waiting for resource " + resource.toString() + matcherDescription;  
	}
}
