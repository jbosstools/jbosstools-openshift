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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;

import com.openshift.client.ApplicationBuilder;
import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.OpenShiftTimeoutException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IEmbeddedCartridge;
import com.openshift.internal.client.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class CreateApplicationJob extends AbstractDelegatingMonitorJob {

	private String name;
	private ApplicationScale scale;
	private IGearProfile gear;
	private IApplication application;
	private IDomain domain;
	private String initialGitUrl;
	private Map<String, String> environmentVariables;
	private Collection<ICartridge> cartridges;
	
	public CreateApplicationJob(final String name,final ApplicationScale scale, final IGearProfile gear, ICartridge cartridge, IDomain domain) {
		this(name, scale, gear, null, new LinkedHashMap<String, String>(), Collections.<ICartridge>singletonList(cartridge), domain);
	}

	public CreateApplicationJob(final String name, final ApplicationScale scale,
			final IGearProfile gear, String initialGitUrl, Map<String, String> environmentVariables, Collection<ICartridge> cartridges, IDomain domain) {
		super(NLS.bind(
				(cartridges == null ?
						OpenShiftExpressUIMessages.CREATING_APPLICATION
						: OpenShiftExpressUIMessages.CREATING_APPLICATION_WITH_EMBEDDED)
				, name));
		this.name = name;
		this.scale = scale;
		this.gear = gear;
		this.initialGitUrl = initialGitUrl;
		this.domain = domain;
		this.environmentVariables = environmentVariables;
		this.cartridges = cartridges;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		try {
			Assert.isLegal(!StringUtils.isEmpty(name), "No application name provided.");
			Assert.isLegal(domain != null, "No domain provided.");
			Assert.isLegal(cartridges != null 
					&& cartridges.size() >= 1, "No application type provided.");
			try {
				this.application = new ApplicationBuilder(domain)
					.setName(name)
					.setCartridges(cartridges)
					.setGearProfile(gear)
					.setApplicationScale(scale)
					.setEnvironmentVariables(environmentVariables)
					.setInitialGitUrl(initialGitUrl)
					.build();
				return new Status(IStatus.OK, ExpressUIActivator.PLUGIN_ID, OK, "timeouted", null);
			} catch (OpenShiftTimeoutException e) {
				this.application = refreshAndCreateApplication(monitor);
				if (application != null) {
					// creation went ok, but initial request timed out
					return new Status(IStatus.OK, ExpressUIActivator.PLUGIN_ID, TIMEOUTED, "timeouted", null);
				} else {
					return new Status(IStatus.CANCEL, ExpressUIActivator.PLUGIN_ID, TIMEOUTED, "timeouted", null);
				}
			}
		} catch (Exception e) {
			safeRefreshDomain();
			return ExpressUIActivator.createErrorStatus(
					OpenShiftExpressUIMessages.COULD_NOT_CREATE_APPLICATION, e, StringUtils.nullToEmptyString(name));
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
					application = new ApplicationBuilder(domain)
						.setName(name)
						.setCartridges(cartridges)
						.setGearProfile(gear)
						.setInitialGitUrl(initialGitUrl)
						.setEnvironmentVariables(environmentVariables)
						.build();
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
			ExpressUIActivator.log(e);
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
								, NLS.bind("Could not create application {0}. ExpressConnection timed out.\n\nKeep trying?",
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
