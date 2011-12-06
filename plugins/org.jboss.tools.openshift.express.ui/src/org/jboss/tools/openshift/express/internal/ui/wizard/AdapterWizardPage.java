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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 * @author Rob Stryker
 * 
 */
public class AdapterWizardPage extends AbstractOpenShiftWizardPage implements IWizardPage, PropertyChangeListener {

	private AdapterWizardPageModel model;
	private Text cloneUriValueText;
	private Label domainValueLabel;
	private Label modeValueLabel;
	private Button serverAdapterCheckbox;
	private IServerType serverTypeToCreate;

	private IObservableValue serverAdapterCheckboxObservable;
	private IObservableValue newProjectCheckboxIsEnabled;

	public AdapterWizardPage(ImportProjectWizard wizard, ImportProjectWizardModel model) {
		super(
				"Import OpenShift application",
				"Select the project to enable, the Git clone destination, the branch to clone"
						+ " and configure your server adapter ",
				"Server Adapter",
				wizard);
		this.model = new AdapterWizardPageModel(model);
		model.addPropertyChangeListener(this);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		Group mergeGroup = createProjectGroup(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(mergeGroup);

		Group cloneGroup = createCloneGroup(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(cloneGroup);

		Group serverAdapterGroup = createAdapterGroup(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(serverAdapterGroup);

		Label fillerLabel = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(fillerLabel);

	}

	private Group createProjectGroup(Composite parent, DataBindingContext dbc) {
		Group projectGroup = new Group(parent, SWT.BORDER);
		projectGroup.setText("Project");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectGroup);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(3).applyTo(projectGroup);

		Button newProjectCheckbox = new Button(projectGroup, SWT.CHECK);
		newProjectCheckbox.setText("Create new Project");
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(newProjectCheckbox);
		IObservableValue newProjectObservable =
				BeanProperties.value(AdapterWizardPageModel.PROPERTY_NEW_PROJECT).observe(model);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(newProjectCheckbox))
				.to(newProjectObservable)
				.in(dbc);

		this.newProjectCheckboxIsEnabled = WidgetProperties.enabled().observe(newProjectCheckbox);

		Label existingProjectLabel = new Label(projectGroup, SWT.NONE);
		existingProjectLabel.setText("Existing Project");
		GridDataFactory
				.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(existingProjectLabel);
		Text newProjectText = new Text(projectGroup, SWT.BORDER);
		newProjectText.setEditable(false);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, true).applyTo(newProjectText);

		IObservableValue newProjectNameObservable =
				BeanProperties.value(AdapterWizardPageModel.PROPERTY_PROJECT_NAME).observe(model);
		ValueBindingBuilder
				.bind(WidgetProperties.text().observe(newProjectText))
				.to(newProjectNameObservable)
				.in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(newProjectText))
				.notUpdating(newProjectObservable)
				.converting(new InvertingBooleanConverter())
				.in(dbc);
		dbc.addValidationStatusProvider(
				new EnableProjectValidator(newProjectObservable, newProjectNameObservable));

