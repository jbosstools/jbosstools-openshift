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
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.job.LoadKeysJob;
import org.jboss.tools.openshift.express.internal.ui.utils.LinkSelectionAdapter;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.ManageSSHKeysWizard;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 * 
 */
public class GitCloningSettingsWizardPage extends AbstractOpenShiftWizardPage implements IWizardPage {

	private GitCloningSettingsWizardPageModel pageModel;
	private IOpenShiftApplicationWizardModel wizardModel;
	private Button useDefaultRemoteNameButton;
	private Button useDefaultRepoPathButton;
	private Text remoteNameText;
	private Label remoteNameLabel;
	private RepoPathValidationStatusProvider repoPathValidator;
	private RemoteNameValidationStatusProvider remoteNameValidator;
	private Link sshLink;

	public GitCloningSettingsWizardPage(OpenShiftApplicationWizard wizard,
			IOpenShiftApplicationWizardModel wizardModel) {
		super(
				"Import an existing OpenShift application",
				"Configure the cloning settings by specifying the clone destination if you create a new project, and the git remote name if you're using an existing project.",
				"Cloning settings", wizard);
		this.pageModel = new GitCloningSettingsWizardPageModel(wizardModel);
		this.wizardModel = wizardModel;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		Composite cloneGroup = createCloneGroup(parent, dbc);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(cloneGroup);
		Composite filler = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(filler);
	}

