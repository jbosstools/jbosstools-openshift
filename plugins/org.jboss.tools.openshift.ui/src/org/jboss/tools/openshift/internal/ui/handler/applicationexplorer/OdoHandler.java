/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.util.function.Consumer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Red Hat Developers
 */
public abstract class OdoHandler extends AbstractHandler {

	private static class OdoNotification extends AbstractNotificationPopup {
		private String text;

		OdoNotification(Shell shell, String text) {
			super(shell.getDisplay());
			this.text = text;
		}

		@Override
		protected void createContentArea(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setText(text);
		}
	}
	
	protected static class Notification {
		private OdoNotification window;
		
		public void close() {
			if (window != null) {
				window.close();
			}
		}
	}
	
	protected Notification openNotification(Notification previous, Shell shell, String text) {
		Notification notification = new Notification();
		shell.getDisplay().asyncExec(() -> {
			if (previous != null){
				previous.close();
			}
			notification.window = new OdoNotification(shell, text);
			notification.window.open();
		});
		return notification;
	}
	
	protected Notification openNotification(Shell shell, String text) {
		return openNotification(null, shell, text);
	}

	protected void executeInJob(String name, Runnable action) {
		Job job = Job.create(name, monitor -> {
			try {
				action.run();
				return Status.OK_STATUS;
			}
			catch (Exception e) {
				return OpenShiftUIActivator.statusFactory().errorStatus(e);
			}
		});
		job.schedule();
	}
}
