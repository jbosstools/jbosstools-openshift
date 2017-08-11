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
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import static org.junit.Assert.assertEquals;

import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesView;
import org.jboss.reddeer.junit.runner.RedDeerSuite;
import org.jboss.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(RedDeerSuite.class)
@OpenPerspective(JBossPerspective.class)
@RequiredBasicConnection()
public class ConnectionPropertiesTest {
	
	private static final String PROPERTY_USERNAME = "User Name";
	private static final String PROPERTY_HOST = "Host";

	@Test
	public void testConnectionProperties() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		OpenShift3Connection connection = explorer.getOpenShift3Connection();
		connection.select();
		
		new ContextMenu(OpenShiftLabel.ContextMenu.PROPERTIES).select();
		
		PropertiesView propertiesView = new PropertiesView();
		propertiesView.activate();
		
		assertEquals("Property host is not valid. Was '" + propertiesView.getProperty(PROPERTY_HOST).getPropertyValue() 
				+ "' but was expected '" + DatastoreOS3.SERVER + "'", 
				DatastoreOS3.SERVER, 
				propertiesView.getProperty(PROPERTY_HOST).getPropertyValue());
		assertEquals("Property user name inot valid. Was '" + propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue() 
				+ "' but was expected '" + DatastoreOS3.USERNAME + "'", 
				DatastoreOS3.USERNAME, 
				propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue());
	}
}
