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
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;

/**
 * @author Andr� Dietisheim
 * @author Xavier Coulon
 */
public class ProjectAndServerAdapterSettingsWizardPage extends AbstractOpenShiftWizardPage {

	public static final String PREF_CONTENTASSISTKEY = "prefContentAssistKey";

	private ProjectAndServerAdapterSettingsWizardPageModel pageModel;

	private Text existingProjectNameText = null;

	public ProjectAndServerAdapterSettingsWizardPage(IWizard wizard, IOpenShiftWizardModel wizardModel) {
		super("Project Configuration",
				"Configure your project and server adapter settings, then click 'next' or 'finish'.",
				"Project Configuration", wizard);
		this.pageModel = new ProjectAndServerAdapterSettingsWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		createProjectGroup(container, dbc);
		createServerAdapterGroup(container, dbc);
		createWorkingSetGroup(container, dbc);
	}

	private Composite createProjectGroup(Composite parent, DataBindingContext dbc) {
		Composite projectGroup = new Composite(parent, SWT.NONE);
		// projectGroup.setText("Project");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(projectGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(projectGroup);

		// new project checkbox
		Button newProjectRadioBtn = new Button(projectGroup, SWT.CHECK);
		newProjectRadioBtn.setText("Create a new project");
		newProjectRadioBtn
		.setToolTipText("The OpenShift application code will be pulled into the newly created project or merged into the selected one.");
		newProjectRadioBtn.setFocus();
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(newProjectRadioBtn);
		final IObservableValue newProjectObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_IS_NEW_PROJECT).observe(pageModel);
		final ISWTObservableValue newProjectRadioBtnSelection = WidgetProperties.selection()
				.observe(newProjectRadioBtn);
		dbc.bindValue(newProjectRadioBtnSelection, newProjectObservable);

		// existing project
		Label existingProjectLabel = new Label(projectGroup, SWT.NONE);
		existingProjectLabel.setText("Use the existing project");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(false, false)
		.indent(10, 0).applyTo(existingProjectLabel);

