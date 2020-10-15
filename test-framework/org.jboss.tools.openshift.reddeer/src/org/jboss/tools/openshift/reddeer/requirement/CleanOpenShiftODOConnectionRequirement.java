/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.lang.StringUtils;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.condition.ODOConnectionExists;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftODOConnectionRequirement.CleanODOConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
/**
 *  Clean OpenShift Application Explorer requirement
 * 
 * @author jkopriva@redhat.com
 */
public class CleanOpenShiftODOConnectionRequirement implements Requirement<CleanODOConnection> {

	private CleanODOConnection cleanODOConnection;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface CleanODOConnection {
		/**
		 * Connection to clean. If nothing is provided {@link DatastoreOS3#SERVER} and
		 * {@link DatastoreOS33#USERNAME} are used.
		 */
		String connectionURL() default StringUtils.EMPTY;
	}

	@Override
	public void fulfill() {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		if (explorer.connectionExistsAndWorking()) {
			explorer.connectToOpenShiftODO(); 
		}
		OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
		connection.refresh();
		for (OpenShiftODOProject project : connection.getAllProjects()) {
			try {
				project.delete();
			} catch (WaitTimeoutExpiredException ex) {
				//delete could throw Delete window, that project has been deleted
				new PushButton(new DefaultShell("Delete"), "OK").click();
			}
		}
	}

	@Override
	public void setDeclaration(CleanODOConnection cleanConnection) {
		this.cleanODOConnection = cleanConnection;
	}

	@Override
	public void cleanUp() {
		// NOTHING TO DO
	}

	@Override
	public CleanODOConnection getDeclaration() {
		return this.cleanODOConnection;
	}

}
