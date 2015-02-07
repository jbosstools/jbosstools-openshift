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

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
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

public class OpenShiftExplorerLabelProvider implements ILabelProvider,  IStyledLabelProvider { 


	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {
		if(element instanceof ResourceGrouping){
			return OpenShiftCommonImages.FOLDER;
		}
		if (element instanceof IConnection) {
			return OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_ICON_IMG;
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
		return null;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).toString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		if(element instanceof IResource){
			IResource resource = (IResource)element;
			switch (resource.getKind()) {
			case Build:
				IBuild build = (IBuild)resource;
				return style(resource.getName(), String.format("(%s)", build.getStatus()));
			case BuildConfig:
				return style(resource.getName(), ((IBuildConfig) resource).getSourceURI());
			case DeploymentConfig:
				IDeploymentConfig config = (IDeploymentConfig) resource;
				return style(config.getName(), "(selector: " + StringUtils.serialize(config.getReplicaSelector()) + ")");
			case ImageRepository:
				IImageRepository repo = (IImageRepository) resource;
				return style(repo.getName(), repo.getDockerImageRepository().toString());
			case Pod:
				IPod pod = (IPod) resource;
				String podQualifiedText = String.format("(ip: %s, labels: %s)", pod.getIP(), StringUtils.serialize(pod.getLabels()));
				return style(pod.getName(), podQualifiedText);
			case Project:
				IProject project = (IProject) resource;
				String namespace = project.getNamespace();
				if(org.apache.commons.lang.StringUtils.isEmpty(namespace)){
					return new StyledString(project.getDisplayName());
				}
				return style(project.getDisplayName(), String.format("(ns: %s)", namespace));
			case ReplicationController:
				IReplicationController rc = (IReplicationController) resource;
				return (style(resource.getName(), String.format("(selector: %s)", StringUtils.serialize(rc.getReplicaSelector()))));
			case Service:
				IService service = (IService) resource;
				String serviceQualifiedText = String.format("routing TCP traffic on %s:%s to %s", service.getPortalIP(), service.getPort(), service.getContainerPort());
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
		return null;
	}

	private StyledString style(String baseText, String qualifiedText) {
		return new StyledString(baseText)
			.append(" ")
			.append(qualifiedText, StyledString.QUALIFIER_STYLER);
	}

}
