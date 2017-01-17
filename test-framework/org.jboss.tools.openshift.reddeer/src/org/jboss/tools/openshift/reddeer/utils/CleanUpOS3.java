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
package org.jboss.tools.openshift.reddeer.utils;

import org.jboss.reddeer.common.exception.RedDeerException;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.core.condition.JobIsRunning;
import org.jboss.tools.openshift.reddeer.view.OpenShiftExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShift3Connection;
import org.junit.After;
import org.junit.Test;

/** 
 * This "test" perform clean up. Clean up consists of deletion of projects 
 * for an existing OpenShift 3 connection.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class CleanUpOS3 {

	@Test
	public void test() {
		// NOTHING TO DO
	}
	
	@After
	public void cleanUp() {
		try {
			OpenShiftExplorerView explorer = new OpenShiftExplorerView();
			explorer.open();
			OpenShift3Connection connection = explorer.getOpenShift3Connection();
	
			if (connection != null) {
				safeDeleteProject(DatastoreOS3.PROJECT1_DISPLAYED_NAME, connection);
				safeDeleteProject(DatastoreOS3.PROJECT2, connection);

				new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
			}
		} catch(RedDeerException e) {
			// swallow intentionally
		}
	}

	private void safeDeleteProject(String projectName, OpenShift3Connection connection) {
		try {
			connection.refresh();
			connection.getProject(projectName).delete();
		} catch (RedDeerException e) {
			// swallow intentionally
		}
	}
}
