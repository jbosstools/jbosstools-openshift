/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IStatus;

/**
 * @author jeff.cantrill
 */
public class ResourceSummaryLabelProvider  implements IStyledLabelProvider, ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener arg0) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {
	}

	@Override
	public String getText(Object arg) {
		if(arg instanceof IResource) {
			IResource resource = (IResource) arg;
			if(isFailedStatus(resource)) {
				return org.apache.commons.lang.StringUtils.capitalize(((IStatus) resource).getMessage());
			}
			return NLS.bind("{0} - {1}", resource.getKind(), resource.getName());
		}
		return arg.toString();
	}

	@Override
	public Image getImage(Object arg) {
		if(arg instanceof IResource) {
			IResource resource = (IResource) arg;
			if(isFailedStatus(resource)) {
				return OpenShiftCommonImages.ERROR;
			}
			return OpenShiftCommonImages.OK_IMG;
		}
		return null;
	}

	@Override
	public StyledString getStyledText(Object arg) {
		if(arg instanceof IResource) {
			IResource resource = (IResource)arg;
			StyledString text = new StyledString();
			if(isFailedStatus(resource)) {
				text.append(((IStatus) resource).getMessage());
			}else {
				text.append(StringUtils.humanize(resource.getKind().toString()));
				text.append(resource.getName(), StyledString.QUALIFIER_STYLER);
			}
			return text;
		}
		return null;
	}
	
	private boolean isFailedStatus(IResource resource) {
		return ResourceKind.STATUS.equals(resource.getKind()) && ((IStatus) resource).isFailure(); 
	}

}
