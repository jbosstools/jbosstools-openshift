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
package org.jboss.tools.openshift.internal.ui.explorer;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;

import com.openshift3.client.model.IBuild;
import com.openshift3.client.model.IBuildConfig;
import com.openshift3.client.model.IDeploymentConfig;
import com.openshift3.client.model.IImageRepository;
import com.openshift3.client.model.IPod;
import com.openshift3.client.model.IProject;
import com.openshift3.client.model.IReplicationController;
import com.openshift3.client.model.IResource;
import com.openshift3.client.model.IService;

public class OpenShiftExplorerLabelProvider extends BaseExplorerLabelProvider { 

	@Override
	public Image getImage(Object element) {
		if(element instanceof ResourceGrouping){
			return OpenShiftCommonImages.FOLDER;
		}
		if(element instanceof IResource){
			IResource resource = (IResource) element;
			switch (resource.getKind()) {
			case BuildConfig:
				return OpenShiftImages.BUILDCONFIG_IMG;
			case ImageRepository:
				return OpenShiftImages.LAYER_IMG;
			case Pod:
				return OpenShiftImages.BLOCKS_IMG;
			case Project:
				return OpenShiftCommonImages.GLOBE_IMG;
			case Service:
				return OpenShiftImages.GEAR_IMG;
			default:
				 return OpenShiftCommonImages.FILE;
			}
		}
		return super.getImage(element);
	}

	@Override
	public StyledString getStyledText(Object element) {
		if(element instanceof IResource){
			IResource resource = (IResource)element;
			switch (resource.getKind()) {
			case Build:
				IBuild build = (IBuild)resource;
				return style(resource.getName(), build.getStatus());
			case BuildConfig:
				return style(resource.getName(), ((IBuildConfig) resource).getSourceURI());
			case DeploymentConfig:
				IDeploymentConfig config = (IDeploymentConfig) resource;
				return style(config.getName(), String.format("selector: %s", StringUtils.serialize(config.getReplicaSelector())));
			case ImageRepository:
				IImageRepository repo = (IImageRepository) resource;
				return style(repo.getName(), repo.getDockerImageRepository().toString());
			case Pod:
				IPod pod = (IPod) resource;
				String labels = StringUtils.serialize(pod.getLabels());
				if(StringUtils.isEmpty(labels)){
					return new StyledString(pod.getName());
				}
				String podQualifiedText = String.format("labels: %s", labels);
				return style(pod.getName(), podQualifiedText);
			case Project:
				IProject project = (IProject) resource;
				String namespace = project.getNamespace();
				String name = org.apache.commons.lang.StringUtils.defaultIfBlank(project.getDisplayName(), project.getName());
				if(org.apache.commons.lang.StringUtils.isEmpty(namespace)){
					return new StyledString(name);
				}
				return style(name, String.format("ns: %s", namespace));
			case ReplicationController:
				IReplicationController rc = (IReplicationController) resource;
				return (style(resource.getName(), String.format("selector: %s", StringUtils.serialize(rc.getReplicaSelector()))));
			case Service:
				IService service = (IService) resource;
				String serviceQualifiedText = String.format("selector: %s", StringUtils.serialize(service.getSelector()));
				return style(service.getName(), serviceQualifiedText);
			default:
				break;
			}
		}
		if(element instanceof ResourceGrouping){
			return new StyledString(StringUtils.humanize(((ResourceGrouping) element).getKind().pluralize()));
		}
		if(element instanceof Connection){
			return new StyledString(element.toString());
		}
		return super.getStyledText(element);
	}

	private StyledString style(String baseText, String qualifiedText) {
		return new StyledString(baseText)
			.append(" ")
			.append(qualifiedText, StyledString.QUALIFIER_STYLER);
	}

}
