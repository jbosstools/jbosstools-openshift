/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployment;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;

import com.openshift.kube.Client;
import com.openshift.kube.Project;
import com.openshift.kube.images.DockerImageDescriptor;

public class DeploymentWizardContext {

	private DockerImageDescriptor image;
	private String projectName = "";
	private boolean createServerAdapter;
	private Project openshiftProject;
	private String userName = "jcantrill";
	private Client client;
	private String repositoryUri;
	private boolean includeBuildConfig = true;
	private String serviceDependencies;

	public DeploymentWizardContext(Client client, Project p, String repositoryUri) {
		this.client = client;
		this.openshiftProject = p;
		this.repositoryUri = repositoryUri;
	}

	public void setImage(DockerImageDescriptor image) {
		this.image = image;
	}

	public DockerImageDescriptor getImage(){
		return this.image;
	}

	public IProject getProject() {
		String projectName = getProjectName();
		if (StringUtils.isEmpty(projectName)) {
			return null;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectName() {
		return this.projectName;
	}

	public boolean createServerAdapter() {
		return this.createServerAdapter;
	}

	public void createServerAdapter(Boolean createServerAdapter) {
		this.createServerAdapter = createServerAdapter;
	}

	public String getNamespace() {
		return this.openshiftProject.getNamespace();
	}
	
	public String getUserName(){
		return this.userName;
	}

	public void setOpenShiftProject(Project project) {
		this.openshiftProject = project;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Client getClient() {
		return this.client;
	}

	public String getRepositoryUri() {
		return this.repositoryUri ;
	}

	public void setIncludeBuildConfig(boolean include) {
		this.includeBuildConfig = include;
	}
	
	public boolean includeBuildConfig(){
		return this.includeBuildConfig;
	}

	public String getServiceDependencies() {
		return this.serviceDependencies;
	}

	public void setServiceDependencies(String services) {
		this.serviceDependencies = services;
	}
	
}
