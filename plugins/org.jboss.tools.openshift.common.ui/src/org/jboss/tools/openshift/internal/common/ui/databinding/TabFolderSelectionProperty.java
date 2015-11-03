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
package org.jboss.tools.openshift.internal.common.ui.databinding;

import org.eclipse.jface.databinding.swt.WidgetValueProperty;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TabFolder;

/**
 * Value property implementation for {@link TabFolder} selected tab.
 * 
 * @author Fred Bricon
 */
public class TabFolderSelectionProperty extends WidgetValueProperty {

	public TabFolderSelectionProperty() {
		super(SWT.Selection);
	}
	
	@Override
	public Object getValueType() {
		return Integer.TYPE;
	}

	@Override
	protected Object doGetValue(Object source) {
		return Integer.valueOf(((TabFolder)source).getSelectionIndex());
	}

	@Override
	protected void doSetValue(Object source, Object value) {
		((TabFolder)source).setSelection((Integer) value);
	}

}
