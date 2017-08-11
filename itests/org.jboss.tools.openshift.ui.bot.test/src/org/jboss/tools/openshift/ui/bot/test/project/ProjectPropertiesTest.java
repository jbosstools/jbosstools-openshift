/*******************************************************************************
 * Copyright (c) 2007-2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.jboss.reddeer.core.exception.CoreLayerException;
import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesView;
import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesViewProperty;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.swt.api.TreeItem;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftServiceRequirement.RequiredService;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftResources;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RequiredBasicConnection
@RequiredProject
@RequiredService(project=DatastoreOS3.TEST_PROJECT, 
		service = OpenShiftResources.NODEJS_SERVICE, 
		template = OpenShiftResources.NODEJS_TEMPLATE)
@RunWith(RedDeerSuite.class)
public class ProjectPropertiesTest {

	private static final String[] BASIC_PROPERTIES =
			{"Creation Timestamp", "Kind", "Name", "Namespace", "Resource Version"};
	private static final String[] BASIC_TABS =
			{"Details", "Builds", "Build Configs", "Deployments",
			"Deployment Configs", "Image Streams", "Pods", "Routes", "Services"};

	private PropertiesView propertiesView;
	private OpenShiftProject project;
	
	@Before
	public void setUp() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		OpenShift3Connection connection = explorer.getOpenShift3Connection();
		project = connection.getProject(DatastoreOS3.TEST_PROJECT);
		project.select();
		project.openProperties();
		
		propertiesView = new PropertiesView();
	}

	@Test
	public void testProjectNameProperties() {
		project.selectTabbedProperty("Details");

		String displayedName = propertiesView.getProperty("Annotations", "openshift.io/display-name").getPropertyValue();
		String name = propertiesView.getProperty("Basic", "Name").getPropertyValue();
		String namespace = propertiesView.getProperty("Basic", "Namespace").getPropertyValue();
		
		assertTrue("Property displayed name is not correct. Property is " + displayedName +
				" but was expected " + DatastoreOS3.TEST_PROJECT, 
				displayedName.equals(DatastoreOS3.TEST_PROJECT)); 
		assertTrue("Property name is not correct. Property is " + name
				 + " but was expected " + DatastoreOS3.TEST_PROJECT, name.equals(DatastoreOS3.TEST_PROJECT));
		assertTrue("Property namespace is not correct. Property is " + namespace 
				 + " but was expected " + DatastoreOS3.TEST_PROJECT, namespace.equals(DatastoreOS3.TEST_PROJECT));
	}

	@Test
	public void testTabs() {
		for (String tabName : BASIC_TABS) {
			project.selectTabbedProperty(tabName);
			assertBasicProperties();
			assertAnnotationProperties();
		}
	}

	private void assertBasicProperties() {
		for (String propertyName : BASIC_PROPERTIES) {
			assertPropertyNotEmpty("Basic", propertyName);
		}
	}
	
	private void assertAnnotationProperties() {
		for (TreeItem property : getCurrentAnnotationProperties()) {
			assertPropertyNotEmpty(property.getPath());
		}
	}
	
	/**
	 * Asserts that property with specified path exists and has a non-empty value.
	 * @param path path to property
	 */
	private void assertPropertyNotEmpty(String... path) {
		String errorMessage = "Property " + path[path.length - 1] + " should be present";
		try {
			String propertyValue = propertiesView.getProperty(path).getPropertyValue();
			assertNotNull(errorMessage, propertyValue);
		} catch (CoreLayerException e) {
			fail(errorMessage + System.lineSeparator() + e.getMessage());
		}
	}

	private List<TreeItem> getCurrentAnnotationProperties() {
		try {
			PropertiesViewProperty annotationPropertiesRoot = propertiesView.getProperty("Annotations");
			List<TreeItem> annotationProperties = annotationPropertiesRoot.getTreeItem().getItems();
			assertTrue("There should be some annotation properties in the tab", 
				annotationProperties != null && annotationProperties.size() > 0);
			
			return annotationProperties;
		} catch (CoreLayerException e) {
			fail(e.getMessage());
			return null;
		}
	}
	
}
