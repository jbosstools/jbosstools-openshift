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
package org.jboss.tools.openshift.internal.ui.wizard.importapp;

import java.io.File;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.FileExistsConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 * 
 */
public class GitCloningWizardPage extends AbstractOpenShiftWizardPage {

	private IGitCloningPageModel model;
	private Button useDefaultRepoPathButton;
	private Button reuseGitRepositoryButton;
	private RepoPathValidator repoPathValidator;

	public GitCloningWizardPage(IWizard wizard, IGitCloningPageModel model) {
		super(
				getTitle(model),
				"Configure the cloning settings by specifying the clone destination",
				"Cloning settings", wizard);
		this.model = model;
	}

	private static String getTitle(IGitCloningPageModel model) {
		String name = (model == null)? null: model.getApplicationName();
		return (name == null)?
				"Import an existing OpenShift application":
				NLS.bind("Import the {0} OpenShift application", name);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		Composite cloneGroup = createCloneGroup(parent, dbc);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, false)
			.applyTo(cloneGroup);
		Composite filler = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(filler);
	}

	private Composite createCloneGroup(Composite parent, DataBindingContext dbc) {
		Group cloneGroup = new Group(parent, SWT.NONE);
		cloneGroup.setText("Cloning settings");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).grab(true, false)
			.applyTo(cloneGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(3).equalWidth(false).margins(10, 10).applyTo(cloneGroup);

		Composite composite = new Composite(cloneGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(composite);
		GridLayoutFactory.fillDefaults()
			.numColumns(3).margins(15, 15)
			.applyTo(composite);

		// Repo Path
		this.useDefaultRepoPathButton = new Button(composite, SWT.CHECK);
		useDefaultRepoPathButton.setText("Use default clone destination");
		useDefaultRepoPathButton.setToolTipText("Uncheck if you want to use a custom location to clone to");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(3, 1).applyTo(useDefaultRepoPathButton);
		Label labelForRepoPath = new Label(composite, SWT.NONE);
		labelForRepoPath.setText("Git Clone Location:");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).grab(false, false).indent(10, 0)
			.applyTo(labelForRepoPath);
		final Text repoPathText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false)
			.applyTo(repoPathText);
		final IObservableValue<String> repoPathObservable = WidgetProperties.text(SWT.Modify).observe(repoPathText);
		final IObservableValue<String> repoPathModelObservable = 
				BeanProperties.value(IGitCloningPageModel.PROPERTY_REPOSITORY_PATH).observe(model);
		ValueBindingBuilder
			.bind(repoPathObservable)
			.to(repoPathModelObservable)
			.in(dbc);

		Button browseRepoPathButton = new Button(composite, SWT.PUSH);
		browseRepoPathButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseRepoPathButton);
		browseRepoPathButton.addSelectionListener(onRepoPath());

		final IObservableValue<Boolean> useDefaultRepoButtonObservable =
				WidgetProperties.selection().observe(useDefaultRepoPathButton);
		final IObservableValue<Boolean> useDefaultRepoModelObservable = 
				BeanProperties.value(IGitCloningPageModel.PROPERTY_USE_DEFAULT_REPOSITORY_PATH).observe(model);
		ValueBindingBuilder
			.bind(useDefaultRepoButtonObservable)
			.to(useDefaultRepoModelObservable)
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(repoPathText))
			.notUpdating(useDefaultRepoModelObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(browseRepoPathButton))
			.notUpdating(useDefaultRepoModelObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		// move focus to the project location text control when not choosing the
		// 'Use default location' option.
		UIUtils.focusOnSelection(useDefaultRepoPathButton, repoPathText);

		// Skip clone
		this.reuseGitRepositoryButton = new Button(composite, SWT.CHECK);
		reuseGitRepositoryButton.setSelection(false);
		reuseGitRepositoryButton.setText("Reuse existing repository");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).span(3, 1)
			.applyTo(reuseGitRepositoryButton);
		reuseGitRepositoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setReuseGitRepository(reuseGitRepositoryButton.getSelection());
			}
		});
		IObservableValue<File> cloneDestinationObservable =
				BeanProperties.value(IGitCloningPageModel.PROPERTY_CLONE_DESTINATION).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(reuseGitRepositoryButton))
			.notUpdating(cloneDestinationObservable)
			.converting(new FileExistsConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.visible().observe(reuseGitRepositoryButton))
			.notUpdating(cloneDestinationObservable)
			.converting(new FileExistsConverter())
			.in(dbc);
		final IObservableValue<Boolean> reuseGitReposityObservable =
				BeanProperties.value(IGitCloningPageModel.PROPERTY_REUSE_GIT_REPOSITORY).observe(model);
		IObservableValue<String> repoNameObservable =
				BeanProperties.value(IGitCloningPageModel.PROPERTY_REPO_NAME).observe(model);
		this.repoPathValidator = new RepoPathValidator(
						useDefaultRepoModelObservable, repoPathObservable, reuseGitReposityObservable, cloneDestinationObservable);
		dbc.addValidationStatusProvider(repoPathValidator);
		ControlDecorationSupport.create(repoPathValidator, SWT.LEFT | SWT.TOP);

		return cloneGroup;
	}

	private SelectionListener onRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Git clone location");
				dialog.setMessage("Choose the location for git clone...");
				dialog.setFilterPath(model.getRepositoryPath());
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					model.setRepositoryPath(repositoryPath);
				}
			}
		};
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		repoPathValidator.forceRevalidate();
	}

	/**
	 * A multivalidator for the repo path. Validates the repo path on behalf of
	 * the selection to use the default repo path and the repo path value.
	 */
	class RepoPathValidator extends MultiValidator {

		private final IObservableValue<Boolean> useDefaultRepoObservable;
		private final IObservableValue<String> repoPathObservable;
		private final IObservableValue<Boolean> reuseGitCloneObservable;
		private final IObservableValue<File> cloneDestinationObservable;

		public RepoPathValidator(IObservableValue<Boolean> useDefaultRepoObservable, IObservableValue<String> repoPathObservable,
				IObservableValue<Boolean> skipCloneObservable, IObservableValue<File> cloneDestinationObservable) {
			this.useDefaultRepoObservable = useDefaultRepoObservable;
			this.repoPathObservable = repoPathObservable;
			this.reuseGitCloneObservable = skipCloneObservable;
			this.cloneDestinationObservable = cloneDestinationObservable;
		}

		@Override
		protected IStatus validate() {
			useDefaultRepoObservable.getValue();
			final String repoPath = repoPathObservable.getValue();
			final Boolean reuseGitRepository = reuseGitCloneObservable.getValue();
			final File cloneDestination = cloneDestinationObservable.getValue();

			final IPath repoResourcePath = new Path(repoPath);
			if (repoResourcePath.isEmpty()
					|| !repoResourcePath.isAbsolute()) {
				return ValidationStatus.cancel("You need to provide an absolute path that we'll clone to.");
			} 

			if (!FileUtils.canWrite(repoResourcePath.toOSString())) {
				return ValidationStatus.error(
						NLS.bind("The location {0} is not writeable.", repoResourcePath.toOSString()));
			} 

			boolean cloneDestinationExists = FileUtils.exists(cloneDestination);
			if (reuseGitRepository) {
				if (!cloneDestinationExists) {
					return ValidationStatus.error(
							NLS.bind("The location \"{0}\" does not contain a folder named \"{1}\"\n"
									+ "Please clone the repository or browse to a location containing the repository.",
									repoResourcePath.toOSString(), model.getRepoName()));
				}
			} else {
				if (cloneDestinationExists) {
					return ValidationStatus.error(
							NLS.bind("The location \"{0}\" already contains a folder named \"{1}\"\n"
									+ "Please choose a different destination, or select 'Reuse existing repository'.",
									repoResourcePath.toOSString(), model.getRepoName()));
				}
			}
			return ValidationStatus.ok();
		}

		public void forceRevalidate() {
			revalidate();
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			setTitle(getTitle(model));
		}
		super.setVisible(visible);
	}

}
