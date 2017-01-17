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

import org.hamcrest.Matcher;
import org.jboss.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesView;
import org.jboss.tools.openshift.reddeer.enums.Resource;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftResource;


/**
 * Wait condition to wait until a specific property value of a resource 
 * is updated.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class ResourceIsUpdated extends AbstractWaitCondition {
	
	private OpenShiftResource resource;
	
	private String[] propertyName;
	private Matcher<String> propertyValueMatcher;
	
	/**
	 * Constructor for waiting for a update of specific resource property value.
	 * 
	 * @param project project where resource is located
	 * @param resourceType resource type
	 * @param resourceName resource name
	 * @param propertyName resource property name
	 * @param propertyValueMatcher resource property value matcher
	 */
	public ResourceIsUpdated(String project, Resource resourceType, String resourceName,
			String[] propertyName, Matcher<String> propertyValueMatcher) {

		this.propertyName = propertyName;
		this.propertyValueMatcher = propertyValueMatcher;
		
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		new PropertiesView().open();
		explorer.open();
		List<OpenShiftResource> resources = explorer.getOpenShift3Connection().
				getProject(project).getOpenShiftResources(resourceType, true);
		for (OpenShiftResource resource: resources) {
			if (resource.getName().equals(resourceName)) {
				this.resource = resource;
				break;
			}
		}
	}
	
	@Override
	public boolean test() {
		resource.select();
		return propertyValueMatcher.matches(resource.getPropertyValue(propertyName));
	}
}
