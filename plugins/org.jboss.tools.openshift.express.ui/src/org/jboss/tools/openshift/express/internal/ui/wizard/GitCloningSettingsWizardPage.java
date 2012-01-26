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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
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

/**
 * @author André Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 * 
 */
public class GitCloningSettingsWizardPage extends AbstractOpenShiftWizardPage implements IWizardPage {

	private GitCloningSettingsWizardPageModel pageModel;
	private Button useDefaultRemoteNameButton;
	private Button useDefaultRepoPathButton;
	private Text remoteNameText;

	public GitCloningSettingsWizardPage(AbstractOpenShiftApplicationWizard<?> wizard, AbstractOpenShiftApplicationWizardModel model) {
		super(
				"Import an existing OpenShift application",
				"Configure the cloning settings by specifying the clone destination if you create a new project, and the git remote name if you're using an existing project.",
				"Cloning settings", wizard);
		this.pageModel = new GitCloningSettingsWizardPageModel(model);
		setPageComplete(false);
	}
	
	public GitCloningSettingsWizardPage(ImportExistingApplicationWizard wizard, AbstractOpenShiftApplicationWizardModel model) {
		super(
				"Import an existing OpenShift application",
				"Configure the cloning settings by specifying the clone destination if you create a new project, and the git remote name if you're using an existing project.",
				"Cloning settings", wizard);
		this.pageModel = new GitCloningSettingsWizardPageModel(model);
		setPageComplete(false);
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
		Group cloneGroup = new Group(parent, SWT.BORDER);
		cloneGroup.setText("Cloning settings");
		cloneGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).margins(10, 10)// .extendedMargins(0, 0, 0, 10)
				.applyTo(cloneGroup);
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
		final IObservableValue repoPathTextObservable = WidgetProperties.text(SWT.Modify).observe(repoPathText);
		final IObservableValue repoPathModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_REPO_PATH).observe(pageModel);
		ValueBindingBuilder.bind(repoPathTextObservable).to(repoPathModelObservable).in(dbc);

		Button browseRepoPathButton = new Button(cloneGroup, SWT.PUSH);
		browseRepoPathButton.setText("Browse");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseRepoPathButton);
		browseRepoPathButton.addSelectionListener(onRepoPath());

		final IObservableValue useDefaultRepoButtonSelectionObservable = WidgetProperties.selection().observe(
				useDefaultRepoPathButton);
		final IObservableValue useDefaultRepoModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_USE_DEFAULT_REPO_PATH).observe(pageModel);
		ValueBindingBuilder.bind(useDefaultRepoButtonSelectionObservable).to(useDefaultRepoModelObservable).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(repoPathText))
				.notUpdating(useDefaultRepoModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseRepoPathButton))
				.notUpdating(useDefaultRepoModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project location text control when not choosing the 'Use default location' option.
		useDefaultRepoPathButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				repoPathText.setFocus();
				repoPathText.selectAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		IObservableValue repoPathValidityObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_CUSTOM_REPO_PATH_VALIDITY).observe(pageModel);
		dbc.addValidationStatusProvider(new RepoPathValidationStatusProvider(repoPathValidityObservable,
				repoPathTextObservable));


		// Remote Name Management
		useDefaultRemoteNameButton = new Button(cloneGroup, SWT.CHECK);
		useDefaultRemoteNameButton.setText("Use default remote name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(useDefaultRemoteNameButton);
		useDefaultRemoteNameButton.addSelectionListener(onDefaultRemoteName());

		Label labelForRemoteName = new Label(cloneGroup, SWT.NONE);
		labelForRemoteName.setText("Remote name:");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(false, false).indent(10, 0)
				.applyTo(labelForRemoteName);
		remoteNameText = new Text(cloneGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().span(1, 1).align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(remoteNameText);
		Label fillerForRemoteName = new Label(cloneGroup, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(fillerForRemoteName);
		final IObservableValue remoteNameTextObservable = WidgetProperties.text(SWT.Modify).observe(remoteNameText);
		final IObservableValue remoteNameModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_REMOTE_NAME).observe(pageModel);
		ValueBindingBuilder.bind(remoteNameTextObservable).to(remoteNameModelObservable).in(dbc);

		final IObservableValue useDefaultRemoteNameButtonSelectionObservable = WidgetProperties.selection().observe(
				useDefaultRemoteNameButton);
		final IObservableValue useDefaultRemoteNameModelObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_USE_DEFAULT_REMOTE_NAME).observe(pageModel);

		ValueBindingBuilder.bind(useDefaultRemoteNameButtonSelectionObservable).to(useDefaultRemoteNameModelObservable)
				.in(dbc);
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(remoteNameText))
				.notUpdating(useDefaultRemoteNameModelObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project name text control when choosing the 'Use an existing project' option.
		useDefaultRemoteNameButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				remoteNameText.setFocus();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		IObservableValue remoteNameValidityObservable = BeanProperties.value(
				GitCloningSettingsWizardPageModel.PROPERTY_CUSTOM_REMOTE_NAME_VALIDITY).observe(pageModel);
		dbc.addValidationStatusProvider(new RemoteNameValidationStatusProvider(remoteNameValidityObservable,
				remoteNameTextObservable));

		Link sshPrefsLink = new Link(parent, SWT.NONE);
		sshPrefsLink.setText("Make sure your SSH key used with the domain is listed in <a>SSH2 Preferences</a>.");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(10, 0)
				.applyTo(sshPrefsLink);
		sshPrefsLink.addSelectionListener(onSshPrefs());

		return cloneGroup;
	}

	private SelectionListener onRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Choose the location to store your repository clone to...");
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					pageModel.setRepositoryPath(repositoryPath);
				}
			}
		};
	}

	private SelectionListener onDefaultRemoteName() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.resetRemoteName();
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
		// allow to enable a proj only for as7 openshift applications
		// setTitle(NLS.bind("Import OpenShift application {0}", pageModel.getApplicationName()));
		pageModel.resetRepositoryPath();
		pageModel.resetRemoteName();
		if (pageModel.isNewProject()) {
			useDefaultRepoPathButton.setEnabled(true);
			useDefaultRemoteNameButton.setEnabled(false);
			useDefaultRemoteNameButton.setSelection(true);
			remoteNameText.setEnabled(false);
		} else {
			useDefaultRepoPathButton.setEnabled(false);
			useDefaultRemoteNameButton.setEnabled(true);
			remoteNameText.setEnabled(!useDefaultRemoteNameButton.getSelection());
		}
		// pageModel.getWizardModel().setProperty(AdapterWizardPageModel.MODE, AdapterWizardPageModel.MODE_SOURCE);
		onPageActivatedBackground(dbc);
	}

	protected void onPageActivatedBackground(final DataBindingContext dbc) {
		/*
		 * new Job("Loading remote OpenShift application") { public IStatus run(IProgressMonitor monitor) { try {
		 * pageModel.loadGitUri(); pageModel.loadApplicationUrl(); } catch (OpenShiftException e) { IStatus status =
		 * OpenShiftUIActivator.createErrorStatus(e.getMessage(), e); OpenShiftUIActivator.log(status); return status; }
		 * return Status.OK_STATUS; } }.schedule();
		 */
	}

	class RepoPathValidationStatusProvider extends MultiValidator {

		private final IObservableValue repoPathValidityObservable;

		private final IObservableValue repoPathTextObservable;

		public RepoPathValidationStatusProvider(IObservableValue repoPathValidityObservable,
				IObservableValue repoPathTextObservable) {
			this.repoPathValidityObservable = repoPathValidityObservable;
			this.repoPathTextObservable = repoPathTextObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus repoPathValidityStatus = (IStatus) repoPathValidityObservable.getValue();

			if (repoPathValidityStatus != null) {
				return repoPathValidityStatus;
			}
			return ValidationStatus.ok();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.databinding.validation.MultiValidator#getTargets()
		 */
		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(repoPathTextObservable);
			return targets;
		}

	}

	class RemoteNameValidationStatusProvider extends MultiValidator {

		private final IObservableValue remoteNameValidityObservable;

		private final IObservableValue remoteNameTextObservable;

		public RemoteNameValidationStatusProvider(IObservableValue remoteNameValidityObservable,
				IObservableValue remoteNameTextObservable) {
			this.remoteNameValidityObservable = remoteNameValidityObservable;
			this.remoteNameTextObservable = remoteNameTextObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus remoteNameValidityStatus = (IStatus) remoteNameValidityObservable.getValue();

			if (remoteNameValidityStatus != null) {
				return remoteNameValidityStatus;
			}
			return ValidationStatus.ok();
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.databinding.validation.MultiValidator#getTargets()
		 */
		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(remoteNameTextObservable);
			return targets;
		}

	}

}
