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
package org.jboss.tools.openshift.express.internal.ui.console;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Xavier Coulon
 * 
 */
public class TailFilesWizardPage extends AbstractOpenShiftWizardPage {

	private final TailFilesWizardPageModel pageModel;

	public TailFilesWizardPage(final TailFilesWizardPageModel pageModel, final IWizard wizard) {
		super("Tail Log Files", "This will run tail on your OpenShift application '" + pageModel.getApplication().getName() +
				"'.\nYou can use the defaults or change the tail options.",
				"TailFilePage", wizard);
		this.pageModel = pageModel;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(parent);
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

		// label
		final Label filePatternLabel = new Label(container, SWT.NONE);
		filePatternLabel.setText("Tail options:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false)
				.applyTo(filePatternLabel);
		// input text field
		final Text filePatternText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(filePatternText);
		IObservableValue existingAppNameTextObservable = WidgetProperties.text(SWT.Modify).observe(filePatternText);
		IObservableValue existingAppNameModelObservable = BeanProperties.value(
				TailFilesWizardPageModel.PROPERTY_FILE_PATTERN).observe(pageModel);
		ValueBindingBuilder.bind(existingAppNameTextObservable).to(existingAppNameModelObservable).in(dbc);
		// reset button (in case user inputs something and wants/needs to revert)
		final Button resetButton = new Button(container, SWT.PUSH);
		resetButton.setText("Reset");
		GridDataFactory.fillDefaults()
				.span(1, 1).align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(resetButton);
		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.resetFilePattern();
			}
		});
	}

	// private SelectionListener onCheckAll() {
	// return new SelectionAdapter() {
	//
	// @Override
	// public void widgetSelected(SelectionEvent e) {
	// viewer.setAllChecked(true);
	// try {
	// addJenkinsCartridge(IEmbeddedCartridge.JENKINS_14);
	// } catch (OpenShiftException ex) {
	// OpenShiftUIActivator.log("Could not select jenkins cartridge", ex);
	// } catch (SocketTimeoutException ex) {
	// OpenShiftUIActivator.log("Could not select jenkins cartridge", ex);
	// }
	// }
	//
	// };
	// }

	// private SelectionListener onUncheckAll() {
	// return new SelectionAdapter() {
	//
	// @Override
	// public void widgetSelected(SelectionEvent e) {
	// viewer.setAllChecked(false);
	// }
	//
	// };
	// }

	private void resetFilePatternText() {
		pageModel.resetFilePattern();
	}
	
	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			resetFilePatternText();
		} catch (Exception e) {
			Logger.error("Could not reset File Pattern text field", e);
		}
	}

}