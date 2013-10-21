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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.ProjectUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 * @author Martes G Wigglesworth
 */
public class ProjectAndServerAdapterSettingsWizardPage extends AbstractOpenShiftWizardPage {

	public static final String PREF_CONTENTASSISTKEY = "prefContentAssistKey";
	
	private static final String PAGE_TITLE_FORMAT = "Set up Project for new OpenShift Appplication named: \"{0}\""; 
	
	private ProjectAndServerAdapterSettingsWizardPageModel pageModel;
	private Text existingProjectNameText = null;

	public ProjectAndServerAdapterSettingsWizardPage(IWizard wizard, IOpenShiftWizardModel wizardModel) {
		super(NLS.bind(PAGE_TITLE_FORMAT, wizardModel.getApplicationName()),
				"Configure your project and server adapter settings, then click 'next' or 'finish'.",
				"Project Configuration", wizard);
		this.pageModel = new ProjectAndServerAdapterSettingsWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		createProjectGroup(container, dbc);
		createServerAdapterGroup(container, dbc);
//		createWorkingSetGroup(container, dbc);
	}
	

	private Composite createProjectGroup(Composite parent, DataBindingContext dbc) {
		Composite projectGroup = new Composite(parent, SWT.NONE);
		// projectGroup.setText("Project");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(projectGroup);

		// new project checkbox
		Button newProjectRadioBtn = new Button(projectGroup, SWT.CHECK);
		newProjectRadioBtn.setText("Create a new project");
		newProjectRadioBtn
				.setToolTipText("The OpenShift application code will be pulled into the newly created project or merged into the selected one.");
		newProjectRadioBtn.setFocus();
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(newProjectRadioBtn);
		final IObservableValue newProjectObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_IS_NEW_PROJECT).observe(pageModel);
		final ISWTObservableValue newProjectRadioBtnSelection = 
				WidgetProperties.selection().observe(newProjectRadioBtn);
		dbc.bindValue(newProjectRadioBtnSelection, newProjectObservable);

		// existing project
		Label existingProjectLabel = new Label(projectGroup, SWT.NONE);
		existingProjectLabel.setText("Use existing project:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(false, false).indent(10, 0).applyTo(existingProjectLabel);

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
		newProjectRadioBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				existingProjectNameText.setFocus();
				existingProjectNameText.selectAll();
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

		adapter.setProposals(ProjectUtils.getAllOpenedProjects());

		Button browseProjectsButton = new Button(projectGroup, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).span(1, 1).grab(false, false)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseProjectsButton))
				.notUpdating(newProjectObservable).converting(new InvertingBooleanConverter()).in(dbc);

		final IObservableValue applicationNameModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_APPLICATION_NAME).observe(pageModel);
		final UseExistingOpenProjectValidator existingProjectValidator = 
				new UseExistingOpenProjectValidator(applicationNameModelObservable, newProjectObservable, projectNameModelObservable);
		dbc.addValidationStatusProvider(existingProjectValidator);
		ControlDecorationSupport.create(existingProjectValidator, SWT.LEFT | SWT.TOP);

		return projectGroup;
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

	private void createServerAdapterGroup(Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults()
				.spacing(12, 8).margins(6, 6).applyTo(container);

		// create server adapter checkbox
		final Button serverAdapterCheckbox = new Button(container, SWT.CHECK);
		serverAdapterCheckbox.setText("Create and set up a server for easy publishing");
		serverAdapterCheckbox
				.setToolTipText("This Server Adapter will let you publish your local changes onto OpenShift, right from your Eclipse workbench.");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(serverAdapterCheckbox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(serverAdapterCheckbox))
				.to(BeanProperties
						.value(ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_CREATE_SERVER_ADAPTER)
						.observe(pageModel))
				.in(dbc);

		// disable maven build
		final Button skipMavenBuildCheckbox = new Button(container, SWT.CHECK);
		skipMavenBuildCheckbox.setText("Disable automatic maven build when pushing to OpenShift");
		skipMavenBuildCheckbox
				.setToolTipText("Configures the project to not get built when pushed to OpenShift.");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(skipMavenBuildCheckbox);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(skipMavenBuildCheckbox))
				.to(BeanProperties
						.value(ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_SKIP_MAVEN_BUILD)
						.observe(pageModel))
				.in(dbc);
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

		private final IObservableValue applicationNameObservable;
		private final IObservableValue newProjectObservable;
		private final IObservableValue projectNameObservable;

		public UseExistingOpenProjectValidator(IObservableValue applicationNameObservable, IObservableValue newProjectObservable, IObservableValue projectNameObservable) {
			this.applicationNameObservable = applicationNameObservable;
			this.newProjectObservable = newProjectObservable;
			this.projectNameObservable = projectNameObservable;
		}

		@Override
		public IStatus validate() {
			IStatus status = Status.OK_STATUS;
			final String projectName = (String) projectNameObservable.getValue();
			final Boolean isNewProject = (Boolean) newProjectObservable.getValue();
			if (isNewProject) {
				final String applicationName = (String) applicationNameObservable.getValue();
				if (StringUtils.isEmptyOrNull(applicationName)) {
					status = OpenShiftUIActivator.createErrorStatus("You have to choose an application name");
				} else if (!StringUtils.isAlphaNumeric(applicationName)) {
					status = OpenShiftUIActivator.createErrorStatus(
							"The name may only contain letters and digits.");
				} else {
					final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(applicationName);
					if (project.exists()) {
						status = OpenShiftUIActivator.createErrorStatus(
								NLS.bind("A project named {0} already exists in the workspace.", applicationName));
					}
				}
			} else {
				if (projectName == null || projectName.isEmpty()) {
					status = OpenShiftUIActivator.createErrorStatus("Select an open project in the workspace.");
				} else {
					final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (!project.exists()) {
						status = OpenShiftUIActivator.createErrorStatus(
								NLS.bind("The project {0} does not exist in your workspace.", projectName));
					} else if (!project.isOpen()) {
						status = OpenShiftUIActivator.createErrorStatus(
								NLS.bind("The project {0} is not open.", projectName));
					}
				}
			}
			return status;
		}
	}

	@Override
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.FORWARDS) {
			pageModel.reset();
		}
	}	
	
	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		setPageTitle();
		dbc.updateTargets();
	}

	private void setPageTitle() {
		setTitle(NLS.bind(PAGE_TITLE_FORMAT, pageModel.getApplicationName()));
	}
}
