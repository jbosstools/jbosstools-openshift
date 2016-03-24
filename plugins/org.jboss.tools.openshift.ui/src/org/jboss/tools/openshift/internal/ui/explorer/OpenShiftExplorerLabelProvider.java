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

import java.util.Collection;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerLabelProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.ITags;
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
import com.openshift.restclient.model.template.ITemplate;

/**
 * @author jeff.cantrill
 * @author Andre Dietisheim
 */
public class OpenShiftExplorerLabelProvider extends BaseExplorerLabelProvider { 
	//Limit for label length = baseText.length + qualifiedText.length
	private static final int DEFAULT_LABEL_LIMIT = 60;

	private int labelLimit;

	public OpenShiftExplorerLabelProvider() {
		setLabelLimit(DEFAULT_LABEL_LIMIT);
	}

	/**
	 * Set limit of label length in characters. 
	 * @param limit
	 */
	public void setLabelLimit(int limit) {
		if(limit <= 0) {
			throw new IllegalArgumentException("Label limit cannot be less than 1: " + limit);
		}
		labelLimit = limit;
	}
	
	@Override
	public String getText(Object element) {
		if(element instanceof IProject) {
			IProject project = (IProject) element;
			String name = project.getName();
			if(org.apache.commons.lang.StringUtils.isNotEmpty(project.getDisplayName())) {
				String[] parts = new String[]{project.getDisplayName(), name};
				applyEllipses(parts);
				name = parts[0] + " (" + parts[1] + ")";
			}
			return name;
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Deployment) {
			return OpenShiftImages.SERVICE_IMG;
		} else if(element instanceof NewProjectLinkNode) {
			return OpenShiftImages.PROJECT_NEW_IMG;
		} else if (element instanceof IResource || element instanceof IResourceUIModel) {
			IResource resource = element instanceof IResourceUIModel ? ((IResourceUIModel)element).getResource() : (IResource) element;
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				return OpenShiftImages.BUILD_IMG;
			case ResourceKind.BUILD_CONFIG:
				return OpenShiftImages.BUILDCONFIG_IMG;
			case ResourceKind.IMAGE_STREAM:
				return OpenShiftImages.IMAGE_IMG;
			case ResourceKind.POD:
				return OpenShiftImages.BLOCKS_IMG;
			case ResourceKind.PROJECT:
				return OpenShiftImages.PROJECT_IMG;
			case ResourceKind.ROUTE:
				return OpenShiftImages.ROUTE_IMG;
			case ResourceKind.SERVICE:
				return OpenShiftImages.SERVICE_IMG;
			default:
				return OpenShiftCommonImages.FILE;
			}
		} else {
			return super.getImage(element);
		}
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof Deployment) {
			Deployment d = (Deployment) element;
			return style(d.getService().getName(), formatRoute(d.getRoutes()));
		} else if(element instanceof NewProjectLinkNode) {
			return getStyledText((NewProjectLinkNode)element);
		}
		if (element instanceof IResource || element instanceof IResourceUIModel) {
			IResource resource = element instanceof IResourceUIModel ? ((IResourceUIModel)element).getResource() : (IResource) element;
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				return getStyledText((IBuild) resource);
			case ResourceKind.BUILD_CONFIG:
				return getStyledText((IBuildConfig) resource);
			case ResourceKind.DEPLOYMENT_CONFIG:
				return getStyledText((IDeploymentConfig) resource);
			case ResourceKind.IMAGE_STREAM:
				return getStyledText((IImageStream) resource);
			case ResourceKind.POD:
				return getStyledText((IPod) resource);
			case ResourceKind.PROJECT:
				return getStyledText((IProject) resource);
			case ResourceKind.ROUTE:
				return getStyledText((IRoute) resource);
			case ResourceKind.REPLICATION_CONTROLLER:
				return getStyledText((IReplicationController) resource);
			case ResourceKind.SERVICE:
				IService service = (IService) resource;
				return getStyledText(service);
			case ResourceKind.TEMPLATE:
				return getStyledText((ITemplate) resource);
			default:
				break;
			}
		}
		 if (element instanceof Connection) {
			return getStyledText((Connection) element);
		}
		return super.getStyledText(element);
	}

	private String formatRoute(Collection<IResourceUIModel> routes) {
		if(routes.size() > 0) {
			IRoute route = (IRoute)routes.iterator().next().getResource();
			return route.getURL();
			
		}
		return "";
	}

	private StyledString getStyledText(IService service) {
		String serviceQualifiedText = String.format("selector: %s", StringUtils.serialize(service.getSelector()));
		return style(service.getName(), serviceQualifiedText);
	}

	private StyledString getStyledText(IReplicationController replicationController) {
		return style(replicationController.getName(), 
				String.format("selector: %s", StringUtils.serialize(replicationController.getReplicaSelector())));
	}

	private StyledString getStyledText(IRoute route) {
		return style(route.getName(), String.format("%s%s", route.getHost(), route.getPath()));
	}

	private StyledString getStyledText(IBuild build) {
		return style(build.getName(), String.format("%s %s","Build",build.getStatus()));
	}

	private StyledString getStyledText(IBuildConfig config) {
		return style(config.getName(), config.getSourceURI());
	}
	
	
	private StyledString getStyledText(IDeploymentConfig config) {
		return style(config.getName(), String.format("selector: %s", StringUtils.serialize(config.getReplicaSelector())));
	}

	private StyledString getStyledText(IPod pod) {
		return style(pod.getName(), String.format("%s %s","Pod",pod.getStatus()));
	}

	private StyledString getStyledText(IImageStream repo) {
		return style(repo.getName(), repo.getDockerImageRepository().toString());
	}

	private StyledString getStyledText(Connection conn) {
		String prefix = org.apache.commons.lang.StringUtils.defaultIfBlank(conn.getUsername(), "<unknown user>");
		if(prefix == null) {
			prefix = "<unknown user>";
		}
		return style(prefix, conn.toString());
	}

	private StyledString getStyledText(ITemplate template) {
		String tags = (String) template.accept(new CapabilityVisitor<ITags, Object>() {
			@Override
			public Object visit(ITags capability) {
				return NLS.bind("({0})", org.apache.commons.lang.StringUtils.join(capability.getTags(), ", "));
			}
		}, null);
		StringBuilder qualifier = new StringBuilder();
		if(!StringUtils.isEmpty(tags)) {
			qualifier.append(tags);
		}
		if(!StringUtils.isEmpty(template.getNamespace())) {
			qualifier.append(" - ").append(template.getNamespace());
		}
		return style(template.getName(), qualifier.toString());
	}

	private StyledString getStyledText(IProject project) {
		if(org.apache.commons.lang.StringUtils.isNotBlank(project.getDisplayName())) {
			return style(project.getDisplayName(), project.getName());
		}
		return style(project.getName(), "");
	}

	private StyledString style(String baseText, String qualifiedText) {
		String[] parts = new String[]{baseText, qualifiedText};
		applyEllipses(parts);
		baseText = parts[0];
		qualifiedText = parts[1];
		StyledString value = new StyledString(baseText);
		if(org.apache.commons.lang.StringUtils.isNotBlank(qualifiedText)) {
			value.append(" ")
				.append(qualifiedText, StyledString.QUALIFIER_STYLER);
			
		}
		return value;
	}

	private void applyEllipses(String[] parts) {
		StringUtils.shorten(parts, labelLimit);
	}

	private StyledString getStyledText(NewProjectLinkNode node) {
		StyledString value = new StyledString();
		value.append(node.toString(), new StyledString.Styler() {

			@Override
			public void applyStyles(TextStyle textStyle) {
				textStyle.underline = true;
				textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
			}

		});
		return value;
	}
}
