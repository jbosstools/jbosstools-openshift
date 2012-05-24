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
package org.jboss.tools.openshift.express.internal.ui.job;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;

/**
 * @author Andre Dietisheim
 */
public class CreateApplicationJob extends AbstractDelegatingMonitorJob {

	private UserDelegate user;
	private String name;
	private ICartridge cartridge;
	private ApplicationScale scale;
	private IGearProfile gear;
	private IApplication application;
	private ReentrantLock lock = new ReentrantLock();

	public CreateApplicationJob(final String name, final ICartridge cartridge, final ApplicationScale scale,
			final IGearProfile gear, UserDelegate user) {
		super(NLS.bind(OpenShiftExpressUIMessages.CREATING_APPLICATION, name));
		this.name = name;
		this.cartridge = cartridge;
		this.scale = scale;
		this.gear = gear;
		this.user = user;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			lock.lock();
			try {
				this.application = user.createApplication(name, cartridge, scale, gear);
			} catch (OpenShiftTimeoutException e) {
				this.application = refreshAndCreateApplication(monitor);
			}
			if (application == null) {
				return OpenShiftUIActivator.createCancelStatus(NLS.bind(
						"User cancelled creation of application {0}", name));
			} else {
				return Status.OK_STATUS;
			}
		} catch (Exception e) {
			return OpenShiftUIActivator.createErrorStatus(
					OpenShiftExpressUIMessages.COULD_NOT_CREATE_APPLICATION, e, name);
		} finally {
			lock.unlock();
		}
	}

	private IApplication refreshAndCreateApplication(IProgressMonitor monitor) throws OpenShiftException {
		IApplication application = null;
		do {
			try {
				IDomain domain = user.getDefaultDomain();
				domain.refresh();
				application = domain.getApplicationByName(name);
				if (application == null) {
					// app is not created yet, try again
					application = user.createApplication(name, cartridge, scale, gear);
				}
			} catch (OpenShiftTimeoutException ex) {
				// ignore
			}
		} while (application == null
				&& openKeepTryingDialog()
				&& !monitor.isCanceled());
		return application;
	}

	public IApplication getApplication() {
		try {
			lock.lock();
			return application;
		} finally {
			lock.unlock();
		}
	}

	protected boolean openKeepTryingDialog() {
		final AtomicBoolean keepTrying = new AtomicBoolean(false);
		final Display display = Display.getDefault();
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				MessageDialog dialog =
						new MessageDialog(display.getActiveShell()
								, NLS.bind("Creating {0}", name)
								, display.getSystemImage(SWT.ICON_QUESTION)
								, NLS.bind("Could not create application {0}. Connection timed out.\n\nKeep trying?",
										name)
								, MessageDialog.QUESTION
								, new String[] { "Keep trying",
										OpenShiftExpressUIMessages.BTN_CLOSE_WIZARD }
								, MessageDialog.QUESTION);
				// style &= SWT.SHEET;
				// dialog.setShellStyle(dialog.getShellStyle() | style);
				keepTrying.set(dialog.open() == IDialogConstants.OK_ID);
			}
		});
		return keepTrying.get();
	}

}
