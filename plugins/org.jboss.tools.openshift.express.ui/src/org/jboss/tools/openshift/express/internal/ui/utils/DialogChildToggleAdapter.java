/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Andre Dietisheim
 */
public class DialogChildToggleAdapter {

	private boolean visible;
	private final Composite composite;
	private final GridData gridData;
	private final Shell shell;
	
	public DialogChildToggleAdapter(Composite composite, Shell shell, boolean visible) {
		Assert.isTrue(composite != null && !composite.isDisposed());
		this.composite = composite;
		Object layoutData = composite.getLayoutData();
		Assert.isTrue(layoutData instanceof GridData, "only supports GridLayout");
		this.gridData = (GridData) layoutData;
		Assert.isTrue(shell != null	&& !shell.isDisposed());
		this.shell = shell;
		this.visible = visible;
		composite.setVisible(visible);
		gridData.exclude = !visible;
	}
	
	public void toggle() {
		this.visible = !visible;
		composite.setVisible(visible);
		gridData.exclude = !visible;
		Point compositeSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point shellSize = shell.getSize();
		if (visible) {
			shellSize.y = shellSize.y + compositeSize.y;
		} else {
			shellSize.y = shellSize.y - compositeSize.y;
		}
		shell.setSize(shellSize.x, shellSize.y);
		shell.layout(true, true);
	}

	public boolean isVisible() {
		return visible;
	}
	
}
