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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;

import com.openshift3.client.model.IProject;
import com.openshift3.client.model.IResource;

public class OpenShiftExplorerLabelProvider implements ILabelProvider { 


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
		if(element instanceof IProject){
			return ((IProject) element).getDisplayName();
		}
		if(element instanceof IResource){
			return ((IResource) element).getName();
		}
		if(element instanceof ResourceGrouping){
			return StringUtils.humanize(((ResourceGrouping) element).getKind().pluralize());
		}
		return element.toString();
	}

}
