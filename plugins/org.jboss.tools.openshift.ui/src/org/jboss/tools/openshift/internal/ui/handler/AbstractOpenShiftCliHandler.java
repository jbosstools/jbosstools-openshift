/*******************************************************************************
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler;

import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.DOWNLOAD_URL;
import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.OPEN_SHIFT_PREFERENCE_PAGE_ID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.OpenShiftCoreMessages;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.core.job.JobChainBuilder;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinary;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryValidationJob;
import org.jboss.tools.openshift.internal.core.ocbinary.OCBinaryValidator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.model.IResource;

/**
 * 
 * @author jeff.cantrill
 *
 */
public abstract class AbstractOpenShiftCliHandler extends AbstractHandler {

	protected abstract void handleEvent(ExecutionEvent event);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		OCBinaryValidationJob validationJob = new OCBinaryValidationJob(
				OCBinary.getInstance().getPath(getConnection(event)), 
				true,
				OCBinaryValidator.NON_RSYNC_REQUIREMENTS);
		new JobChainBuilder(validationJob)
			.runWhenSuccessfullyDone(new UIJob(HandlerUtil.getActiveShell(event).getDisplay(), "") {
				
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IStatus status = validationJob.getOCVersionValidity();
					if (status.isOK()) {
						handleEvent(event);
					} else {
						showOCBinaryErrorMessage(status, HandlerUtil.getActiveShell(event));
					}
					return Status.OK_STATUS;
				}
			})
			.schedule();
		return null;
	}

	private void showOCBinaryErrorMessage(IStatus status, Shell shell) {
		new MessageDialog(shell,
				"OpenShift Client Error", 
				null,
				"There's a problem with the OpenShift Client '" + OCBinary.getInstance().getName() + "':",
				MessageDialog.ERROR, 
				new String[] { IDialogConstants.OK_LABEL }, 0) {
			@Override
			protected Control createCustomArea(Composite parent) {
				Composite container = new Composite(parent, SWT.NONE);
				GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
				GridLayoutFactory.fillDefaults().applyTo(container);
				Link link = new Link(container, SWT.WRAP);
				link.setText(status.getMessage());
				link.addSelectionListener(new ErrorLinksListener(this));
				container.setFocus();
				return container;
			}
		}.open();

	}

	private static class ErrorLinksListener extends SelectionAdapter {

		private Dialog dialog;

		public ErrorLinksListener(Dialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			dialog.close();

			if (OpenShiftCoreMessages.OCBinaryPreferencesDeactivatedLink.equals(e.text)) {
				onPreferencesClicked();
			} else if (OpenShiftCoreMessages.OCBinaryDownloadDeactivatedLink.equals(e.text)){
				onDownloadClicked();
			}
		}

		private void onPreferencesClicked() {
			//Opening in asyncExec to workaround https://bugs.eclipse.org/471717 on OSX
			Display.getDefault().asyncExec(() ->
					PreferencesUtil
							.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
									OPEN_SHIFT_PREFERENCE_PAGE_ID, new String[] { OPEN_SHIFT_PREFERENCE_PAGE_ID }, null)
							.open()
			);
		}

		private void onDownloadClicked() {
			new BrowserUtility().checkedCreateExternalBrowser(DOWNLOAD_URL,
					OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

	protected IConnection getConnection(ExecutionEvent event) {
        Connection connection = null;
		IResource resource = getSelectedElement(event, IResource.class);
		if (resource != null) {
			connection = ConnectionsRegistryUtil.safeGetConnectionFor(resource);
		}
		return connection;
	}

	protected <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
		ISelection selection = UIUtils.getCurrentSelection(event);
		return UIUtils.getFirstElement(selection, klass);
	}

}
