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
	static final String PROPERTY_RESOURCE_NAME = "resourceName";
	static final String PROPERTY_IMAGE_NAME = "imageName";
	static final String PROPERTY_PUSH_IMAGE_TO_REGISTRY = "pushImageToRegistry";
	static final String PROPERTY_TARGET_REGISTRY_LOCATION = "targetRegistryLocation";
	static final String PROPERTY_TARGET_REGISTRY_USERNAME = "targetRegistryUsername";
	static final String PROPERTY_TARGET_REGISTRY_PASSWORD = "targetRegistryPassword";
	
	
	/**
	 * 
	 */
	boolean originatedFromDockerExplorer();

	/**
	 * Returns true if model is initialized with active connection selected project and
	 * when wizard is originated from docker explorer connection page may be skipped.
	 * @return
	 */
	boolean isStartedWithActiveConnection();
	
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
	List<IProject> getProjects();

	void setProjects(List<IProject> projects);
	
	/**
	 * The project to associate with this deployment
	 * @return
	 */
	IProject getProject();
	void setProject(IProject project);
	
	/**
	 * @return the name to be used for the deployed resources
	 */
	String getResourceName();
	
	/**
	 * Sets the name to be used for the deployed resources
	 * @param resourceName the name to be used for the deployed resources
	 */
	void setResourceName(String resourceName);
	
	/**
	 * @return the name of the Docker Image to use
	 */
	String getImageName();
	
	/**
	 * Sets the name of the Docker Image to use
	 * @param imageName the name of the Docker Image to use
	 */
	void setImageName(String imageName);

	/**
	 * Since method setImageName(String) ignores attempts to set an empty value, 
	 * ui gets not synchronized with model as soon as image name is cleared.
	 * Then, if the previous value is selected again, model will not be changed
	 * if that method is called, and will not fire change event. This method forces it.
	 * @param imageName
	 * @param forceUpdate
	 */
	void setImageName(String imageName, boolean forceUpdate);

	/**
	 * @return the list of names of all images for the current Docker connection.
	 */
	List<String> getImageNames();
	

	/**
	 * @return flag to indicate if the image should be pushed to the Docker registry on OpenShift
	 */
	boolean isPushImageToRegistry();

	/**
	 * @param pushImageToRegistry flag to indicate if the image should be pushed to the Docker registry on OpenShift
	 */
	void setPushImageToRegistry(boolean pushImageToRegistry);
	
	/**
	 * @return the URL to the target registry where the image will be pushed
	 */
	String getTargetRegistryLocation();
	
	/**
	 * @param targetRegistryLocation the URL to the target registry where the image will be pushed
	 */
	void setTargetRegistryLocation(String targetRegistryLocation);
	
	/**
	 * @return the username to connect to the target registry where the image will be pushed
	 */
	String getTargetRegistryUsername();
	
	/**
	 * @param targetRegistryUsername the username to connect to the target registry where the image will be pushed
	 */
	void setTargetRegistryUsername(String targetRegistryUsername);
	
	/**
	 * @return the password to connect to the target registry where the image will be pushed
	 */
	String getTargetRegistryPassword();
	
	/**
	 * @param targetRegistryPassword the password to connect to the target registry where the image will be pushed
	 */
	void setTargetRegistryPassword(String targetRegistryPassword);
	
	/**
	 * Initializes the container info from the selected Docker Image.
	 * 
	 * <p>
	 * <strong>Note:</strong> This operation can be consuming since it may
	 * involve remote calls to retrive the Docker Image metadata.
	 * 
	 * @return <code>true</code> if the initialization succeeded, <code>false</code> otherwise.
	 */
	boolean initializeContainerInfo();
	
	/**
	 * Free any resource
	 */
	void dispose();
}
