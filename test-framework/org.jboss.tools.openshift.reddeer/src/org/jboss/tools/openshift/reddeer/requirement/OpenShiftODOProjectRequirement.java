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
import org.eclipse.reddeer.junit.requirement.Requirement;
import org.eclipse.reddeer.swt.impl.button.OkButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftODOProjectRequirement.RequiredODOProject;
import org.jboss.tools.openshift.reddeer.utils.DatastoreOS3;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.utils.TestUtils;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOConnection;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;

/**
 * Create OpenShiftODOProject in OpenShift Application Explorer
 * 
 * @author jkopriva@redhat.com
 */
public class OpenShiftODOProjectRequirement implements Requirement<RequiredODOProject> {

	private RequiredODOProject projectSpec;

	private OpenShiftODOProject project;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface RequiredODOProject {
		/**
		 * the connection to use when connecting. If nothing is provided
		 * {@link DatastoreOS3#SERVER} and {@link DatastoreOS33#USERNAME} are used.
		 */
		String connectionURL() default StringUtils.EMPTY;

		/**
		 * the project name to use. If nothing is provided
		 * {@link OpenShiftResources#TEST_PROJECT} is used
		 */
		String name() default StringUtils.EMPTY;

		/**
		 * whether the project created by the requirement should be automatically
		 * deleted after test class, default false
		 */
		boolean cleanup() default false;
	}

	@Override
	public void fulfill() {
		String projectName = TestUtils.getValueOrDefault(projectSpec.name(), DatastoreOS3.TEST_PROJECT);
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
		connection.refresh();
		try {
			this.project = connection.createNewProject(projectName);
		} catch (WaitTimeoutExpiredException ex) {
			// error dialog is shown
			new DefaultShell(OpenShiftLabel.Shell.CREATE_PROJECT);
			new OkButton().click();
			// refresh connection and try it once again
			connection.refresh();
			this.project = connection.createNewProject(projectName);
		}
	}

	@Override
	public void setDeclaration(RequiredODOProject projectSpec) {
		this.projectSpec = projectSpec;
	}

	@Override
	public void cleanUp() {
		if (projectSpec.cleanup()) {
			OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
			explorer.open();
			OpenShiftODOConnection connection = explorer.getOpenShiftODOConnection();
			connection.refresh();
			OpenShiftODOProject odoProject = connection.getProject(this.project.getName());
			odoProject.delete();
		}
	}

	public OpenShiftODOProject getProject() {
		return project;
	}

	public String getProjectName() {
		if (project == null) {
			return null;
		}
		return project.getName();
	}

	@Override
	public RequiredODOProject getDeclaration() {
		return this.projectSpec;
	}
}
