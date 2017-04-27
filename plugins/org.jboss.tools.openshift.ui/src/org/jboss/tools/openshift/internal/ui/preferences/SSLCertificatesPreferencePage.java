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
package org.jboss.tools.openshift.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.openshift.core.connection.HostCertificate;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.wizard.connection.SSLCertificateUIHelper;

public class SSLCertificatesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private List<HostCertificate> items = new ArrayList<>();
	private CheckboxTableViewer listViewer;
	private Button remove;
	private StyledText details;
	
	public SSLCertificatesPreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(SSLCertificatesPreference.getInstance().getPreferenceStore());		
		items.addAll(SSLCertificatesPreference.getInstance().getSavedItems());
	}

	@Override
	protected void createFieldEditors() {
		Composite composite = getFieldEditorParent();
		adjustGridLayout();

		Label label = new Label(composite, SWT.NONE);
		GridData dl = new GridData();
		dl.horizontalSpan = 3;
		label.setLayoutData(dl);
		label.setText("Decisions on untrusted SSL certificates, checked ones are accepted.");		

		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		data.heightHint = 100;
		listViewer.getTable().setLayoutData(data);
		listViewer.setLabelProvider(new LP());
		listViewer.setContentProvider(new CP());
		listViewer.getTable().setLinesVisible(true);
		listViewer.getTable().setHeaderVisible(true);

		String[] columnNames = new String[] {"Approved", "Certificate", "Validity"};
		int[] columnWidths = new int[] {80, 200, 100};

		for (int i = 0; i < columnNames.length; i++) {
			TableColumn tc = new TableColumn(listViewer.getTable(), SWT.LEFT);
			tc.setText(columnNames[i]);
			tc.setWidth(columnWidths[i]);
			tc.setResizable(i > 0);
		}

		listViewer.setInput(items);
		updateChecked();

		remove = new Button(composite, SWT.PUSH);
		remove.setText("Delete");
		remove.addSelectionListener(onRemoveClicked());
		GridData d = new GridData();
		d.verticalAlignment = SWT.BEGINNING;
		remove.setLayoutData(d);
		UIUtils.setDefaultButtonWidth(remove);
		listViewer.addSelectionChangedListener(onListItemSelected());
		updateRemoveButton();
		
		Label detailsLabel = new Label(composite, SWT.NONE);
		GridData d1 = new GridData(GridData.FILL_HORIZONTAL);
		d1.horizontalIndent = 15;
		d1.horizontalSpan = 3;
		detailsLabel.setLayoutData(d1);
		detailsLabel.setText("Certificate details:");
		
		details = new StyledText(composite, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
		details.setEditable(false);
		GridData d2 = new GridData(GridData.FILL_HORIZONTAL);
		d2.horizontalSpan = 2;
		d2.heightHint = 250;
		details.setLayoutData(d2);

		updateDetails();
	}

	private ISelectionChangedListener onListItemSelected() {
		return new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRemoveButton();
				updateDetails();
			}
		};
	}

	private SelectionListener onRemoveClicked() {
		return new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};
	}

	private void updateRemoveButton() {
		if (!DisposeUtils.isDisposed(listViewer)) {
			remove.setEnabled(!listViewer.getSelection().isEmpty());
		}
	}

	private void updateDetails() {
		if (!DisposeUtils.isDisposed(listViewer)) {
			if(listViewer.getSelection().isEmpty()) {
				SSLCertificateUIHelper.INSTANCE.clean(details);
			} else {
				IStructuredSelection s = listViewer.getStructuredSelection();
				HostCertificate item = (HostCertificate)s.getFirstElement();
				SSLCertificateUIHelper.INSTANCE.writeCertificate(
						item.getIssuer(), item.getValidity(), item.getFingerprint(), details);
			}
		}
	}

	@Override
	protected void adjustGridLayout() {
		getFieldEditorParent().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridLayout) getFieldEditorParent().getLayout()).numColumns = 3;
	}

	void deleteSelection() {
		IStructuredSelection s = listViewer.getStructuredSelection();
		if (!s.isEmpty()) {
			for (Object item: s.toArray()) {
				items.remove(item);
			}
			listViewer.setInput(items);
		}
	}

	private class CP implements IStructuredContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return items.toArray();
		}

	}

	private class LP extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			HostCertificate i = (HostCertificate)element;
			if(columnIndex == 0) {
				return "";
			} else if(columnIndex == 2) {
				return i.getValidity().replace("\n", " ").replace("\r", " ");
			}
			return i.getIssuer().replace("\n", " ").replace("\r", " ");
		}

	}

	@Override
	public boolean performOk() {
		for (HostCertificate i: items) {
			i.setAccepted(listViewer.getChecked(i));
		}
		SSLCertificatesPreference.getInstance().saveWorkingCopy(items);
		return true;
	}

	@Override
	protected void performDefaults() {
		if(listViewer == null || listViewer.getTable() == null || listViewer.getTable().isDisposed()) {
			return;
		}
		items.clear();
		items.addAll(SSLCertificatesPreference.getInstance().getSavedItems());
		listViewer.setInput(items);
		updateChecked();
	}

	private void updateChecked() {
		if (!DisposeUtils.isDisposed(listViewer)) {
			for (HostCertificate i: items) {
				listViewer.setChecked(i, i.isAccepted());
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if(listViewer != null) {
			listViewer = null;
		}
		if(remove != null) {
			remove = null;
		}
	}
}
