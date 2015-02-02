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
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

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
		if (element instanceof IConnection) {
			return OpenShiftCommonImages.OPENSHIFT_LOGO_WHITE_ICON_IMG;
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return element.toString();
	}

	@Override
	public StyledString getStyledText(Object element) {
		return new StyledString(element.toString());
//		StyledString styledString = null;
//FIXME leaving here as examples of what we might do
		//		if (element instanceof IConnection) {
//			styledString = createStyledString((IConnection) element);
//		} else if (element instanceof Project){
//			Project p = (Project) element;
//			String label = 
//					new StringBuilder(p.getDisplayName()).append(" (ns:").append(p.getNamespace()).append(')').toString();
//
//			styledString = new StyledString(label);
//			styledString.setStyle(p.getDisplayName().length() +1, label.length() - p.getDisplayName().length() - 1, StyledString.QUALIFIER_STYLER);
//		} else if (element instanceof Service){
//			Service s = (Service) element;
//			StringBuilder b = new StringBuilder(s.getName());
//			b.append(" (selector: ").append(s.getSelector()).append(")");
//			styledString = new StyledString(b.toString());
//			styledString.setStyle(s.getName().length() + 1,b.length() - s.getName().length() -1 , StyledString.QUALIFIER_STYLER);
//		} else if (element instanceof DeploymentConfig){
//			DeploymentConfig config = (DeploymentConfig) element;
//			StringBuilder b = new StringBuilder(config.getName());
//			styledString = new StyledString(b.toString());
//		} else if (element instanceof BuildConfig){
//			BuildConfig config = (BuildConfig) element;
//			StringBuilder b = new StringBuilder(config.getName());
//			b.append(" ").append(config.getSourceUri());
//			styledString = new StyledString(b.toString());
//			styledString.setStyle(config.getName().length() + 1,b.length() - config.getName().length() -1 , StyledString.QUALIFIER_STYLER);
	}

}
