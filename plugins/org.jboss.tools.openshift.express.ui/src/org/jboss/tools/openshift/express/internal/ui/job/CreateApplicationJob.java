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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * @author Andre Dietisheim
 */
public class CreateApplicationJob extends AbstractDelegatingMonitorJob {

	private static final int TIMEOUT = 60 * 10 * 1000;
	
	private String name;
	private IStandaloneCartridge cartridge;
	private ApplicationScale scale;
	private IGearProfile gear;
	private IApplication application;
	private IDomain domain;
	private String initialGitUrl;
	private Collection<IEmbeddableCartridge> embeddableCartridges;
	
	public CreateApplicationJob(final String name, final IStandaloneCartridge cartridge, final ApplicationScale scale,
			final IGearProfile gear, IDomain domain) {
		this(name, cartridge, scale, gear, null, null, domain);
	}

	public CreateApplicationJob(final String name, final IStandaloneCartridge cartridge, final ApplicationScale scale,
			final IGearProfile gear, String initialGitUrl, Collection<IEmbeddableCartridge> embeddableCartridges, IDomain domain) {
		super(NLS.bind((embeddableCartridges == null ?
				OpenShiftExpressUIMessages.CREATING_APPLICATION : OpenShiftExpressUIMessages.CREATING_APPLICATION_WITH_EMBEDDED)
				, name));
		this.name = name;
		this.cartridge = cartridge;
		this.scale = scale;
		this.gear = gear;
		this.initialGitUrl = initialGitUrl;
		this.domain = domain;
		this.embeddableCartridges = embeddableCartridges;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			try {
				if (embeddableCartridges == null
						|| embeddableCartridges.isEmpty()) {
					this.application = domain.createApplication(
							name, cartridge, scale, gear, initialGitUrl, TIMEOUT);
				} else {
					this.application = domain.createApplication(
							name, cartridge, scale, gear, initialGitUrl, TIMEOUT, embeddableCartridges.toArray(new IEmbeddableCartridge[embeddableCartridges.size()]));
				}
				return new Status(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, OK, "timeouted", null);
			} catch (OpenShiftTimeoutException e) {
				this.application = refreshAndCreateApplication(monitor);
				if (application != null) {
					// creation went ok, but initial request timed out
					return new Status(IStatus.OK, OpenShiftUIActivator.PLUGIN_ID, TIMEOUTED, "timeouted", null);
				} else {
					return new Status(IStatus.CANCEL, OpenShiftUIActivator.PLUGIN_ID, TIMEOUTED, "timeouted", null);
				}
			}
		} catch (Exception e) {
			safeRefreshDomain();
			return OpenShiftUIActivator.createErrorStatus(
					OpenShiftExpressUIMessages.COULD_NOT_CREATE_APPLICATION, e, name);
		}
	}



	private IApplication refreshAndCreateApplication(IProgressMonitor monitor) throws OpenShiftException {
		if (monitor.isCanceled()) {
			return null;
		}
		IApplication application = null;
		do {
			try {
				domain.refresh();
				application = domain.getApplicationByName(name);
				if (application == null) {
					// app is not created yet, try again
					application = domain.createApplication(name, cartridge, scale, gear);
				}
			} catch (OpenShiftTimeoutException ex) {
				// ignore
			}
		} while (application == null
				&& openKeepTryingDialog()
				&& !monitor.isCanceled());
		return application;
	}

	private void safeRefreshDomain() {
		try {
			domain.refresh();
		} catch (OpenShiftException e) {
			OpenShiftUIActivator.log(e);
		}
	}
	
	public IApplication getApplication() {
		return application;
	}
	
	public List<IEmbeddedCartridge> getAddedCartridges() {
		return application.getEmbeddedCartridges();
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