		existingProjectNameText = new Text(projectGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false)
				.applyTo(existingProjectNameText);
		final IObservableValue projectNameModelObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_PROJECT_NAME).observe(pageModel);
		final ISWTObservableValue existingProjectNameTextObservable = WidgetProperties.text(SWT.Modify).observe(
				existingProjectNameText);
		ValueBindingBuilder.bind(existingProjectNameTextObservable).to(projectNameModelObservable).in(dbc);
		// disable the project name text when the model state is set to 'new project'
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(existingProjectNameText))
				.notUpdating(newProjectObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project name text control when choosing the 'Use an existing project' option.
		newProjectRadioBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				existingProjectNameText.setFocus();
				existingProjectNameText.selectAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		// let's provide content assist on the existing project name
		ControlDecoration dec = new ControlDecoration(existingProjectNameText, SWT.TOP | SWT.LEFT);
		FieldDecoration contentProposalFieldIndicator = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing a project name.");
		dec.setShowOnlyOnFocus(true);

		AutoCompleteField adapter = new AutoCompleteField(existingProjectNameText, new TextContentAdapter(),
				new String[] {});

		adapter.setProposals(getOpenProjectsInWorkspace());

		Button browseProjectsButton = new Button(projectGroup, SWT.NONE);
		browseProjectsButton.setText("Browse");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).span(1, 1).grab(false, false)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseProjectsButton))
				.notUpdating(newProjectObservable).converting(new InvertingBooleanConverter()).in(dbc);

		final IObservableValue existingProjectValidityObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_EXISTING_PROJECT_VALIDITY).observe(pageModel);
		final UseExistingOpenProjectValidator existingProjectValidator = new UseExistingOpenProjectValidator(
				existingProjectValidityObservable);
		dbc.addValidationStatusProvider(existingProjectValidator);
		ControlDecorationSupport.create(existingProjectValidator, SWT.LEFT | SWT.TOP);

		return projectGroup;
	}

	private String[] getOpenProjectsInWorkspace() {
		List<String> projects = new ArrayList<String>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (project.exists() && project.isOpen()) {
				projects.add(project.getName());
			}
		}
		return projects.toArray(new String[projects.size()]);
	}

	/**
	 * Open a dialog box to select an open project when clicking on the 'Browse' button.
	 * 
	 * @return
	 */
	private SelectionListener onBrowseProjects() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectExistingProjectDialog dialog = new SelectExistingProjectDialog(pageModel.getApplicationName(),
						getShell());
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					if (selectedProject instanceof IProject) {
						pageModel.setProjectName(((IProject) selectedProject).getName());
					}
				}
			}

		};
	}

	private Group createServerAdapterGroup(Composite container, DataBindingContext dbc) {
		Group serverAdapterGroup = new Group(container, SWT.BORDER);
		serverAdapterGroup.setText("Server Adapter");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(serverAdapterGroup);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(serverAdapterGroup);

		Composite c = new Composite(serverAdapterGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(12, 8).applyTo(c);

		final Button serverAdapterCheckbox = new Button(c, SWT.CHECK);
		serverAdapterCheckbox.setText("Create a new Server Adapter");
		serverAdapterCheckbox
				.setToolTipText("This Server Adapter will let you publish your local changes onto OpenShift, right from your Eclipse workbench.");
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(serverAdapterCheckbox);
		final IObservableValue serverAdapterCheckboxObservable = WidgetProperties.selection().observe(
				serverAdapterCheckbox);
		final IObservableValue serverAdapterModelObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_CREATE_SERVER_ADAPTER).observe(pageModel);
		ValueBindingBuilder.bind(serverAdapterCheckboxObservable).to(serverAdapterModelObservable).in(dbc);
		return serverAdapterGroup;
	}

	private WorkingSetGroup createWorkingSetGroup(Composite container, DataBindingContext dbc) {
		return new WorkingSetGroup(container, null, new String[] { "org.eclipse.ui.resourceWorkingSetPage", //$NON-NLS-1$
				"org.eclipse.jdt.ui.JavaWorkingSetPage" /* JavaWorkingSetUpdater.ID */});
	}

	/**
	 * Verify that if the 'use an existing project' option was chose, then the project name actually matches an open
	 * project in the workspace.
	 */
	// @Override
	// protected void onPageWillGetDeactivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
	// if (direction == Direction.BACKWARDS) {
	// return;
	// }
	// if (!pageModel.isNewProject()) {
	// try {
	// final ArrayBlockingQueue<IStatus> queue = new ArrayBlockingQueue<IStatus>(1);
	// WizardUtils.runInWizard(new Job("Verifying existing project exists and is open...") {
	// @Override
	// protected IStatus run(IProgressMonitor monitor) {
	// IStatus status = pageModel.validateExistingProject();
	// queue.offer(status);
	// monitor.done();
	// return Status.OK_STATUS;
	// }
	// }, new DelegatingProgressMonitor(), getContainer(), getDatabindingContext());
	// final IStatus status = queue.poll(10, TimeUnit.SECONDS);
	// event.doit = status.isOK();
	// if (!event.doit) {
	// existingProjectNameText.setFocus();
	// existingProjectNameText.selectAll();
	// }
	// } catch (Exception ex) {
	// event.doit = false;
	// } finally {
	// }
	//
	// }
	// }

	class UseExistingOpenProjectValidator extends MultiValidator {

		private final IObservableValue existingProjectValidityObservable;

		public UseExistingOpenProjectValidator(IObservableValue existingProjectValidityObservable) {
			this.existingProjectValidityObservable = existingProjectValidityObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus existingProjectValidityStatus = (IStatus) existingProjectValidityObservable.getValue();

			if (existingProjectValidityStatus == null) {
				return ValidationStatus.ok();
			}
			return existingProjectValidityStatus;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage#onPageActivated(org.eclipse.
	 * core.databinding.DataBindingContext)
	 */
	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		pageModel.validateExistingProject();
	}

}
