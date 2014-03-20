/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.NavigatorContentServiceFactory;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.details.ApplicationDetailsDialog;

import com.openshift.client.IApplication;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class SelectApplicationWizardPage extends AbstractOpenShiftWizardPage {

	private static final String COMMON_VIEWER_ID = "org.jboss.tools.openshift.express.internal.ui.wizard.application.SelectApplicationWizardPage";
	private final SelectApplicationWizardPageModel pageModel;
	private TreeViewer applicationsTreeViewer;

	public SelectApplicationWizardPage(OpenShiftApplicationWizardModel wizardModel, IWizard wizard) {
		super("Select Existing Application", "Please choose the existing application that you want to import.", "Select Existing Application", wizard);
		this.pageModel = new SelectApplicationWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(parent);
		GridLayoutFactory.fillDefaults()
			.numColumns(2).applyTo(parent);

		Label existingApplicationsLabel = new Label(parent, SWT.NONE);
		existingApplicationsLabel.setText("Existing Applications:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(existingApplicationsLabel);

		// applications tree
		this.applicationsTreeViewer = createApplicationsTree(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 200).span(1,4)
				.applyTo(applicationsTreeViewer.getControl());

		Binding selectedApplicationBinding = ValueBindingBuilder
				.bind(ViewerProperties.singlePostSelection().observe(applicationsTreeViewer))
				.validatingAfterGet(new IValidator() {
					
					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof IApplication)) {
							return ValidationStatus.cancel("Please choose the existing application that you want to import.");
						}
						return ValidationStatus.ok();
					}
				})
				.to(BeanProperties.value(SelectApplicationWizardPageModel.PROPERTY_SELECTED_APPLICATION).observe(pageModel))
				.in(dbc);
			
		ControlDecorationSupport.create(
				selectedApplicationBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		
		// buttons
		Button detailsButton = new Button(parent, SWT.PUSH);
		detailsButton.setText("De&tails...");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).hint(80, SWT.DEFAULT)
				.applyTo(detailsButton);
		DataBindingUtils.bindEnablementToValidationStatus(detailsButton, IStatus.OK, dbc, selectedApplicationBinding);
		detailsButton.addSelectionListener(onDetails(dbc));
		
		Control filler = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.applyTo(filler);

		Button refreshButton = new Button(parent, SWT.PUSH);
		refreshButton.setText("R&efresh");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(refreshButton);
		refreshButton.addSelectionListener(onRefresh(dbc));

		filler = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.grab(false, true).applyTo(filler);
		
	}

	protected TreeViewer createApplicationsTree(Composite parent, DataBindingContext dbc) {
		CommonViewer commonViewer =
				new CommonViewer(COMMON_VIEWER_ID, parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		INavigatorContentService contentService =
				NavigatorContentServiceFactory.INSTANCE.createContentService(COMMON_VIEWER_ID, commonViewer);
		contentService.createCommonContentProvider();
		contentService.createCommonLabelProvider();
		
		return commonViewer;
	}
	
	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		pageModel.loadOpenShiftResources();
		setViewerInput(pageModel);
	}

	private SelectionAdapter onRefresh(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					WizardUtils.runInWizard(new Job("Loading applications...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							pageModel.refresh(); 
							setViewerInput(pageModel);
							return Status.OK_STATUS;
						}

					}, getContainer(), dbc);
				} catch (Exception e) {
					Logger.error("Failed to refresh applications list", e);
					// ignore
				}
			}
		};
	}

	private SelectionAdapter onDetails(DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				new ApplicationDetailsDialog(pageModel.getSelectedApplication(), getShell()).open();
			}
		};
	}

	private void setViewerInput(final SelectApplicationWizardPageModel pageModel) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				applicationsTreeViewer.setInput(pageModel.getConnection());
			}
		});
	}

	public IApplication getSelectedApplication() {
		return pageModel.getSelectedApplication();
	}
}
