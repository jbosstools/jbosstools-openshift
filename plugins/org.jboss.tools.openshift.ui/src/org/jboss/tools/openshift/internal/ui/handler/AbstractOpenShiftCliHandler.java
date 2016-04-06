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
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import static org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants.OPEN_SHIFT_PREFERENCE_PAGE_ID;

/**
 * 
 * @author jeff.cantrill
 *
 */
public abstract class AbstractOpenShiftCliHandler extends AbstractHandler {
	
	protected abstract void handleEvent(ExecutionEvent event);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String location = OCBinary.getInstance().getLocation();
		if(StringUtils.isBlank(location)) {
			
			final MessageDialog dialog = new MessageDialog(HandlerUtil.getActiveShell(event),
													"Unknown executable location",
													null,
													"The OpenShift Client '"+ OCBinary.getInstance().getName()+"' executable can not be found.",
													MessageDialog.ERROR,
													new String[] { IDialogConstants.OK_LABEL }, 0) {
				@Override
				protected Control createCustomArea(Composite parent) {
					Composite container = new Composite(parent, SWT.NONE);
					GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
					GridLayoutFactory.fillDefaults().applyTo(container);
					Link link = new Link(container, SWT.WRAP);
					link.setText("You must set the executable location in the <a>OpenShift 3 preferences</a>.");
					link.addSelectionListener(new OpenPreferencesListener(this));
					container.setFocus();
					return container;
				}
			};
			dialog.open();
			return null;
		}
		handleEvent(event);
		return null;
	}

	private static class OpenPreferencesListener extends SelectionAdapter {

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
