/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.ExpressResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressImages;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerContentProvider.LoadingStub;
import org.jboss.tools.openshift.express.internal.ui.explorer.OpenShiftExplorerContentProvider.NotConnectedUserStub;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class OpenShiftExplorerLabelProvider implements IStyledLabelProvider, ILabelProvider {

	private static final String DEFAULT_MARKER = "(default)";

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
		Image image = null;
		if (element instanceof IConnection) {
			image = ExpressImages.OPENSHIFT_LOGO_WHITE_ICON_IMG;
		} else if (element instanceof IDomain || element instanceof Project) {
			image = ExpressImages.GLOBE_IMG;
		} else if (element instanceof IApplication || element instanceof DeploymentConfig) {
			image = ExpressImages.QUERY_IMG;
		} else if (element instanceof BuildConfig){
			image = ExpressImages.BUILDCONFIG_IMG;
		} else if (element instanceof IEmbeddedCartridge) {
			image = ExpressImages.TASK_REPO_IMG;
		} else if (element instanceof LoadingStub) {
			image = ExpressImages.SYSTEM_PROCESS_IMG;
		} else if (element instanceof OpenShiftException) {
			image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
		return image;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString styledString = null;
		if (element instanceof IConnection) {
			styledString = createStyledString((IConnection) element);
		} else if (element instanceof Project){
			Project p = (Project) element;
			String label = 
					new StringBuilder(p.getDisplayName()).append(" (ns:").append(p.getNamespace()).append(')').toString();

			styledString = new StyledString(label);
			styledString.setStyle(p.getDisplayName().length() +1, label.length() - p.getDisplayName().length() - 1, StyledString.QUALIFIER_STYLER);
		} else if (element instanceof Service){
			Service s = (Service) element;
			StringBuilder b = new StringBuilder(s.getName());
			b.append(" (selector: ").append(s.getSelector()).append(")");
			styledString = new StyledString(b.toString());
			styledString.setStyle(s.getName().length() + 1,b.length() - s.getName().length() -1 , StyledString.QUALIFIER_STYLER);
		} else if (element instanceof DeploymentConfig){
			DeploymentConfig config = (DeploymentConfig) element;
			StringBuilder b = new StringBuilder(config.getName());
			styledString = new StyledString(b.toString());
		} else if (element instanceof BuildConfig){
			BuildConfig config = (BuildConfig) element;
			StringBuilder b = new StringBuilder(config.getName());
			b.append(" ").append(config.getSourceUri());
			styledString = new StyledString(b.toString());
			styledString.setStyle(config.getName().length() + 1,b.length() - config.getName().length() -1 , StyledString.QUALIFIER_STYLER);
		}else if (element instanceof IDomain) {
			styledString = createStyledString((IDomain) element);
		} else if (element instanceof IApplication) {
			styledString = createStyledString((IApplication) element);
		} else if (element instanceof IEmbeddedCartridge) {
			styledString = createStyledString((IEmbeddedCartridge) element);
		} else if (element instanceof LoadingStub) {
			styledString = new StyledString(OpenShiftExpressUIMessages.LOADING_USER_APPLICATIONS_LABEL);
		} else if (element instanceof NotConnectedUserStub) {
			styledString = new StyledString(OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL);
		} else if (element instanceof OpenShiftException) {
			styledString = new StyledString(((OpenShiftException) element).getMessage());
		} else {
			styledString = new StyledString(element.toString());
		}
		return styledString;
	}

	private StyledString createStyledString(IConnection connection) {
		String name = connection.getUsername();
		String host = connection.getHost();
		StringBuilder builder = new StringBuilder(name).append(' ').append(host);
		if (connection instanceof org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection 
				&& ((org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection)connection).isDefaultHost()) {
			builder.append(' ').append(DEFAULT_MARKER);
		}
		String label = builder.toString();
		StyledString styledString = new StyledString(label);
		styledString.setStyle(name.length() + 1, builder.length() - name.length() - 1, StyledString.QUALIFIER_STYLER);
		return styledString;
	}

	private StyledString createStyledString(IDomain domain) {
		String id = domain.getId();
		String fullName = ExpressResourceLabelUtils.toString(domain); 
		String label = 
				new StringBuilder(id).append(' ').append(fullName).toString();

		StyledString styledString = new StyledString(label);
		styledString.setStyle(id.length() + 1, fullName.length(), StyledString.QUALIFIER_STYLER);
		return styledString;
	}

	private StyledString createStyledString(IApplication application) {
		String appName = application.getName();
		String appType = StringUtils.null2emptyString(ExpressResourceLabelUtils.toString(application.getCartridge()));
		StringBuilder sb = new StringBuilder(appName).append(' ').append(appType);
		StyledString styledString = new StyledString(sb.toString());
		styledString.setStyle(appName.length() + 1, appType.length(), StyledString.QUALIFIER_STYLER);
		return styledString;
	}

	private StyledString createStyledString(IEmbeddedCartridge cartridge) {
		String displayName = cartridge.getDisplayName();
		String name = cartridge.getName();
		StringBuilder sb = new StringBuilder(displayName).append(' ').append(name);
		StyledString styledString = new StyledString(sb.toString());
		styledString.setStyle(displayName.length() + 1, name.length(), StyledString.QUALIFIER_STYLER);
		return styledString;
	}
}
