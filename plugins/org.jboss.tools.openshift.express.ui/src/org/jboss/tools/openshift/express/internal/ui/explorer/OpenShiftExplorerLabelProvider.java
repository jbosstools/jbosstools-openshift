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
package org.jboss.tools.openshift.express.internal.ui.explorer;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
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
		if (element instanceof Connection) {
			image = OpenShiftImages.OPENSHIFT_LOGO_WHITE_ICON_IMG;
		} else if (element instanceof IDomain) {
			image = OpenShiftImages.GLOBE_IMG;
		} else if (element instanceof IApplication) {
			image = OpenShiftImages.QUERY_IMG;
		} else if (element instanceof IEmbeddedCartridge) {
			image = OpenShiftImages.TASK_REPO_IMG;
		} else if (element instanceof LoadingStub) {
			image = OpenShiftImages.SYSTEM_PROCESS_IMG;
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
		if (element instanceof Connection) {
			styledString = createStyledString((Connection) element);
		} else if (element instanceof IDomain) {
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
		}
		return styledString;
	}

	private StyledString createStyledString(Connection connection) {
		String name = connection.getUsername();
		String host = connection.getHost();
		StringBuilder builder = new StringBuilder(name).append(' ').append(host);
		if (connection.isDefaultHost()) {
			builder.append(' ').append(DEFAULT_MARKER);
		}
		String label = builder.toString();
		StyledString styledString = new StyledString(label);
		styledString.setStyle(name.length() + 1, builder.length() - name.length() - 1, StyledString.QUALIFIER_STYLER);
		return styledString;
	}

	private StyledString createStyledString(IDomain domain) {
		String id = domain.getId();
		String fullName = 
				new StringBuilder(id).append('.').append(domain.getSuffix()).toString(); 
		String label = 
				new StringBuilder(id).append(' ').append(fullName).toString();

		StyledString styledString = new StyledString(label);
		styledString.setStyle(id.length() + 1, fullName.length() - 1, StyledString.QUALIFIER_STYLER);
		return styledString;
	}

	private StyledString createStyledString(IApplication application) {
		String appName = application.getName();
		String appType = StringUtils.null2emptyString(OpenShiftResourceUtils.toString(application.getCartridge()));
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
