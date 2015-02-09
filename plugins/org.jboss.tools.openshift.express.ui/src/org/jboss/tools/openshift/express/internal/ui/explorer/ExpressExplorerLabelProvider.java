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

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.util.ExpressResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressImages;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerContentProvider;
import org.jboss.tools.openshift.internal.common.ui.explorer.BaseExplorerLabelProvider;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddedCartridge;

/**
 * @author Xavier Coulon
 * @author Andre Dietisheim
 */
public class ExpressExplorerLabelProvider extends BaseExplorerLabelProvider {

	private static final String DEFAULT_MARKER = "(default)";

	@Override
	public Image getImage(Object element) {
		if (element instanceof IDomain) {
			return ExpressImages.GLOBE_IMG;
		}
		if (element instanceof IApplication) {
			return ExpressImages.QUERY_IMG;
		} 
		if (element instanceof IEmbeddedCartridge) {
			return ExpressImages.TASK_REPO_IMG;
		} 
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof ExpressConnection) {
			return createStyledString((ExpressConnection) element);
		} else if (element instanceof IDomain) {
			return createStyledString((IDomain) element);
		} else if (element instanceof IApplication) {
			return createStyledString((IApplication) element);
		} else if (element instanceof IEmbeddedCartridge) {
			return createStyledString((IEmbeddedCartridge) element);
		} else if (element instanceof BaseExplorerContentProvider.LoadingStub) {
			return new StyledString(OpenShiftExpressUIMessages.LOADING_USER_APPLICATIONS_LABEL);
		} else if (element instanceof BaseExplorerContentProvider.NotConnectedUserStub) {
			return new StyledString(OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL);
		} else if (element instanceof OpenShiftException) {
			return new StyledString(((OpenShiftException) element).getMessage());
		}
		return super.getStyledText(element);
	}

	private StyledString createStyledString(ExpressConnection connection) {
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
