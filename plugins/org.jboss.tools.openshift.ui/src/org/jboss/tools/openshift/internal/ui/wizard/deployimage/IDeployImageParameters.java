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

import java.util.Map;

import org.jboss.tools.openshift.internal.ui.wizard.common.IResourceLabelsPageModel;

/**
 * The parameters need to generate the OpenShift resources
 * to support a Docker image
 * 
 * @author jeff.cantrill
 *
 */
public interface IDeployImageParameters 
	extends IDeployImagePageModel, 
			IDeploymentConfigPageModel, 
			IResourceLabelsPageModel,
			IServiceAndRoutingPageModel{

	void setOriginatedFromDockerExplorer(boolean b);
	
	/**
	 * Retrieve the env vars declared by the image
	 * @return
	 */
	Map<String, String> getImageEnvVars();

}
