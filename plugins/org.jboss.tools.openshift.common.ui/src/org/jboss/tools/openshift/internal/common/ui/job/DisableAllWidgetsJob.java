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
package org.jboss.tools.openshift.internal.common.ui.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 */
public class DisableAllWidgetsJob extends UIJob {

	private boolean disableWidgets;
	private Cursor cursor;
	private boolean disableCursor;
	private Composite container;

	public DisableAllWidgetsJob(boolean disable, Composite container, Cursor cursor) {
		this(disable, container, disable, cursor);
	}
	
	public DisableAllWidgetsJob(boolean disableWidgets, Composite container, boolean disableCursor, Cursor cursor) {
		super((disableWidgets? "Disabling" : "Enabling") + " all controls...");
		this.disableWidgets = disableWidgets;
		this.container = container;
		this.disableCursor = disableCursor;
		this.cursor = cursor;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		UIUtils.enableAllChildren(!disableWidgets, container);
		if (!disableCursor) {
			container.setCursor(null);
			if (cursor != null) {
				cursor.dispose();
			}
		} else {
			if (cursor != null) {
				container.setCursor(cursor);
			}
		}
		return Status.OK_STATUS;
	}

}
