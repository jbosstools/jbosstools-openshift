/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.deployment;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.util.ProjectUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.SelectExistingProjectDialog;

public class DeploymentSettingsWizardPage extends AbstractOpenShiftWizardPage {

	public static final String PREF_CONTENTASSISTKEY = "prefContentAssistKey";
	private static final String PAGE_DESCRIPTION_PATTERN = "Configure OpenShift resources in namespace: \"{0}\".";
	
	private DeploymentSettingsWizardPageModel pageModel;

	public DeploymentSettingsWizardPage(IWizard wizard, DeploymentWizardContext context) {
		super("Set up resources for a new OpenShift Deployment",
				NLS.bind(PAGE_DESCRIPTION_PATTERN, context.getNamespace()),
				"Deployment Configuration", wizard);
		this.pageModel = new DeploymentSettingsWizardPageModel(context);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
			.margins(10, 10)
			.applyTo(container);

		createProjectControls(container, dbc);
	}

	private Composite createProjectControls(Composite parent, DataBindingContext dbc) {
		Composite projectComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(projectComposite);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(projectComposite);

		// new project checkbox
		final Button buildConfigCheckbox = new Button(projectComposite, SWT.CHECK);
		buildConfigCheckbox.setEnabled(true);
		buildConfigCheckbox.setText("Include Build Configuration");
		buildConfigCheckbox
				.setToolTipText("A build configuration will generate a deployable image when code is pushed to the source repository.");
		buildConfigCheckbox.setFocus();
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(buildConfigCheckbox);
		final IObservableValue includeBuildConfigObservable = BeanProperties.value(
				DeploymentSettingsWizardPageModel.PROPERTY_INCLUDE_BUILD_CONFIG).observe(pageModel);
		ISWTObservableValue buildConfigCheckboxObservable = WidgetProperties.selection().observe(buildConfigCheckbox);
		ValueBindingBuilder
			.bind(buildConfigCheckboxObservable)
			.to(includeBuildConfigObservable)
			.in(dbc);

		// existing project
		Label sourceLabel = new Label(projectComposite, SWT.NONE);
		sourceLabel.setText("Use source from:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(false, false)
				.indent(10, 0)
				.applyTo(sourceLabel);

		final Text eclipseProjectNameText = new Text(projectComposite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(eclipseProjectNameText);
		ISWTObservableValue projectNameTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(eclipseProjectNameText);
		ValueBindingBuilder
			.bind(projectNameTextObservable)
			.to(BeanProperties.value(
					DeploymentSettingsWizardPageModel.PROPERTY_ECLIPSE_PROJECT_NAME).observe(pageModel))
			.in(dbc);
		// enable the project name text when the model state is set to 'include the build'
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(eclipseProjectNameText))
				.to(includeBuildConfigObservable)
				.in(dbc);

		// move focus to the project name text control when including the build config option.
		buildConfigCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				eclipseProjectNameText.setFocus();
				eclipseProjectNameText.selectAll();
			}
		});
		// project name content assist
		ControlDecoration dec = new ControlDecoration(eclipseProjectNameText, SWT.TOP | SWT.RIGHT);
		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing a project name.");
		dec.setShowOnlyOnFocus(true);

		new AutoCompleteField(eclipseProjectNameText, new TextContentAdapter(), ProjectUtils.getAllOpenedProjects());

		// browse projects
		final Button browseProjectsButton = new Button(projectComposite, SWT.NONE);
		browseProjectsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER)
				.hint(100, SWT.DEFAULT)
				.grab(false, false)
				.applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects());
		
		// enable the browse button when the model state is set to 'include the build'
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseProjectsButton))
				.to(includeBuildConfigObservable)
				.in(dbc);

		buildConfigCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseProjectsButton.setEnabled(buildConfigCheckbox.isEnabled());
			}
		});
		
		Label usesLabel = new Label(projectComposite, SWT.NONE);
		usesLabel.setText("Service Dependencies:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(false, false)
				.indent(10, 0)
				.applyTo(usesLabel);
		final Text usesServicesText = new Text(projectComposite, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(usesServicesText);
		ISWTObservableValue usesTextObservable = 
				WidgetProperties.text(SWT.Modify).observe(usesServicesText);
		ValueBindingBuilder
			.bind(usesTextObservable)
			.to(BeanProperties.value(
					DeploymentSettingsWizardPageModel.PROPERTY_ECLIPSE_SERVICES_DEPENDENCIES).observe(pageModel))
			.in(dbc);
		
		final EclipseProjectValidator existingProjectValidator = 
				new EclipseProjectValidator(buildConfigCheckboxObservable, projectNameTextObservable);
		dbc.addValidationStatusProvider(existingProjectValidator);
		ControlDecorationSupport.create(existingProjectValidator, SWT.LEFT | SWT.TOP);

		return projectComposite;
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
				SelectExistingProjectDialog dialog = 
						new SelectExistingProjectDialog(pageModel.getEclipseProjectName(), getShell());
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					if (selectedProject instanceof IProject) {
						pageModel.setEclipseProjectName(((IProject) selectedProject).getName());
					}
				}
			}
		};
	}
	
	class EclipseProjectValidator extends MultiValidator {

		private final IObservableValue includeBuildConfigObservable;
		private final IObservableValue projectNameObservable;
		
		public EclipseProjectValidator(IObservableValue includeBuildConfigObservable, IObservableValue projectNameObservable) {
			this.includeBuildConfigObservable = includeBuildConfigObservable;
			this.projectNameObservable = projectNameObservable;
		}

		@Override
		public IStatus validate() {
			// access all value observable to register them
			final String projectName = (String) projectNameObservable.getValue();
			final Boolean includeBuildConfig = (Boolean) includeBuildConfigObservable.getValue();

			IStatus status = ValidationStatus.ok();
			if (includeBuildConfig) {
				if (StringUtils.isEmpty(projectName)) {
					status = ValidationStatus.cancel("Select an open Eclipse project in the workspace.");
				} else {
					final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (!ProjectUtils.exists(project)) {
						status = ValidationStatus.error(
								NLS.bind("The Eclipse project {0} does not exist in your workspace.", projectName));
					} else if (!ProjectUtils.isAccessible(project)) {
						status = ValidationStatus.error(
								NLS.bind("The Eclipse project {0} is not open.", projectName));
					} else if (EGitUtils.isSharedWithGit(project)){
						if (!EGitUtils.isGitFolderInRootOf(project)) {
							status = ValidationStatus.error(NLS.bind(
									"The Eclipse project {0} is not at the root of your git repository and appears to be a sub-project. Please copy your project to it's own repository.",
									project.getName()));
						} else {
							status = getGitDirtyStatus(project);						
						}
					}
				}
			}
			return status;
		}

		private IStatus getGitDirtyStatus(IProject project) {
			IStatus repoCorruptError = ValidationStatus.error(NLS.bind(
					"The git repository for project {0} looks corrupt. Please fix it before using it.",
					project.getName()));
			try {
				if (EGitUtils.isDirty(project, false, new NullProgressMonitor())) {
					return ValidationStatus.error(NLS.bind(
							"The project {0} has uncommitted changes. Please commit those changes first.",
							project.getName()));
				} else {
					return ValidationStatus.ok();
				}
			} catch (NoWorkTreeException e) {
				return repoCorruptError;
			} catch (IOException e) {
				return repoCorruptError;
			} catch (GitAPIException e) {
				return repoCorruptError;
			}
		}

		@Override
		public IObservableList getTargets() {
			// only decorate project name
			return Observables.staticObservableList(Collections.singletonList(projectNameObservable));
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
		setPageDescription();
		dbc.updateTargets();
	}

	private void setPageDescription() {
		setDescription(NLS.bind(PAGE_DESCRIPTION_PATTERN, pageModel.getEclipseProjectName()));
	}
}
