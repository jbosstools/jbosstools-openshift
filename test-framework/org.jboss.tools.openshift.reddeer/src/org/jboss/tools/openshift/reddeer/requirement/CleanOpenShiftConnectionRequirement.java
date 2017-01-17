/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.requirement;

import static org.junit.Assert.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.reddeer.common.wait.TimePeriod;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.condition.core.ProjectExists;
import org.jboss.tools.openshift.reddeer.requirement.CleanOpenShiftConnectionRequirement.CleanConnection;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

public class CleanOpenShiftConnectionRequirement implements Requirement<CleanConnection> {

	private CleanConnection cleanConnection;
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface CleanConnection {
		/**
		 * Connection to clean. If nothing is provided
		 * {@link DatastoreOS3#SERVER} and {@link DatastoreOS33#USERNAME} are
		 * used.
		 */
		String connectionURL() default StringUtils.EMPTY;
	}

	@Override
	public boolean canFulfill() {
		return true;
	}

	@Override
	public void fulfill() {
		Connection connection = ConnectionUtils.getConnectionOrDefault(cleanConnection.connectionURL());
		assertNotNull("There is no connection with URL " + cleanConnection.connectionURL(), connection);
		List<IResource> projects = connection.getResources(ResourceKind.PROJECT);
		for (IResource project: projects) {
			String projectName = project.getName();
			connection.deleteResource(project);
			new WaitWhile(new ProjectExists(projectName, connection), TimePeriod.LONG);
		}
	}

	@Override
	public void setDeclaration(CleanConnection cleanConnection) {
		this.cleanConnection = cleanConnection;
		
	}

	@Override
	public void cleanUp() {
		// NOTHING TO DO
	}
	
}
