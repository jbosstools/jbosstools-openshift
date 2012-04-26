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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
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
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.common.ui.ssh.SshPrivateKeysPreferences;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 * 
 */
public class GitCloningSettingsWizardPage extends AbstractOpenShiftWizardPage implements IWizardPage {

	private GitCloningSettingsWizardPageModel pageModel;
	private Button useDefaultRemoteNameButton;
	private Button useDefaultRepoPathButton;
	private Text remoteNameText;
	private Label remoteNameLabel;

	public GitCloningSettingsWizardPage(OpenShiftExpressApplicationWizard wizard, IOpenShiftExpressWizardModel model) {
		super(
				"Import an existing OpenShift application",
				"Configure the cloning settings by specifying the clone destination if you create a new project, and the git remote name if you're using an existing project.",
				"Cloning settings", wizard);
		this.pageModel = new GitCloningSettingsWizardPageModel(model);
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
		useDefaultRepoPathButton.setText("Use default location");
		useDefaultRepoPathButton.setToolTipText("Uncheck if you want to use a custom location for your project");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(useDefaultRepoPathButton);
		Label labelForRepoPath = new Label(cloneGroup, SWT.NONE);
		labelForRepoPath.setText("Location:");
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

		dbc.addValidationStatusProvider(
				new RepoPathValidationStatusProvider(
						isDefaultRepoObservable
						, repoPathObservable
						, BeanProperties.value(GitCloningSettingsWizardPageModel.PROPERTY_APPLICATION_NAME).observe(
								pageModel)));

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

		dbc.addValidationStatusProvider(
				new RemoteNameValidationStatusProvider(
						useDefaultRemoteNameObservable, remoteNameTextObservable));

		Link sshPrefsLink = new Link(parent, SWT.NONE);
		sshPrefsLink.setText("Make sure your SSH key used with the domain is listed in <a>SSH2 Preferences</a>.");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(10, 0)
				.applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());

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

	private SelectionAdapter onSshPrefs() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SshPrivateKeysPreferences.openPreferencesPage(getShell());
			}
		};
	}

	protected void onPageActivated(DataBindingContext dbc) {
		enableWidgets(pageModel.isNewProject());
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

		private final IObservableValue isDefaultRepoPathObservable;
		private final IObservableValue repoPathObservable;
		private IObservableValue applicationNameObservable;

		public RepoPathValidationStatusProvider(IObservableValue isDefaultRepoPathObservable,
				IObservableValue repoPathObservable, IObservableValue applicationNameObservable) {
			this.isDefaultRepoPathObservable = isDefaultRepoPathObservable;
			this.repoPathObservable = repoPathObservable;
			this.applicationNameObservable = applicationNameObservable;
		}

		@Override
		protected IStatus validate() {
			Boolean isDefaultRepoPath = (Boolean) isDefaultRepoPathObservable.getValue();
			String repoPath = (String) repoPathObservable.getValue();
			String applicationName = (String) applicationNameObservable.getValue();

			// skip the validation if the user wants to create a new project.
			// The
			// name and state of the existing project do
			// not matter...
			if (applicationName == null
					|| applicationName.length() == 0) {
				return OpenShiftUIActivator
						.createCancelStatus("You have to choose an application name / existing application");
			}

			if (isDefaultRepoPath == null
					|| !isDefaultRepoPath) {
				final IPath repoResourcePath = new Path(repoPath);
				if (repoResourcePath.isEmpty()
						|| !repoResourcePath.isAbsolute()
						|| !repoResourcePath.toFile().canWrite()) {
					return OpenShiftUIActivator.createErrorStatus("The path does not exist or is not writeable.");
				}
				final IPath applicationPath = repoResourcePath.append(new Path(applicationName));
				if (applicationPath.toFile().exists()) {
					return OpenShiftUIActivator.createErrorStatus(
							NLS.bind("The location \"{0}\" already contains a folder named \"{1}\"",
									repoResourcePath.toOSString(), applicationName));
				}
			}

			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(repoPathObservable);
			return targets;
		}

	}

	/**
	 * A multi validator that validates the remote name on behalf of the
	 * selection to use the default remote name and the remote name value.
	 */
	class RemoteNameValidationStatusProvider extends MultiValidator {

		private final IObservableValue isDefaultRemoteNameObservable;
		private final IObservableValue remoteNameObservable;

		public RemoteNameValidationStatusProvider(IObservableValue isDefaultRemoteName,
				IObservableValue remoteNameTextObservable) {
			this.isDefaultRemoteNameObservable = isDefaultRemoteName;
			this.remoteNameObservable = remoteNameTextObservable;
		}

		@Override
		protected IStatus validate() {
			IStatus status = Status.OK_STATUS;
			Boolean isDefaultRemoteName = (Boolean) isDefaultRemoteNameObservable.getValue();
			String remoteName = (String) remoteNameObservable.getValue();

			// skip the validation if the user wants to create a new project.
			// The name and state of the existing project do
			// not matter...
			if (isDefaultRemoteName == null
					|| !isDefaultRemoteName) {
				if (StringUtils.isEmpty(remoteName)) {
					return OpenShiftUIActivator.createErrorStatus(
							"The custom remote name must not be empty.");
				} else if (!remoteName.matches("\\S+")) {
					return OpenShiftUIActivator.createErrorStatus(
							"The custom remote name must not contain spaces.");
				} else if (hasRemoteName(remoteName, pageModel.getProject())) {
					return OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"The existing project already has a remote named {0}.", remoteName));
				}
			}
			return status;
		}

		private boolean hasRemoteName(String remoteName, IProject project) {
			try {
				if (project == null
						|| !project.isAccessible()) {
					return false;
				}

				Repository repository = EGitUtils.getRepository(project);
				return EGitUtils.hasRemote(remoteName, repository);
			} catch (Exception e) {
				OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(e.getMessage(), e));
				return false;
			}
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(remoteNameObservable);
			return targets;
		}
	}

}
