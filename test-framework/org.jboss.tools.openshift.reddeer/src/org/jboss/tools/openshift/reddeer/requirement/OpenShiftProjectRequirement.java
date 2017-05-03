/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.osgi.util.NLS;
import org.jboss.reddeer.common.wait.WaitWhile;
import org.jboss.reddeer.junit.requirement.Requirement;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.reddeer.condition.OpenShiftProjectExists;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftProjectRequirement.RequiredProject;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.utils.v3.OpenShift3NativeProjectUtils;

import com.openshift.restclient.model.IProject;

/**
 * A requirement that makes sure a given project exists. If it doesnt it will
 * get created
 * 
 * @author adietish@redhat.com
 */
public class OpenShiftProjectRequirement implements Requirement<RequiredProject> {

	private RequiredProject projectSpec;

	private IProject project;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RequiredProject {
		/**
		 * the connection to use when connecting. If nothing is provided
		 * {@link DatastoreOS3#SERVER} and {@link DatastoreOS33#USERNAME} are
		 * used.
		 */
		String connectionURL() default StringUtils.EMPTY;

		/**
		 * the project name to use. If nothing is provided
		 * {@link OpenShiftResources#TEST_PROJECT} is used
		 */
		String name() default StringUtils.EMPTY;

		/**
		 * the project display name to use. If nothing is provided
		 * {@link #name()} is used
		 */
		String displayName() default StringUtils.EMPTY;

		/**
		 * the project description to use. If nothing is provided
		 * {@link #name()} is used
		 */
		String description() default StringUtils.EMPTY;
		
		/**
		 * whether the project created by the requirement should be automatically deleted after test class, default false
		 */
		boolean cleanup() default false;
	}

	@Override
	public boolean canFulfill() {
		return true;
	}

	@Override
	public void fulfill() {
		String projectName = TestUtils.getValueOrDefault(projectSpec.name(), DatastoreOS3.TEST_PROJECT);
		Connection connection = ConnectionUtils.getConnectionOrDefault(projectSpec.connectionURL());
		assertNotNull(NLS.bind("No connection {0} exists", projectSpec.connectionURL()), connection);

		this.project = OpenShift3NativeProjectUtils.getOrCreateProject(projectName, projectSpec.displayName(),
				projectSpec.description(), connection);
	}

	@Override
	public void setDeclaration(RequiredProject projectSpec) {
		this.projectSpec = projectSpec;
	}

	@Override
	public void cleanUp() {
		if (projectSpec.cleanup()) {
			Connection connection = ConnectionUtils.getConnectionOrDefault(projectSpec.connectionURL());
			connection.deleteResource(project);
			new WaitWhile(new OpenShiftProjectExists(project.getName()));
		}
	}

	public IProject getProject() {
		return project;
	}

	public String getProjectName() {
		if (project == null) {
			return null;
		}
		return project.getName();
	}
}
