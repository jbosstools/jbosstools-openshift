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
package org.jboss.tools.openshift.internal.ui.handler;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.preferences.OpenShiftPreferencePage.OCBinaryName;

import com.openshift.restclient.capability.resources.IPortForwardable;

/**
 * 
 * @author jeff.cantrill
 *
 */
public abstract class AbstractOpenShiftCliHandler extends AbstractHandler {
	
	protected abstract void handleEvent(ExecutionEvent event);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//check binary locations
		final String location = OpenShiftUIActivator.getDefault().getCorePreferenceStore().getString(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);
		if(StringUtils.isBlank(location)) {
			
			final MessageDialog dialog = new MessageDialog(HandlerUtil.getActiveShell(event),
													"Unknown binary location",
													null,
													"The OpenShift Client '"+ OCBinaryName.getInstance().getName()+"' binary can not be found.",
													MessageDialog.ERROR,
													new String[] { IDialogConstants.OK_LABEL }, 0) {
				@Override
				protected Control createCustomArea(Composite parent) {
					Composite container = new Composite(parent, SWT.NONE);
					GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
					GridLayoutFactory.fillDefaults().applyTo(container);
					Link link = new Link(container, SWT.WRAP);
					link.setText("You must set the binary location in the <a>OpenShift 3 preferences</a>.");
					link.addSelectionListener(new OpenPreferencesListener(this));
					container.setFocus();
					return container;
				}
			};
			dialog.open();
			return null;
		}
		System.setProperty(IPortForwardable.OPENSHIFT_BINARY_LOCATION, location);
		handleEvent(event);
		return null;
	}

	private static class OpenPreferencesListener extends SelectionAdapter {

		private static final String OPEN_SHIFT_PREFERENCE_PAGE_ID = "org.jboss.tools.openshift.ui.preferences.OpenShiftPreferencePage";
		private Dialog dialog;

		public OpenPreferencesListener(MessageDialog messageDialog) {
			this.dialog = messageDialog;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			dialog.close();
			//Opening in asyncExec to workaround https://bugs.eclipse.org/471717 on OSX
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
							OPEN_SHIFT_PREFERENCE_PAGE_ID,
							new String[] {OPEN_SHIFT_PREFERENCE_PAGE_ID},
							null
							).open();
				}
			});
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}
}
