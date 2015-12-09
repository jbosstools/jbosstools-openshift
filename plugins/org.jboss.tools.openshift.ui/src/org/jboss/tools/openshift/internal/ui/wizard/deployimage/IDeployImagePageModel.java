/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployimage;

import java.util.Collection;
import java.util.List;

import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

import com.openshift.restclient.model.IProject;

/**
 * Page model for the deploy image page
 * @author jeff.cantrill
 *
 */
public interface IDeployImagePageModel extends IConnectionAware<Connection>{

	static final String PROPERTY_CONNECTIONS = "connections";
	static final String PROPERTY_DOCKER_CONNECTIONS = "dockerConnections";
	static final String PROPERTY_DOCKER_CONNECTION = "dockerConnection";
	static final String PROPERTY_PROJECTS = "projects";
	static final String PROPERTY_PROJECT = "project";
	static final String PROPERTY_NAME = "name";
	static final String PROPERTY_IMAGE = "image";
	
	/**
	 * 
	 */
	boolean originatedFromDockerExplorer();
	
	/**
	 * The set of known OpenShift connections
	 * @return
	 */
	Collection<Connection> getConnections();

	List<IDockerConnection> getDockerConnections();
	
	IDockerConnection getDockerConnection();
	
	void setDockerConnection(IDockerConnection connection);
	
	/**
	 * The list of OpenShift projects associated with a the selected
	 * OpenShift connection
	 * @return
	 */
	Collection<IProject> getProjects();
	void setProjects(Collection<IProject> projects);
	
	/**
	 * The project to associate with this deployment
	 * @return
	 */
	IProject getProject();
	void setProject(IProject project);
	
	/**
	 * The name to be used for the deployed resources
	 * @return
	 */
	String getName();
	void setName(String name);
	
	/**
	 * The docker image to use
	 * @return
	 */
	String getImage();
	void setImage(String image);
	
	/**
	 * 
	 * @param image
	 * @return true if the image exists in the docker registry
	 */
	boolean imageExists(String image);
}
