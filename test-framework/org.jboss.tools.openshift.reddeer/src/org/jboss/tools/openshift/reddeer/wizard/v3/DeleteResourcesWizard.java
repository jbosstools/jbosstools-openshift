/*******************************************************************************
 * Copyright (c) 2007-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.wizard.v3;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.swt.api.TableItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.condition.TableHasRows;
import org.eclipse.reddeer.swt.impl.button.CancelButton;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.table.DefaultTable;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.swt.matcher.ColumnTableItemMatcher;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.enums.ResourceOpenShift;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;

/**
 * 
 * @author jkopriva@redhat.com
 */
public class DeleteResourcesWizard {
	
	protected Connection connection;

	public DeleteResourcesWizard(Connection connection) {
		this.connection = connection;
	}
	
	public void openWizardFromExplorer() {
		openWizardFromExplorer(null);
	}

	/**
	 * Opens a new OpenShift 3 Delete Resources... wizard from OpenShift Explorer view with the given project pre-selected.
	 * If the project is null, a generated project is used.
	 * 
	 * @param project project name for deleting resources 
	 */
	public void openWizardFromExplorer(String project) {
		OpenShiftExplorerView explorer = new OpenShiftExplorerView();
		explorer.reopen();

		selectExplorerProject(project, explorer);
	
		new ContextMenuItem(OpenShiftLabel.ContextMenu.DELETE_RESOURCES).select();
		
		new WaitUntil(new ShellIsAvailable(OpenShiftLabel.Shell.DELETE_OS_RESOURCES), TimePeriod.LONG);
		
		new DefaultShell(OpenShiftLabel.Shell.DELETE_OS_RESOURCES);
		
		new WaitUntil(new TableHasRows(new DefaultTable()), TimePeriod.LONG);
	}
	
	public void selectAll() {
		new PushButton(OpenShiftLabel.Button.SELECT_ALL).click();
	}
	
	public void deSelectAll() {
		new PushButton(OpenShiftLabel.Button.DESELECT_ALL).click();
	}
	
	public List<TableItem> getAllResources() {
		return new DefaultTable().getItems();
	}
	
	public List<TableItem> getResourcesByType(ResourceOpenShift resource) {
		return new DefaultTable().getItems(new ColumnTableItemMatcher(1, resource.toString()));
	}
	
	public List<TableItem> getResourceByName(String resourceName) {
		return new DefaultTable().getItems(new ColumnTableItemMatcher(0, resourceName));
	}
	
	public void deleteResourceByName(String resourceName) {
		getResourceByName(resourceName).get(0) .select();
		new WaitUntil(new ControlIsEnabled(new PushButton(OpenShiftLabel.Button.DELETE)));
		delete();
	}
	
	public void delete() {
		new PushButton(OpenShiftLabel.Button.DELETE).click();
		new WaitUntil(new JobIsRunning(), false);
	}

	private void selectExplorerProject(String project, OpenShiftExplorerView explorer) {
		if (StringUtils.isEmpty(project)) {
			explorer.getOpenShift3Connection(this.connection).getProject().select();
		} else {
			explorer.getOpenShift3Connection(this.connection).getProject(project).select();
		}
	}
	
	public void setFilter(String filter) {
		new LabeledText(OpenShiftLabel.TextLabels.LABEL_FILTER).setFocus();
		new LabeledText(OpenShiftLabel.TextLabels.LABEL_FILTER).setText(filter);
		new WaitUntil(new JobIsRunning(), false);
	}

	public void cancel() {
		new CancelButton().click();
	}
}