	private Composite createCloneGroup(Composite parent, DataBindingContext dbc) {
		Group cloneGroup = new Group(parent, SWT.NONE);
		cloneGroup.setText("Cloning settings");
		cloneGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayoutFactory.fillDefaults()
				.numColumns(3).equalWidth(false).margins(10, 10).applyTo(cloneGroup);

		// Repo Path Management
		useDefaultRepoPathButton = new Button(cloneGroup, SWT.CHECK);
		useDefaultRepoPathButton.setText("Use default clone destination");
		useDefaultRepoPathButton.setToolTipText("Uncheck if you want to use a custom location to clone to");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(useDefaultRepoPathButton);
		Label labelForRepoPath = new Label(cloneGroup, SWT.NONE);
		labelForRepoPath.setText("Git Clone Destination:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).indent(10, 0)
				.applyTo(labelForRepoPath);
		final Text repoPathText = new Text(cloneGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(repoPathText);
		final IObservableValue repoPathObservable = WidgetProperties.text(SWT.Modify).observe(repoPathText);
		final IObservableValue repoPathModelObservable =
				BeanProperties.value(GitCloningSettingsWizardPageModel.PROPERTY_REPO_PATH).observe(pageModel);
		ValueBindingBuilder.bind(repoPathObservable).to(repoPathModelObservable).in(dbc);

		Button browseRepoPathButton = new Button(cloneGroup, SWT.PUSH);
		browseRepoPathButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseRepoPathButton);
		browseRepoPathButton.addSelectionListener(onRepoPath());

		final IObservableValue isDefaultRepoObservable =
				WidgetProperties.selection().observe(useDefaultRepoPathButton);
		final IObservableValue useDefaultRepoModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_USE_DEFAULT_REPO_PATH).observe(pageModel);
		ValueBindingBuilder.bind(isDefaultRepoObservable).to(useDefaultRepoModelObservable).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(repoPathText))
				.notUpdating(useDefaultRepoModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseRepoPathButton))
				.notUpdating(useDefaultRepoModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project location text control when not choosing the
		// 'Use default location' option.
		UIUtils.focusOnSelection(useDefaultRepoPathButton, repoPathText);

		this.repoPathValidator =
				new RepoPathValidationStatusProvider(
						repoPathObservable
						, BeanProperties.value(
								GitCloningSettingsWizardPageModel.PROPERTY_APPLICATION_NAME).observe(pageModel)
						, BeanProperties.value(
								GitCloningSettingsWizardPageModel.PROPERTY_NEW_PROJECT).observe(pageModel));
		dbc.addValidationStatusProvider(repoPathValidator);
		ControlDecorationSupport.create(repoPathValidator, SWT.LEFT | SWT.TOP);

		// Remote Name Management
		useDefaultRemoteNameButton = new Button(cloneGroup, SWT.CHECK);
		useDefaultRemoteNameButton.setText("Use default remote name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(useDefaultRemoteNameButton);

		this.remoteNameLabel = new Label(cloneGroup, SWT.NONE);
		remoteNameLabel.setText("Remote name:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).indent(10, 0)
				.applyTo(remoteNameLabel);
		remoteNameText = new Text(cloneGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().span(1, 1).align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(remoteNameText);

		final IObservableValue remoteNameTextObservable = WidgetProperties.text(SWT.Modify).observe(remoteNameText);
		final IObservableValue remoteNameModelObservable =
				BeanProperties.value(GitCloningSettingsWizardPageModel.PROPERTY_REMOTE_NAME).observe(pageModel);
		ValueBindingBuilder.bind(remoteNameTextObservable).to(remoteNameModelObservable).in(dbc);

		final IObservableValue useDefaultRemoteNameModelObservable =
				BeanProperties.value(GitCloningSettingsWizardPageModel.PROPERTY_USE_DEFAULT_REMOTE_NAME).observe(
						pageModel);
		final IObservableValue useDefaultRemoteNameObservable =
				WidgetProperties.selection().observe(useDefaultRemoteNameButton);
		ValueBindingBuilder
				.bind(useDefaultRemoteNameObservable)
				.to(useDefaultRemoteNameModelObservable)
				.in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(remoteNameText))
				.notUpdating(useDefaultRemoteNameModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(remoteNameLabel))
				.notUpdating(useDefaultRemoteNameModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project name text control when choosing the 'Use an
		// existing project' option.
		useDefaultRemoteNameButton.addSelectionListener(onDefaultRemoteNameUnchecked());
		final IObservableValue projectNameModelObservable =
				BeanProperties.value(IOpenShiftApplicationWizardModel.PROP_PROJECT_NAME).observe(wizardModel);

		dbc.addValidationStatusProvider(
				this.remoteNameValidator =
						new RemoteNameValidationStatusProvider(remoteNameTextObservable, projectNameModelObservable));
		ControlDecorationSupport.create(remoteNameValidator, SWT.LEFT | SWT.TOP);

		this.sshLink = new Link(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).indent(10, 0).applyTo(sshLink);
		sshLink.addSelectionListener(onSshPrefs("SSH2 Preferences"));
		sshLink.addSelectionListener(onManageSSHKeys("SSH Keys wizard", dbc));
		
		// we need a binding to have validation setting wizard validation status
		Label dummyLabel = new Label(parent, SWT.None);
		dummyLabel.setVisible(false);
		GridDataFactory.fillDefaults().exclude(true).applyTo(dummyLabel);
		ValueBindingBuilder
				.bind(WidgetProperties.text().observe(dummyLabel))
				.notUpdating(BeanProperties.value(
						GitCloningSettingsWizardPageModel.PROPERTY_HAS_REMOTEKEYS).observe(pageModel))
				.validatingAfterGet(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof Boolean)) {
							return ValidationStatus.ok();
						}
						Boolean hasRemoteKeys = (Boolean) value;
						if (hasRemoteKeys) {
							return ValidationStatus.ok();
						} else {
							return ValidationStatus
									.error("No public keys found in your account. Please use the SSH keys wizard.");
						}
					}
				})
				.in(dbc);
		refreshHasRemoteKeys(dbc);
		return cloneGroup;
	}

	private SelectionAdapter onDefaultRemoteNameUnchecked() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remoteNameText.setFocus();
			}
		};
	}

	private SelectionListener onRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Git clone location");
				dialog.setMessage("Choose the location for git clone...");
				dialog.setFilterPath(pageModel.getRepositoryPath());
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					pageModel.setRepositoryPath(repositoryPath);
				}
			}
		};
	}

	private SelectionAdapter onSshPrefs(String text) {
		return new LinkSelectionAdapter(text) {

			@Override
			public void doWidgetSelected(SelectionEvent e) {
				SshPrivateKeysPreferences.openPreferencesPage(getShell());
			}
		};
	}

	private SelectionAdapter onManageSSHKeys(String text, final DataBindingContext dbc) {
		return new LinkSelectionAdapter(text) {

			@Override
			public void doWidgetSelected(SelectionEvent e) {
				WizardDialog manageSSHKeysWizard =
						new OkButtonWizardDialog(getShell(), new ManageSSHKeysWizard(pageModel.getConnection()));
				if (manageSSHKeysWizard.open() == Dialog.OK) {
					refreshHasRemoteKeys(dbc);
				}
			}
		};
	}

	protected void onPageActivated(DataBindingContext dbc) {
		enableWidgets(pageModel.isNewProject());
		repoPathValidator.forceRevalidate();
		remoteNameValidator.forceRevalidate();
		setSSHLinkText();
		if (pageModel.isConnected()) {
			refreshHasRemoteKeys(dbc);
		}
	}

	private void setSSHLinkText() {
		if (pageModel.isConnected()) {
			sshLink.setText("Make sure that you have SSH keys added to your OpenShift account "
					+ pageModel.getConnection().getUsername() 
					+ " via <a>SSH Keys wizard</a> and that the private keys are listed in <a>SSH2 Preferences</a>");
		} else {
			sshLink.setText("Make sure that you have SSH keys added to your OpenShift account"
					+ " via <a>SSH Keys wizard</a> and that the private keys are listed in <a>SSH2 Preferences</a>");
		}
		sshLink.getParent().layout(true, true);
	}

	private void refreshHasRemoteKeys(DataBindingContext dbc) {
		try {
			if (!pageModel.isConnected()) {
				return;
			}
			final LoadKeysJob loadKeysJob = new LoadKeysJob(pageModel.getConnection());
			new JobChainBuilder(loadKeysJob).runWhenDone(new UIJob("") {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					IStatus status = loadKeysJob.getResult(); 
					if(status.equals(Status.OK_STATUS)){
						pageModel.setHasRemoteKeys(loadKeysJob.getKeys().size() > 0);
						setErrorMessage(null);
						return Status.OK_STATUS;
					}else{
						setErrorMessage(status.getMessage());
						return status;
					}
				}
			});
			WizardUtils.runInWizard(loadKeysJob, getContainer(), dbc);
		} catch (Exception e) {
			StatusManager.getManager().handle(
					OpenShiftUIActivator.createErrorStatus("Could not load ssh keys.", e), StatusManager.LOG);
		}
	}

	@Override
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.FORWARDS) {
			pageModel.reset();
			dbc.updateTargets();
		}
	}

	private void enableWidgets(boolean isNewProject) {
		if (isNewProject) {
			useDefaultRepoPathButton.setEnabled(true);
			useDefaultRemoteNameButton.setEnabled(false);
			useDefaultRemoteNameButton.setSelection(true);
			remoteNameText.setEnabled(false);
			remoteNameLabel.setEnabled(false);
		} else {
			useDefaultRepoPathButton.setEnabled(false);
			useDefaultRemoteNameButton.setEnabled(true);
			remoteNameText.setEnabled(!useDefaultRemoteNameButton.getSelection());
			remoteNameLabel.setEnabled(!useDefaultRemoteNameButton.getSelection());
		}
	}

	/**
	 * A multivalidator for the repo path. Validates the repo path on behalf of
	 * the selection to use the default repo path and the repo path value.
	 */
	class RepoPathValidationStatusProvider extends MultiValidator {

		private final IObservableValue repoPathObservable;
		private final IObservableValue applicationNameModelObservable;
		private final IObservableValue newProjectModelObservable;

		public RepoPathValidationStatusProvider(IObservableValue repoPathObservable,
				IObservableValue applicationNameModelObservable, IObservableValue newProjectModelObservable) {
			this.repoPathObservable = repoPathObservable;
			this.applicationNameModelObservable = applicationNameModelObservable;
			this.newProjectModelObservable = newProjectModelObservable;
		}

		// Validator is also be called when application name is set..
		@Override
		protected IStatus validate() {
			final String repoPath = (String) repoPathObservable.getValue();
			final String applicationName = (String) applicationNameModelObservable.getValue();
			final boolean newProject = (Boolean) newProjectModelObservable.getValue();

			if (newProject) {
				final IPath repoResourcePath = new Path(repoPath);
				if (repoResourcePath.isEmpty()
						|| !repoResourcePath.isAbsolute()
						|| !repoResourcePath.toFile().canWrite()) {
					return OpenShiftUIActivator.createErrorStatus("The location '" + repoResourcePath.toOSString()
							+ "' does not exist or is not writeable.");
				}
				final IPath applicationPath = applicationName != null ? repoResourcePath.append(new Path(
						applicationName)) : null;
				if (applicationPath != null && applicationPath.toFile().exists()) {
					return OpenShiftUIActivator.createErrorStatus(
							NLS.bind("The location \"{0}\" already contains a folder named \"{1}\"",
									repoResourcePath.toOSString(), applicationName));
				}
			}
			return ValidationStatus.ok();
		}

		public void forceRevalidate() {
			revalidate();
		}

	}

	/**
	 * A multi validator that validates the remote name on behalf of the
	 * selection to use the default remote name and the remote name value.
	 */
	class RemoteNameValidationStatusProvider extends MultiValidator {

		private final IObservableValue remoteNameObservable;
		private final IObservableValue projectNameObservable;

		public RemoteNameValidationStatusProvider(final IObservableValue remoteNameTextObservable,
				final IObservableValue projectNameObservable) {
			this.remoteNameObservable = remoteNameTextObservable;
			this.projectNameObservable = projectNameObservable;
		}

		@Override
		protected IStatus validate() {
			IStatus status = Status.OK_STATUS;
			String remoteName = (String) remoteNameObservable.getValue();
			String projectName = (String) projectNameObservable.getValue();

			// skip the validation if the user wants to create a new project.
			// The name and state of the existing project do
			// not matter...
			if (StringUtils.isEmpty(remoteName)) {
				return OpenShiftUIActivator.createErrorStatus(
						"The custom remote name must not be empty.");
			} else if (!remoteName.matches("\\S+")) {
				return OpenShiftUIActivator.createErrorStatus(
						"The custom remote name must not contain spaces.");
			} else if (!pageModel.isNewProject()
					&& hasRemoteName(remoteName, getProject(projectName))) {
				return OpenShiftUIActivator.createErrorStatus(NLS.bind(
						"The project {0} already has a remote named {1}.", projectName, remoteName));
			}
			return status;
		}

		public IProject getProject(final String projectName) {
			if (StringUtils.isEmpty(projectName)) {
				return null;
			}
			return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}

		private boolean hasRemoteName(String remoteName, IProject project) {
			try {
				if (project == null
						|| !project.isAccessible()) {
					return false;
				}

				Repository repository = EGitUtils.getRepository(project);
				if (repository == null) {
					return false;
				}
				return EGitUtils.hasRemote(remoteName, repository);
			} catch (Exception e) {
				OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(e.getMessage(), e));
				return false;
			}
		}
		
		public void forceRevalidate() {
			revalidate();
		}
	}
}
