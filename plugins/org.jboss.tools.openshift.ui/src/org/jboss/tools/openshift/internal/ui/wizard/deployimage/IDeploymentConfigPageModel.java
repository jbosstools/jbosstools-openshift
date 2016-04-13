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

import java.util.List;

import org.jboss.tools.openshift.internal.ui.wizard.common.IEnvironmentVariablesPageModel;

import com.openshift.restclient.model.IPort;

/**
 * Page model for the deployment config page
 * @author jeff.cantrill
 *
 */
public interface IDeploymentConfigPageModel extends IEnvironmentVariablesPageModel {

	static final String PROPERTY_VOLUMES = "volumes";
	static final String PROPERTY_SELECTED_VOLUME = "selectedVolume";
	static final String PROPERTY_PORT_SPECS = "portSpecs";
	static final String PROPERTY_REPLICAS = "replicas";
	
	String getResourceName();


	void setVolumes(List<String> volumes);
	List<String> getVolumes();
	void setSelectedVolume(String volume);
	String getSelectedVolume();
	void updateVolume(String volume, String value);
	
	List<IPort> getPortSpecs();
	
	/**
	 * The number of replicas to define in the deployment config.
	 * This is scalability factor;
	 * @return
	 */
	int getReplicas();
	
	/**
	 * The number of replicas to define in the deployment config.
	 * This is scalability factor;

	 * @param replicas  a number of 1 or more replicas
	 */
	void setReplicas(int replicas);




}
