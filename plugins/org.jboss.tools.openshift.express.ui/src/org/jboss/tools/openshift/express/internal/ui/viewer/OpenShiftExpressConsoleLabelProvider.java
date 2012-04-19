/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.viewer.OpenShiftExpressConsoleContentProvider.LoadingStub;
import org.jboss.tools.openshift.express.internal.ui.viewer.OpenShiftExpressConsoleContentProvider.NotConnectedUserStub;

import com.openshift.client.IApplication;
import com.openshift.client.IEmbeddedCartridge;
import com.openshift.client.OpenShiftException;

/**
 * @author Xavier Coulon
 */
public class OpenShiftExpressConsoleLabelProvider implements IStyledLabelProvider, ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof UserDelegate) {
			return OpenShiftUIActivator.getDefault().createImage("repository-middle.gif");
		}
		if (element instanceof IApplication) {
			return OpenShiftUIActivator.getDefault().createImage("query.gif");
		}
		if (element instanceof IEmbeddedCartridge) {
			return OpenShiftUIActivator.getDefault().createImage("task-repository.gif");
		}
		if (element instanceof LoadingStub) {
			return OpenShiftUIActivator.getDefault().createImage("systemprocess.gif");
		}
		if (element instanceof OpenShiftException ) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof UserDelegate) {
			String message = ((UserDelegate) element).getRhlogin();
			StyledString styledString = new StyledString(message);
			styledString.setStyle(0, message.length(), StyledString.DECORATIONS_STYLER);
			return new StyledString(message);
		}
		if (element instanceof IApplication) {
			IApplication app = (IApplication) element;
			String appName = app.getName();
			String appType = app.getCartridge().getName();
			StringBuilder sb = new StringBuilder();
			sb.append(appName);
			sb.append(" ");
			sb.append(appType);
			StyledString styledString = new StyledString(sb.toString());
			styledString.setStyle(appName.length() + 1, appType.length(), StyledString.QUALIFIER_STYLER);
			return styledString;
		}
		if (element instanceof IEmbeddedCartridge) {
			String message = ((IEmbeddedCartridge) element).getName();
			StyledString styledString = new StyledString(message);
			styledString.setStyle(0, message.length(), StyledString.DECORATIONS_STYLER);
			return new StyledString(message);
		}

		if (element instanceof LoadingStub) {
			return new StyledString(OpenShiftExpressUIMessages.LOADING_USER_APPLICATIONS_LABEL);
		}
		if (element instanceof NotConnectedUserStub) {
			return new StyledString(OpenShiftExpressUIMessages.USER_NOT_CONNECTED_LABEL);
		}
		if (element instanceof OpenShiftException ) {
			return new StyledString( ((OpenShiftException)element).getMessage());
		}
		return null;
	}

}
