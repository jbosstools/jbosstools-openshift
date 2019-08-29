/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.dialog;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jboss.tools.openshift.core.IDialogProvider;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

public class DialogProvider implements IDialogProvider {

	private static final String MSG_DONT_REMIND_AGAIN = "Dont remind me again";

	@Override
	public void warn(String title, String message, String preferencesKey) {
		boolean show = isShowDialog(preferencesKey);
		if (show) {
			Display.getDefault().syncExec(() -> 
				MessageDialogWithToggle.openWarning(
						Display.getDefault().getActiveShell(),
						title,
						message,
						MSG_DONT_REMIND_AGAIN,
						!show,
						OpenShiftUIActivator.getDefault().getPreferenceStore(),
						preferencesKey)					
				);
		}
	}

	@Override
	public int message(String title, int type, String message, Consumer<String> callback,
			LinkedHashMap<String, Integer> buttonLabelToIdMap, int defaultButton, String preferencesKey) {
		boolean show = isShowDialog(preferencesKey);
		if (!show) {
			return IDialogProvider.NO_ID;
		}
		final int[] answer = new int[1];
		Display.getDefault().syncExec(() -> {
				MessageDialogWithToggle dialog = 
					new LinkTextMessageDialogWithToggle(Display.getDefault().getActiveShell(), 
						title,
						null,
						message,
						type | SWT.SHEET, 
						buttonLabelToIdMap,
						defaultButton,
						MSG_DONT_REMIND_AGAIN,
						!show,
						callback);
				dialog.setPrefStore(OpenShiftUIActivator.getDefault().getPreferenceStore());
				dialog.setPrefKey(preferencesKey);
				answer[0] = dialog.open();
		});
		return answer[0];
	}

	private boolean isShowDialog(String preferencesKey) {
		IPreferenceStore preferences = OpenShiftUIActivator.getDefault().getPreferenceStore();
		String prefsValue = preferences.getString(preferencesKey);
		return !MessageDialogWithToggle.ALWAYS.equals(prefsValue);
	}

	@Override
	public void preferencePage(String page) {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
				Display.getDefault().getActiveShell(),
				page,
				new String[] {}, 
				null);
		dialog.open();
	}
	
	private class LinkTextMessageDialogWithToggle extends MessageDialogWithToggle {

		private Consumer<String> callback;
		private String linkText;

		public LinkTextMessageDialogWithToggle(Shell parentShell, String dialogTitle, Image image, String message,
				int dialogImageType, LinkedHashMap<String, Integer> buttonLabelToIdMap, int defaultIndex, String toggleMessage,
				boolean toggleState, Consumer<String> callback) {
			super(parentShell, dialogTitle, image, null, dialogImageType, buttonLabelToIdMap, defaultIndex, toggleMessage, toggleState);
			this.callback = callback;
			this.linkText = message;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Link link = new Link(parent, SWT.None);
			link.setText(linkText);
			link.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Display.getDefault().syncExec(() ->	callback.accept(e.text));
				}
				
			});
			return link;
		}
	}
}
