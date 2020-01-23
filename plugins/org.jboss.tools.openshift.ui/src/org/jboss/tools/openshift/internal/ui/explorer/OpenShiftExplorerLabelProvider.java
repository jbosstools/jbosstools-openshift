/*******************************************************************************
 * Copyright (c) 2015-2017 Red Hat, Inc.
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

import org.eclipse.core.runtime.IAdaptable;
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
import org.jboss.tools.openshift.internal.ui.models.IResourceWrapper;
import org.jboss.tools.openshift.internal.ui.models.IServiceWrapper;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.IApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromimage.ImageStreamApplicationSource;
import org.jboss.tools.openshift.internal.ui.wizard.newapp.fromtemplate.TemplateApplicationSource;

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
 * @author Andre Dietisheim
 * @author Jeff Maury
 */
public class OpenShiftExplorerLabelProvider extends BaseExplorerLabelProvider {

	@Override
	public String getText(Object element) {
		if (element instanceof IProject) {
			IProject project = (IProject) element;
			String name = project.getName();
			if (org.apache.commons.lang.StringUtils.isNotEmpty(project.getDisplayName())) {
				String[] parts = new String[] { project.getDisplayName(), name };
				applyEllipses(parts, labelLimit);
				name = parts[0] + " (" + parts[1] + ")";
			}
			return name;
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		element = getAdaptedElement(element);
		if (element instanceof NewProjectLinkNode) {
			return OpenShiftImages.PROJECT_NEW_IMG;
		} else if (element instanceof IResource) {
			IResource resource = (IResource) element;
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
			case ResourceKind.REPLICATION_CONTROLLER:
			case ResourceKind.DEPLOYMENT_CONFIG:
				return OpenShiftImages.REPLICATION_CONTROLLER_IMG;
			default:
				return OpenShiftCommonImages.FILE;
			}
		} else if (element instanceof ImageStreamApplicationSource) {
			return OpenShiftImages.IMAGE_IMG;
		} else if (element instanceof TemplateApplicationSource) {
			return OpenShiftImages.TEMPLATE_IMG;
		} else {
			return super.getImage(element);
		}
	}

	private Object getAdaptedElement(Object element) {
		if (element instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) element;
			IResource resource = adaptable.getAdapter(IResource.class);
			if (resource != null) {
				element = resource;
			}
			Connection connection = adaptable.getAdapter(Connection.class);
			if (connection != null) {
				element = connection;
			}
		}
		return element;
	}

	@Override
	public StyledString getStyledText(Object element, int limit) {
		if (element instanceof IServiceWrapper) {
			IServiceWrapper d = (IServiceWrapper) element;
			return style(d.getWrapped().getName(), formatRoute(d.getResourcesOfKind(ResourceKind.ROUTE)), limit);
		}
		element = getAdaptedElement(element);
		if (element instanceof NewProjectLinkNode) {
			return getStyledText((NewProjectLinkNode) element, limit);
		} else if (element instanceof IApplicationSource) {
			return getStyledText((IApplicationSource) element, limit);
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			switch (resource.getKind()) {
			case ResourceKind.BUILD:
				return getStyledText((IBuild) resource, limit);
			case ResourceKind.BUILD_CONFIG:
				return getStyledText((IBuildConfig) resource, limit);
			case ResourceKind.DEPLOYMENT_CONFIG:
				return getStyledText((IDeploymentConfig) resource, limit);
			case ResourceKind.IMAGE_STREAM:
				return getStyledText((IImageStream) resource, limit);
			case ResourceKind.POD:
				return getStyledText((IPod) resource, limit);
			case ResourceKind.PROJECT:
				return getStyledText((IProject) resource, limit);
			case ResourceKind.ROUTE:
				return getStyledText((IRoute) resource, limit);
			case ResourceKind.REPLICATION_CONTROLLER:
				return getStyledText((IReplicationController) resource, limit);
			case ResourceKind.SERVICE:
				IService service = (IService) resource;
				return getStyledText(service, limit);
			default:
				break;
			}
		}
		if (element instanceof Connection) {
			return getStyledText((Connection) element, limit);
		}
		return super.getStyledText(element, limit);
	}

	private String formatRoute(Collection<IResourceWrapper<?, ?>> routes) {
		if (routes.size() > 0) {
			IRoute route = (IRoute) routes.iterator().next().getWrapped();
			return route.getURL();

		}
		return "";
	}

	private StyledString getStyledText(IService service, int limit) {
		String serviceQualifiedText = String.format("selector: %s", StringUtils.serialize(service.getSelector()));
		return style(service.getName(), serviceQualifiedText, limit);
	}

	private StyledString getStyledText(IReplicationController replicationController, int limit) {
		return style(replicationController.getName(),
				String.format("selector: %s", StringUtils.serialize(replicationController.getReplicaSelector())), limit);
	}

	private StyledString getStyledText(IRoute route, int limit) {
		return style(route.getName(), String.format("%s%s", route.getHost(), route.getPath()), limit);
	}

	private StyledString getStyledText(IBuild build, int limit) {
		return style(build.getName(),
				build.getStatus() == null ? "Build" : String.format("%s %s", "Build", build.getStatus()), limit);
	}

	private StyledString getStyledText(IBuildConfig config, int limit) {
		return style(config.getName(), config.getSourceURI(), limit);
	}

	private StyledString getStyledText(IDeploymentConfig config, int limit) {
		return style(config.getName(),
				String.format("selector: %s", StringUtils.serialize(config.getReplicaSelector())), limit);
	}

	private StyledString getStyledText(IPod pod, int limit) {
		return style(pod.getName(), pod.getStatus() == null ? "Pod" : String.format("%s %s", "Pod", pod.getStatus()), limit);
	}

	private StyledString getStyledText(IImageStream repo, int limit) {
		return style(repo.getName(), repo.getDockerImageRepository().toString(), limit);
	}

	private StyledString getStyledText(Connection conn, int limit) {
		String prefix = org.apache.commons.lang.StringUtils.defaultIfBlank(conn.getUsername(), "<unknown user>");
		if (prefix == null) {
			prefix = "<unknown user>";
		}
		return style(prefix, conn.toString(), limit);
	}

	private StyledString getStyledText(IApplicationSource source, int limit) {
		String tags = NLS.bind("({0})", org.apache.commons.lang.StringUtils.join(source.getTags(), ", "));
		StringBuilder qualifier = new StringBuilder();
		if (!StringUtils.isEmpty(tags)) {
			qualifier.append(tags);
		}
		if (!StringUtils.isEmpty(source.getNamespace())) {
			qualifier.append(" - ").append(source.getNamespace());
		}
		return style(source.getName(), qualifier.toString(), limit);
	}

	private StyledString getStyledText(IProject project, int limit) {
		if (org.apache.commons.lang.StringUtils.isNotBlank(project.getDisplayName())) {
			return style(project.getDisplayName(), project.getName(), limit);
		}
		return style(project.getName(), "", limit);
	}

	private StyledString getStyledText(NewProjectLinkNode node, int limit) {
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