		Button browseProjectsButton = new Button(projectGroup, SWT.NONE);
		browseProjectsButton.setText("Browse");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseProjectsButton);
		browseProjectsButton.addSelectionListener(onBrowseProjects());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(browseProjectsButton))
				.notUpdating(newProjectObservable)
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		return projectGroup;
	}

	private Group createCloneGroup(Composite parent, DataBindingContext dbc) {
		Group cloneGroup = new Group(parent, SWT.BORDER);
		cloneGroup.setText("Git clone");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cloneGroup);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(4).applyTo(cloneGroup);

		Label gitUriLabel = new Label(cloneGroup, SWT.NONE);
		gitUriLabel.setText("Cloning From");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(gitUriLabel);

		cloneUriValueText = new Text(cloneGroup, SWT.BORDER);
		cloneUriValueText.setEditable(false);
		GridDataFactory
				.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cloneUriValueText);
		ValueBindingBuilder
				.bind(WidgetProperties.text(SWT.Modify).observe(cloneUriValueText))
				.notUpdating(BeanProperties.value(AdapterWizardPageModel.PROPERTY_CLONE_URI).observe(model))
				.in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(cloneUriValueText))
				.notUpdating(BeanProperties.value(AdapterWizardPageModel.PROPERTY_LOADING).observe(model))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		// bind loading state to page complete
		ValueBindingBuilder
				.bind(new WritableValue(false, Boolean.class))
				.notUpdating(BeanProperties.value(AdapterWizardPageModel.PROPERTY_LOADING).observe(model))
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (Boolean.FALSE.equals(value)) {
							return ValidationStatus.ok();
						} else {
							return ValidationStatus.cancel("Loading...");
						}
					}
				})
				.in(dbc);

		Label repoPathLabel = new Label(cloneGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(repoPathLabel);
		repoPathLabel.setText("Destination");

		Button defaultRepoPathButton = new Button(cloneGroup, SWT.CHECK);
		defaultRepoPathButton.setText("default");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(defaultRepoPathButton);
		defaultRepoPathButton.addSelectionListener(onDefaultRepoPath());
		IObservableValue defaultRepoButtonSelection = WidgetProperties.selection().observe(defaultRepoPathButton);

		Text repoPathText = new Text(cloneGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(repoPathText);
		DataBindingUtils.bindMandatoryTextField(
				repoPathText, "Location", AdapterWizardPageModel.PROPERTY_REPO_PATH, model, dbc);
		ValueBindingBuilder
				.bind(defaultRepoButtonSelection)
				.converting(new InvertingBooleanConverter())
				.to(WidgetProperties.enabled().observe(repoPathText))
				.notUpdatingParticipant()
				.in(dbc);

		Button browseRepoPathButton = new Button(cloneGroup, SWT.PUSH);
		browseRepoPathButton.setText("Browse");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseRepoPathButton);
		browseRepoPathButton.addSelectionListener(onRepoPath());
		ValueBindingBuilder
				.bind(defaultRepoButtonSelection)
				.converting(new InvertingBooleanConverter())
				.to(WidgetProperties.enabled().observe(browseRepoPathButton))
				.notUpdatingParticipant()
				.in(dbc);

		defaultRepoButtonSelection.setValue(true);

		Label remoteNameLabel = new Label(cloneGroup, SWT.NONE);
		remoteNameLabel.setText("Remote name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteNameLabel);

		Button defaultRemoteNameButton = new Button(cloneGroup, SWT.CHECK);
		defaultRemoteNameButton.setText("default");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(defaultRemoteNameButton);
		defaultRemoteNameButton.addSelectionListener(onDefaultRemoteName());

		Text remoteNameText = new Text(cloneGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(remoteNameText);
		DataBindingUtils.bindMandatoryTextField(
				remoteNameText, "Remote name", AdapterWizardPageModel.PROPERTY_REMOTE_NAME, model, dbc);

		IObservableValue defaultRemoteNameSelection = WidgetProperties.selection().observe(defaultRemoteNameButton);
		ValueBindingBuilder
				.bind(defaultRemoteNameSelection)
				.converting(new InvertingBooleanConverter())
				.to(WidgetProperties.enabled().observe(remoteNameText))
				.notUpdatingParticipant()
				.in(dbc);
		defaultRemoteNameSelection.setValue(true);

		Link sshPrefsLink = new Link(cloneGroup, SWT.NONE);
		sshPrefsLink
				.setText("Make sure your SSH key used with the domain is listed in <a>SSH2 Preferences</a>");
		GridDataFactory.fillDefaults()
				.span(4, 1).align(SWT.FILL, SWT.CENTER).indent(0, 10).applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());

		return cloneGroup;
	}

	private SelectionListener onBrowseProjects() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectExistingProjectDialog dialog =
						new SelectExistingProjectDialog(model.getApplicationName(), getShell());
				if (dialog.open() == Dialog.OK) {
					Object selectedProject = dialog.getFirstResult();
					if (selectedProject instanceof IProject) {
						model.setProjectName(((IProject) selectedProject).getName());
					}
				}
			}

		};
	}

	private SelectionListener onDefaultRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				model.resetRepositoryPath();
			}
		};
	}

	private SelectionListener onRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Choose the location to store your repository clone to...");
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					model.setRepositoryPath(repositoryPath);
				}
			}
		};
	}

	private SelectionListener onDefaultRemoteName() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				model.resetRemoteName();
			}
		};
	}

	private SelectionAdapter onSshPrefs() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SshPrivateKeysPreferences.openPreferencesPage(getShell());
			}
		};
	}

	private Group createAdapterGroup(Composite parent, DataBindingContext dbc) {
		Group serverAdapterGroup = new Group(parent, SWT.BORDER);
		serverAdapterGroup.setText("OpenShift JBoss Server adapter");
		GridLayoutFactory.fillDefaults().margins(6, 6).applyTo(serverAdapterGroup);

		Composite c = new Composite(serverAdapterGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(3).spacing(12, 8).applyTo(c);

		serverAdapterCheckbox = new Button(c, SWT.CHECK);
		serverAdapterCheckbox.setText("Create a JBoss server adapter");
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(serverAdapterCheckbox);
		serverAdapterCheckbox.addSelectionListener(onCreateAdapter());

		Label domainLabel = new Label(c, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(domainLabel);
		domainLabel.setText("Host");
		domainValueLabel = new Label(c, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(domainValueLabel);
		ValueBindingBuilder
				.bind(WidgetProperties.text().observe(domainValueLabel))
				.notUpdating(BeanProperties.value(AdapterWizardPageModel.PROPERTY_APPLICATION_URL).observe(model))
				.in(dbc);

		Label modeLabel = new Label(c, SWT.NONE);
		modeLabel.setText("Mode");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(modeLabel);
		modeValueLabel = new Label(c, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(modeValueLabel);

		model.getWizardModel().setProperty(
				AdapterWizardPageModel.CREATE_SERVER, serverAdapterCheckbox.getSelection());
		this.serverAdapterCheckboxObservable =
				WidgetProperties.selection().observe(serverAdapterCheckbox);

		return serverAdapterGroup;
	}

	private SelectionListener onCreateAdapter() {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				model.getWizardModel().setProperty(
						AdapterWizardPageModel.CREATE_SERVER, serverAdapterCheckbox.getSelection());
				enableServerWidgets(serverAdapterCheckbox.getSelection());
			}
		};
	}

	protected void enableServerWidgets(boolean enabled) {
		domainValueLabel.setEnabled(enabled);
		modeValueLabel.setEnabled(enabled);
	}

	private IServerType getServerTypeToCreate() {
		return ServerCore.findServerType("org.jboss.tools.openshift.express.openshift.server.type");
	}

	protected void onPageActivated(DataBindingContext dbc) {
		// allow to enable a proj only for as7 openshift applications
		setTitle(NLS.bind("Import OpenShift application {0}", model.getApplicationName()));

		newProjectCheckboxIsEnabled.setValue(model.isJBossAS7Application());

		model.resetRepositoryPath();
		serverTypeToCreate = getServerTypeToCreate();
		model.getWizardModel().setProperty(AdapterWizardPageModel.SERVER_TYPE, serverTypeToCreate);
		modeValueLabel.setText("Source");
		model.getWizardModel().setProperty(AdapterWizardPageModel.MODE, AdapterWizardPageModel.MODE_SOURCE);
		onPageActivatedBackground(dbc);
	}

	protected void onPageActivatedBackground(final DataBindingContext dbc) {
		new Job("Loading remote OpenShift application") {
			public IStatus run(IProgressMonitor monitor) {
				try {
					model.loadGitUri();
					model.loadApplicationUrl();
				} catch (OpenShiftException e) {
					IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
					OpenShiftUIActivator.log(status);
					return status;
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (ImportProjectWizardModel.APPLICATION.equals(evt.getPropertyName())) {
			handleApplicationChanged();
		}
	}

	private void handleApplicationChanged() {
		// we need to enable or disable all the server widgets depending on
		// if we can make a server out of this
		serverTypeToCreate = getServerTypeToCreate();
		boolean canCreateServer = serverTypeToCreate != null;
		serverAdapterCheckbox.setEnabled(canCreateServer);
		serverAdapterCheckboxObservable.setValue(canCreateServer);
		enableServerWidgets(canCreateServer);
		model.getWizardModel().setProperty(AdapterWizardPageModel.SERVER_TYPE, serverTypeToCreate);
		model.getWizardModel().setProperty(AdapterWizardPageModel.CREATE_SERVER, canCreateServer);
	}

	/**
	 * A multi validator that validates the state of the project that shall be
	 * (OpenShift) enabled
	 */
	private class EnableProjectValidator extends MultiValidator {

		private IObservableValue enableProjectObservable;
		private IObservableValue enabledProjectNameObservable;

		public EnableProjectValidator(IObservableValue enableProjectObservable,
				IObservableValue enabledProjectNameObservable) {
			this.enableProjectObservable = enableProjectObservable;
			this.enabledProjectNameObservable = enabledProjectNameObservable;
		}

		@Override
		protected IStatus validate() {
			/**
			 * WARNING: it is important to evaluate the validation state on
			 * behalf of observable values (not widgets!). The multi validator
			 * is tracking what observables are read to know when he has to
			 * recalculate it's state.
			 */
			if (Boolean.FALSE.equals(enableProjectObservable.getValue())) {
				return ValidationStatus.ok();
			}

			if (enabledProjectNameObservable != null
					&& enabledProjectNameObservable.getValue() != null
					&& (!((String) enabledProjectNameObservable.getValue()).isEmpty())) {
				return ValidationStatus.ok();
			} else {
				return ValidationStatus.error(
						"You have to select a project that shall be enabled for OpenShift");
			}
		}
	}

	// private static class GitUriLabelProvider implements ILabelProvider {
	//
	// @Override
	// public void addListener(ILabelProviderListener listener) {
	// }
	//
	// @Override
	// public void dispose() {
	// }
	//
	// @Override
	// public boolean isLabelProperty(Object element, String property) {
	// return true;
	// }
	//
	// @Override
	// public void removeListener(ILabelProviderListener listener) {
	// }
	//
	// @Override
	// public Image getImage(Object element) {
	// return null;
	// }
	//
	// @Override
	// public String getText(Object element) {
	// if (!(element instanceof GitUri)) {
	// return null;
	// }
	// return ((GitUri) element).getLabel();
	// }
	// }

	// private class MergeUriValidator implements IValidator {
	//
	// @Override
	// public IStatus validate(Object value) {
	// String mergeUri = (String) value;
	// if (mergeUri == null
	// || mergeUri.length() == 0) {
	// return ValidationStatus
	// .warning("You have to provide a git uri to merge with");
	// }
	// GitUri gitUri = model.getKnownMergeUri(mergeUri);
	// if (gitUri == null) {
	// return ValidationStatus
	// .warning("You are not merging with an official example. Things may go wrong");
	// }
	// if (!model.isCompatibleToApplicationCartridge(gitUri.getCartridge())) {
	// return ValidationStatus
	// .warning("The example you've chosen is not compatible to your application");
	// }
	// return ValidationStatus.ok();
	// }
	// }
}
