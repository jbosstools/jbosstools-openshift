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
package org.jboss.tools.openshift.reddeer.view.resources;

import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.reddeer.core.condition.ShellWithTextIsAvailable;
import org.jboss.reddeer.eclipse.ui.views.properties.PropertiesView;
import org.jboss.reddeer.swt.api.TableItem;
import org.jboss.reddeer.swt.exception.SWTLayerException;
import org.jboss.reddeer.swt.impl.button.OkButton;
import org.jboss.reddeer.swt.impl.menu.ContextMenu;
import org.jboss.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.reddeer.swt.impl.table.DefaultTable;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;

/**
 * OpenShift resource. This class represents build config, build, deployment config, image stream,
 * pod, replication controller, route, service or pod in properties view.
 * 	
 * @author mlabuda@redhat.com
 *
 */
public class OpenShiftResource {

	private TableItem tableItem;
	
	public OpenShiftResource(TableItem tableItem) {
		this.tableItem = tableItem;
	}
	
	/**
	 * Gets name of OpenShift resource.
	 * @return name of OpenShift resource
	 */
	public String getName() {
		return getColumnText("Name");
	}
	
	/**
	 * Gets status of OpenShift resource (pod, build...) if it exists.
	 * 
	 * @return status of resource, null if it does not exists
	 */
	public String getStatus() {
		return getColumnText("Status");
	}
	
	/**
	 * Gets property value for the OpenShift resource.
	 * 
	 * @param propertyPath path to property with its name
	 * @return property value
	 */
	public String getPropertyValue(String... propertyPath) {
		tableItem.select();
		return new PropertiesView().getProperty(propertyPath).getPropertyValue();
	}
	
	protected String getColumnText(String columnHeader) {
		int index = -1; 
		try {
			index = new DefaultTable().getHeaderIndex(columnHeader);
		} catch (SWTLayerException ex) { }
		
		if (index >= 0) {
			return tableItem.getText(index);
		} else {
			return null;
		}
	}
	
	/**
	 * Selects resource.
	 */
	public void select() {
		tableItem.click();
		tableItem.select();
	}
	
	/**
	 * Deletes resource via properties view.
	 */
	public void delete() {
		select();
		new ContextMenu(OpenShiftLabel.ContextMenu.DELETE_RESOURCE).select();
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_RESOURCE);
		new OkButton().click();
		
		new WaitWhile(new ShellWithTextIsAvailable(OpenShiftLabel.Shell.DELETE_RESOURCE), TimePeriod.LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
	}
	
}
