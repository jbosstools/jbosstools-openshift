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

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IImageStream;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;
import com.openshift.restclient.model.route.IRoute;

/**
 * @author jeff.cantrill
 */
public class OpenShiftExplorerLabelProvider extends BaseExplorerLabelProvider { 

	@Override
	public Image getImage(Object element) {
		if (element instanceof ResourceGrouping) {
			return OpenShiftCommonImages.FOLDER;
		} else if (element instanceof IResource) {
			IResource resource = (IResource) element;
			switch (resource.getKind()) {
			case ResourceKind.BUILD_CONFIG:
				return OpenShiftImages.BUILDCONFIG_IMG;
			case ResourceKind.IMAGE_STREAM:
				return OpenShiftImages.LAYER_IMG;
			case ResourceKind.POD:
				return OpenShiftImages.BLOCKS_IMG;
			case ResourceKind.PROJECT:
				return OpenShiftCommonImages.GLOBE_IMG;
			case ResourceKind.SERVICE:
				return OpenShiftImages.GEAR_IMG;
			default:
				return OpenShiftCommonImages.FILE;
			}
		} else {
			return super.getImage(element);
		}
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof IResource) {
			IResource resource = (IResource)element;
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				IBuild build = (IBuild)resource;
				return style(resource.getName(), build.getStatus());
			case ResourceKind.BUILD_CONFIG:
				return style(resource.getName(), ((IBuildConfig) resource).getSourceURI());
			case ResourceKind.DEPLOYMENT_CONFIG:
				IDeploymentConfig config = (IDeploymentConfig) resource;
				return style(config.getName(), String.format("selector: %s", StringUtils.serialize(config.getReplicaSelector())));
			case ResourceKind.IMAGE_STREAM:
				IImageStream repo = (IImageStream) resource;
				return style(repo.getName(), repo.getDockerImageRepository().toString());
			case ResourceKind.POD:
				IPod pod = (IPod) resource;
				String labels = StringUtils.serialize(pod.getLabels());
				if(StringUtils.isEmpty(labels)){
					return new StyledString(pod.getName());
				}
				String podQualifiedText = String.format("labels: %s", labels);
				return style(pod.getName(), podQualifiedText);
			case ResourceKind.PROJECT:
				IProject project = (IProject) resource;
				String name = org.apache.commons.lang.StringUtils.defaultIfBlank(project.getDisplayName(), project.getName());
				return style(name, "");
			case ResourceKind.ROUTE:
				IRoute route = (IRoute) resource;
				return style(route.getName(), String.format("%s%s", route.getHost(),route.getPath()));
			case ResourceKind.REPLICATION_CONTROLLER:
				IReplicationController rc = (IReplicationController) resource;
				return (style(resource.getName(), String.format("selector: %s", StringUtils.serialize(rc.getReplicaSelector()))));
			case ResourceKind.SERVICE:
				IService service = (IService) resource;
				String serviceQualifiedText = String.format("selector: %s", StringUtils.serialize(service.getSelector()));
				return style(service.getName(), serviceQualifiedText);
			default:
				break;
			}
		}
		if (element instanceof ResourceGrouping) {
			return new StyledString(StringUtils.humanize(((ResourceGrouping) element).getKind() + "s"));
		} else if (element instanceof Connection) {
			Connection conn = (Connection) element;
			String prefix = org.apache.commons.lang.StringUtils.defaultIfBlank(conn.getUsername(), "<unknown user>");
			if(prefix == null) {
				prefix = "<unknown user>";
			}
			return style(prefix, conn.toString());
		}
		return super.getStyledText(element);
	}

	private StyledString style(String baseText, String qualifiedText) {
		StyledString value = new StyledString(baseText);
		if(org.apache.commons.lang.StringUtils.isNotBlank(qualifiedText)) {
			value.append(" ")
				.append(qualifiedText, StyledString.QUALIFIER_STYLER);
			
		}
		return value;
	}

}
