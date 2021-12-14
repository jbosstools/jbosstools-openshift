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
package org.jboss.tools.openshift.ui.bot.test.connection.v3;

import static org.junit.Assert.assertEquals;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.common.util.ResultRunnable;
import org.eclipse.reddeer.core.handler.WidgetHandler;
import org.eclipse.reddeer.eclipse.ui.views.properties.PropertySheet;
import org.eclipse.reddeer.junit.requirement.inject.InjectRequirement;
import org.eclipse.reddeer.junit.runner.RedDeerSuite;
import org.eclipse.reddeer.swt.api.CTabItem;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.keyboard.Keyboard;
import org.eclipse.reddeer.swt.keyboard.KeyboardFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Widget;
import org.jboss.tools.openshift.reddeer.enums.AuthenticationMethod;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftConnectionRequirement.RequiredBasicConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.jboss.tools.openshift.ui.bot.test.application.v3.basic.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RequiredBasicConnection
@RunWith(RedDeerSuite.class)
public class ConnectionPropertiesTest extends AbstractTest {
	@InjectRequirement
	private OpenShiftConnectionRequirement connectionReq;

	private static final String PROPERTY_USERNAME = "User Name";
	private static final String PROPERTY_HOST = "Host";

	@Test
	public void testConnectionProperties() {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.open();
		
		Keyboard keyboard = KeyboardFactory.getKeyboard();
		keyboard.invokeKeyCombination(SWT.F3);

		OpenShift3Connection connection = explorer.getOpenShift3Connection(connectionReq.getConnection());
		connection.select();

		new ContextMenuItem(OpenShiftLabel.ContextMenu.PROPERTIES).select();

		PropertySheet propertiesView = new PropertySheet();
		propertiesView.activate();

		assertEquals(
				"Property host is not valid. Was '" + propertiesView.getProperty(PROPERTY_HOST).getPropertyValue()
						+ "' but was expected '" + DatastoreOS3.SERVER + "'",
				DatastoreOS3.SERVER, propertiesView.getProperty(PROPERTY_HOST).getPropertyValue());
		if (DatastoreOS3.AUTH_METHOD.equals(AuthenticationMethod.BASIC)) {
			assertEquals(
					"Property user name is not valid. Was '"
							+ propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue() + "' but was expected '"
							+ DatastoreOS3.USERNAME + "'",
					DatastoreOS3.USERNAME, propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue());
		} else {
			assertEquals(
					"Property user name is not valid. Was '"
							+ propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue() + "' but was expected '"
							+ connectionReq.getConnection().getUsername() + "'",
							connectionReq.getConnection().getUsername(), propertiesView.getProperty(PROPERTY_USERNAME).getPropertyValue());
		}
	}
	
	public void click(final Point point, Widget swtWidget) {
		Event e = createEvent(swtWidget);
		Point p = point;
		e.x = p.x;
		e.y = p.y;
		WidgetHandler.getInstance().notifyWidget(SWT.MouseDoubleClick, e, swtWidget);
	}
	
	private Event createEvent(Widget widget) {
		Event event = new Event();
		event.time = (int) System.currentTimeMillis();
		event.widget = widget;
		event.display = Display.getDisplay();
		event.type = SWT.MouseUp;
		event.button = 1;
		return event;
	}
}
