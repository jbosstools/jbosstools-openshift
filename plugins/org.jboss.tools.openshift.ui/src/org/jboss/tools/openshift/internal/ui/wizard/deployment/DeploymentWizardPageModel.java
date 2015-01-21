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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.common.databinding.ObservablePojo;

import com.openshift.kube.images.DockerImageDescriptor;
import com.openshift.kube.images.ImageUri;

/**
 * @author Jeff Cantrill
 */
public class DeploymentWizardPageModel extends ObservablePojo {
	
	public static final String PROPERTY_SELECTED_IMAGE = "selectedImage";
	
	private DeploymentWizardContext context;
	
	public DeploymentWizardPageModel(DeploymentWizardContext context){
		this.context = context;
	}
	
	public List<DockerImageDescriptor> getBaseImages(){
		List<DockerImageDescriptor> images = new ArrayList<DockerImageDescriptor>();
		images.add(new DockerImageDescriptor(new ImageUri("openshift/wildfly-8-centos"), "WildFly Application Server 8.1.0.Final"));
		
		Map<String, String> mongoEnv = new HashMap<String, String>();
		mongoEnv.put("OPENSHIFT_MONGODB_DB_USERNAME", "mongo");
		mongoEnv.put("OPENSHIFT_MONGODB_DB_PASSWORD", "mongo");
		images.add(new DockerImageDescriptor(new ImageUri("library/mongo"), "MongoDB Document Database"));
		return images;
	}
	
	public DockerImageDescriptor getSelectedImage() {
		return context.getImage();
	}

	public void setSelectedImage(DockerImageDescriptor selectedImage) {
		context.setImage(selectedImage);
	}
}
