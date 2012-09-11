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
package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.progress.UIJob;

/**
 * @author Andre Dietisheim
 */
public class RefreshViewerJob extends UIJob {

	private StructuredViewer viewer;

	public RefreshViewerJob(StructuredViewer viewer) {
		super("Refreshing viewer...");
		this.viewer = viewer;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		viewer.refresh();
		return Status.OK_STATUS;
	}
	
	public static void refresh(StructuredViewer viewer) {
		new RefreshViewerJob(viewer).schedule();
	}
}
