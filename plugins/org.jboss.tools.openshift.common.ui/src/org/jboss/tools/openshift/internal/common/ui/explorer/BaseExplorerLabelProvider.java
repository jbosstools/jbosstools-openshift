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
package org.jboss.tools.openshift.internal.common.ui.explorer;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.IDescriptionProvider;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.OpenShiftCommonImages;

public abstract class BaseExplorerLabelProvider implements IStyledLabelProvider, ILabelProvider, IDescriptionProvider {
	//Limit for label length = baseText.length + qualifiedText.length
	private static final int DEFAULT_LABEL_LIMIT = 60;
	
	//Status line has with CLabel its own ellipses adjusting string to available space.
	//We could set Integer.MAX_VALUE as limit, but performance will be too low for really long texts.
	private static final int STATUS_LINE_LABEL_LIMIT = 512;


	protected int labelLimit;
	
	protected int statusLineLabelLimit;

	public BaseExplorerLabelProvider() {
		setLabelLimit(DEFAULT_LABEL_LIMIT);
		setStatusLineLabelLimit(STATUS_LINE_LABEL_LIMIT);
	}

	/**
	 * Set limit of label length in characters. 
	 * @param limit
	 */
	public void setLabelLimit(int limit) {
		if (limit <= 0) {
			throw new IllegalArgumentException("Label limit cannot be less than 1: " + limit);
		}
		labelLimit = limit;
	}

	/**
	 * Set limit of label length in characters. 
	 * @param limit
	 */
	public void setStatusLineLabelLimit(int limit) {
		if (limit <= 0) {
			throw new IllegalArgumentException("Label limit cannot be less than 1: " + limit);
		}
		statusLineLabelLimit = limit;
	}


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
		if (element instanceof BaseExplorerContentProvider.LoadingStub) {
			return OpenShiftCommonImages.SYSTEM_PROCESS_IMG;
		}
		if (element instanceof Exception) {
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
		return getStyledText(element, labelLimit);
	}

	public StyledString getStyledText(Object element, int limit) {
		if (element instanceof BaseExplorerContentProvider.LoadingStub) {
			return new StyledString("Loading...");
		}
		if (element instanceof Exception) {
			Exception exception = (Exception) element;
			return new StyledString(org.apache.commons.lang.StringUtils.defaultIfBlank(exception.getMessage(), exception.getClass().getName()));
		}
		return new StyledString(element.toString());
	}
	
	protected void applyEllipses(String[] parts, int limit) {
		StringUtils.shorten(parts, limit);
	}

	protected StyledString style(String baseText, String qualifiedText, int limit) {
		String[] parts = new String[] { baseText, qualifiedText };
		applyEllipses(parts, limit);
		baseText = parts[0];
		qualifiedText = parts[1];
		StyledString value = new StyledString(baseText);
		if (org.apache.commons.lang.StringUtils.isNotBlank(qualifiedText)) {
			value.append(" ").append(qualifiedText, StyledString.QUALIFIER_STYLER);

		}
		return value;
	}
	
	@Override
	public String getDescription(Object anElement) {
		return getStyledText(anElement, statusLineLabelLimit).getString();
	}
}
