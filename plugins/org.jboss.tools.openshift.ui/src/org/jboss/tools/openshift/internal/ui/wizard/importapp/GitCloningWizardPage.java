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
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.ComputedValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.ide.eclipse.as.core.util.RegExUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.core.plugin.log.StatusFactory;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.internal.common.ui.databinding.FileExistsConverter;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * @author Andre Dietisheim
 * @author Rob Stryker
 * @author Xavier Coulon
 * 
 */
public class GitCloningWizardPage extends AbstractOpenShiftWizardPage {

	private IGitCloningPageModel model;
	private CloneDestinationPathValidator cloneDestinationPathValidator;

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
	}
	
	private Composite createCloneGroup(Composite parent, DataBindingContext dbc) {
		Group cloneGroup = new Group(parent, SWT.NONE);
		cloneGroup.setText("Clone destination");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.TOP).grab(true, false)	
			.applyTo(cloneGroup);
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).applyTo(cloneGroup);

		Composite cloneGroupComposite = new Composite(cloneGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(cloneGroupComposite);
		GridLayoutFactory.fillDefaults()
			.numColumns(3)
			.applyTo(cloneGroupComposite);

		// use default clone destination path
		Button useDefaultCloneDestinationButton = new Button(cloneGroupComposite, SWT.CHECK);
		useDefaultCloneDestinationButton.setText("Use default clone destination");
		useDefaultCloneDestinationButton.setToolTipText("Uncheck if you want to use a custom location to clone to");
		GridDataFactory.fillDefaults()
			.span(3, 1).align(SWT.LEFT, SWT.CENTER).applyTo(useDefaultCloneDestinationButton);
		final IObservableValue<Boolean> useDefaultCloneDestinationObservable = 
				BeanProperties.value(IGitCloningPageModel.PROPERTY_USE_DEFAULT_CLONE_DESTINATION).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.selection().observe(useDefaultCloneDestinationButton))
			.to(useDefaultCloneDestinationObservable)
			.in(dbc);

		// clone destination
		Label repoPathLabel = new Label(cloneGroupComposite, SWT.NONE);
		repoPathLabel.setText("Git Clone Location:");
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(repoPathLabel);
		final Text cloneDestinationText = new Text(cloneGroupComposite, SWT.BORDER);
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cloneDestinationText);
		final IObservableValue<String> cloneDestinationObservable = WidgetProperties.text(SWT.Modify).observe(cloneDestinationText);
		final IObservableValue<String> cloneDestinationModelObservable = 
				BeanProperties.value(IGitCloningPageModel.PROPERTY_CLONE_DESTINATION).observe(model);
		ValueBindingBuilder
			.bind(cloneDestinationObservable)
			.to(cloneDestinationModelObservable)
			.in(dbc);
		Button browseCloneDestinationButton = new Button(cloneGroupComposite, SWT.PUSH);
		browseCloneDestinationButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseCloneDestinationButton);
		browseCloneDestinationButton.addSelectionListener(onCloneDestination());
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(cloneDestinationText))
			.notUpdating(useDefaultCloneDestinationObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(browseCloneDestinationButton))
			.notUpdating(useDefaultCloneDestinationObservable)
			.converting(new InvertingBooleanConverter())
			.in(dbc);
		// move focus to the project location text control when not choosing the
		// 'Use default location' option.
		UIUtils.focusOnSelection(useDefaultCloneDestinationButton, cloneDestinationText);

		// Reuse git repository
		Button reuseRepositoryButton = new Button(cloneGroupComposite, SWT.CHECK);
		reuseRepositoryButton.setSelection(false);
		reuseRepositoryButton.setText("Do not clone - use existing repository");
		GridDataFactory.fillDefaults()
			.span(3,1).align(SWT.LEFT, SWT.CENTER).applyTo(reuseRepositoryButton);
		final IObservableValue<Boolean> reuseGitReposityObservable =
				WidgetProperties.selection().observe(reuseRepositoryButton);
		ValueBindingBuilder
			.bind(reuseGitReposityObservable)
			.to(BeanProperties.value(IGitCloningPageModel.PROPERTY_REUSE_GIT_REPOSITORY).observe(model))
			.in(dbc);
		IObservableValue<File> repoPathObservable =
				BeanProperties.value(IGitCloningPageModel.PROPERTY_REPO_PATH).observe(model);
		ValueBindingBuilder
			.bind(WidgetProperties.enabled().observe(reuseRepositoryButton))
			.notUpdating(repoPathObservable)
			.converting(new FileExistsConverter())
			.in(dbc);
		ISWTObservableValue reuseRepoButtonEnabled = WidgetProperties.enabled().observe(reuseRepositoryButton);
		ValueBindingBuilder
			.bind(reuseRepoButtonEnabled)
			.notUpdating(repoPathObservable)
			.converting(new Converter(File.class, Boolean.class) {
 
				@Override
				public Object convert(Object fromObject) {
					return fromObject instanceof File
							&& EGitUtils.isRepository((File) fromObject);
				}})
			.in(dbc);
		this.cloneDestinationPathValidator = new CloneDestinationPathValidator(
						useDefaultCloneDestinationObservable, cloneDestinationObservable, reuseGitReposityObservable, repoPathObservable);
		dbc.addValidationStatusProvider(cloneDestinationPathValidator);
		ControlDecorationSupport.create(cloneDestinationPathValidator, SWT.LEFT | SWT.TOP);

		// checkout reused repo button
		Composite checkoutComposite = new Composite(cloneGroupComposite, SWT.NONE);
		GridDataFactory.fillDefaults()
			.span(3,1).indent(20, SWT.DEFAULT).align(SWT.FILL, SWT.FILL).grab(true, true)
			.applyTo(checkoutComposite);
		GridLayoutFactory.fillDefaults()
			.numColumns(2)
			.applyTo(checkoutComposite);
		Button checkoutBranchCheckbox = new Button(checkoutComposite, SWT.CHECK);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).applyTo(checkoutBranchCheckbox);		
		ISWTObservableValue checkoutBranchCheckboxObservable = WidgetProperties.selection().observe(checkoutBranchCheckbox);
		ValueBindingBuilder
			.bind(checkoutBranchCheckboxObservable)
			.validatingAfterConvert(new CheckoutBranchValidator())
			.to(BeanProperties.value(IGitCloningPageModel.PROPERTY_CHECKOUT_BRANCH_REUSED_REPO).observe(model))
			.validatingAfterConvert(new CheckoutBranchValidator())
			.in(dbc);
		IObservableValue<Boolean> isRepositoryBranchGitRefObservable = 
				BeanProperties.value(IGitCloningPageModel.PROPERTY_IS_REPOSITORY_BRANCH_GIT_REF).observe(model);
		ComputedValue<Boolean> checkoutBranchEnablement = new ComputedValue<Boolean>() {

			@Override
			protected Boolean calculate() {
				// access all involved observables
				Boolean isReuseRepoButtonEnabled = (Boolean) reuseRepoButtonEnabled.getValue();
				Boolean isRepositoryBranchGitRef = isRepositoryBranchGitRefObservable.getValue();
				Boolean isReuseGitRepository = reuseGitReposityObservable.getValue();
				return isReuseRepoButtonEnabled
						&& !isRepositoryBranchGitRef
						&& isReuseGitRepository;
			}
		};
		// checkout branch label
		Label checkoutBranchLabel = new Label(checkoutComposite, SWT.NONE);
		GridDataFactory.fillDefaults()
			.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(checkoutBranchLabel);		
		checkoutBranchLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				checkoutBranchCheckboxObservable.setValue(!checkoutBranchCheckbox.getSelection());
			}});
		ValueBindingBuilder
			.bind(WidgetProperties.text().observe(checkoutBranchLabel))
			.notUpdating(BeanProperties.value(IGitCloningPageModel.PROPERTY_GIT_REF).observe(model))
			.converting(new Converter(String.class, String.class) {
				@Override
				public Object convert(Object fromObject) {
					return "Check out branch " + fromObject;
				}
			})
			.in(dbc);
		ValueBindingBuilder
			.bind(new WritableValue<Boolean>() {

				@Override
				public void doSetValue(Boolean value) {
					checkoutBranchCheckbox.setEnabled(value);
					checkoutBranchLabel.setEnabled(value);
				}
			})	
			.notUpdating(checkoutBranchEnablement)
			.in(dbc);

		return cloneGroup;
	}

	private SelectionListener onCloneDestination() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Git clone location");
				dialog.setMessage("Choose the location for git clone...");
				dialog.setFilterPath(model.getCloneDestination());
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					model.setCloneDestination(repositoryPath);
				}
			}
		};
	}

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		if (cloneDestinationPathValidator != null) {
			cloneDestinationPathValidator.forceRevalidate();
		}
	}

	/**
	 * A multivalidator for the repo path. Validates the repo path on behalf of
	 * the selection to use the default repo path and the repo path value.
	 */
	private class CloneDestinationPathValidator extends MultiValidator {

		private final IObservableValue<Boolean> useDefaultCloneDestinationObservable;
		private final IObservableValue<String> cloneDestinationObservable;
		private final IObservableValue<Boolean> reuseGitCloneObservable;
		private final IObservableValue<File> repoPathObservable;

		public CloneDestinationPathValidator(IObservableValue<Boolean> useDefaultCloneDestinationObservable, 
				IObservableValue<String> cloneDestinationObservable, IObservableValue<Boolean> skipCloneObservable, 
				IObservableValue<File> repoPathObservable) {
			this.useDefaultCloneDestinationObservable = useDefaultCloneDestinationObservable;
			this.cloneDestinationObservable = cloneDestinationObservable;
			this.reuseGitCloneObservable = skipCloneObservable;
			this.repoPathObservable = repoPathObservable;
		}

		@Override
		protected IStatus validate() {
			// access all involved observables
			useDefaultCloneDestinationObservable.getValue();
			final String cloneDestination = cloneDestinationObservable.getValue();
			final Boolean reuseGitRepository = reuseGitCloneObservable.getValue();
			final File repoPath = repoPathObservable.getValue();

			final IPath cloneDestinationPath = new Path(cloneDestination);
			if (cloneDestinationPath.isEmpty()
					|| !cloneDestinationPath.isAbsolute()) {
				return ValidationStatus.cancel("You need to provide an absolute path that we'll clone to.");
			}

			if (!FileUtils.canWrite(cloneDestinationPath.toOSString())) {
				return ValidationStatus.error(
						NLS.bind("The location {0} is not writeable.", cloneDestinationPath.toOSString()));
			}

			boolean cloneDestinationExists = FileUtils.exists(repoPath);
			if (reuseGitRepository) {
				return validateReuseGitRepo(repoPath, cloneDestinationPath, cloneDestinationExists);
			} else {
				return validateClone(cloneDestinationPath, cloneDestinationExists);
			}
		}

		private IStatus validateClone(final IPath cloneDestinationPath, boolean cloneDestinationExists) {
			if (cloneDestinationExists) {
				return ValidationStatus.error(
						NLS.bind("There already is a folder named \"{0}\" in \"{1}\".\n"
								+ "Please choose a different destination, or select 'Reuse existing repository'.",
								model.getRepoName(), cloneDestinationPath.toOSString()));
			}
			return ValidationStatus.ok();
		}

		private IStatus validateReuseGitRepo(final File repoPath, final IPath cloneDestinationPath,
				boolean cloneDestinationExists) {
			if (!cloneDestinationExists) {
				return ValidationStatus.error(
						NLS.bind("There is no folder named \"{0}\" in \"{1}\"\n"
								+ "Please clone the repository or browse to a location containing the repository.",
								model.getRepoName(), cloneDestinationPath.toOSString()));
			}

			if (!EGitUtils.isRepository(repoPath)) {
				return ValidationStatus.error(
						NLS.bind("There already is a folder named \"{0}\" in \"{1}\" but it isnt a git repository.\n"
								+ "Please remove this folder or use a different location.",
								model.getRepoName(), cloneDestinationPath.toOSString()));
			} 

			if (!hasRemoteUrl(model.getGitUrl(), cloneDestinationPath)) {
				return ValidationStatus.warning(NLS.bind(
						"The reused git repository has no remote to {2}. "
								+ "It does not seem to match the source that is used in your OpenShift application.",
						new String[] { model.getRepoName(), cloneDestinationPath.toOSString(),
								model.getGitUrl() }));
			}
			return ValidationStatus.ok();
		}

		private boolean hasRemoteUrl(final String gitUrl, final IPath repoResourcePath) {
			try {
				return EGitUtils.hasRemoteUrl(
						Pattern.compile(RegExUtils.escapeRegex(gitUrl)), model.getRepository());
			} catch (CoreException e) {
				OpenShiftUIActivator.getDefault().getLog().log(StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID,
					NLS.bind("Could not inspect remotes for git repo {0} at {1}.", 
						model.getCloneDestination(), repoResourcePath.toOSString()), e));
				return false;
			}
		}

		public void forceRevalidate() {
			revalidate();
		}
	}

	/**
	 * A validator that warns if the repository for the reused git repository
	 * isnt in the current branch that's used in openshift.
	 * 
	 * @see IGitCloningPageModel#getRepoPath()
	 * @see IGitCloningPageModel#getGitRef()
	 */
	private class CheckoutBranchValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			if (!(value instanceof Boolean)) {
				return ValidationStatus.ok();
			}

			boolean checkoutBranch = (boolean) value;
			Repository repository = model.getRepository();
			if (checkoutBranch 
					&& repository != null
					&& !isCurrentBranch(model.getGitRef(), repository)) {
				return ValidationStatus.warning(
						NLS.bind("Branch {0} will be checked out in the reused git repository.\n"
								+ "Please make sure it is clean.",
								model.getGitRef()));
			}
			return ValidationStatus.ok();
		}

		private boolean isCurrentBranch(String gitRef, Repository repository) {
			if (StringUtils.isBlank(gitRef)) {
				return true;
			}

			try {
				String branch = EGitUtils.getCurrentBranch(repository);
				return gitRef.equals(branch);
			} catch (CoreException e) {
				OpenShiftUIActivator.getDefault().getLog().log(
						StatusFactory.errorStatus(OpenShiftUIActivator.PLUGIN_ID,
								NLS.bind("Could not get current branch in repository at {0} .", 
										repository.getDirectory()), e));
					return false;
			}
		}

	}
	
	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this, dbc);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			setTitle(getTitle(model));
		}
		super.setVisible(visible);
	}
}
