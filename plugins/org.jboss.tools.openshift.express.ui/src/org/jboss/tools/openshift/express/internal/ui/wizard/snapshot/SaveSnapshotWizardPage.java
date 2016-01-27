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
package org.jboss.tools.openshift.express.internal.ui.wizard.snapshot;

import java.io.File;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.common.core.utils.FileUtils;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.common.ui.SelectProjectDialog;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class SaveSnapshotWizardPage extends AbstractOpenShiftWizardPage {

	private SaveSnapshotWizardPageModel pageModel;

	public SaveSnapshotWizardPage(SaveSnapshotWizardModel wizardModel, IWizard wizard) {
		this("Save Snapshot Cartridges",
				NLS.bind("Please choose the snapshot type and file for the snapshot of application {0}",
						StringUtils.null2emptyString(wizardModel.getApplication().getName())),
				wizardModel, wizard);
	}

	protected SaveSnapshotWizardPage(String title, String description, SaveSnapshotWizardModel wizardModel,
			IWizard wizard) {
		super(title, description, title, wizard);
		this.pageModel = new SaveSnapshotWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		// snapshot type
		GridLayoutFactory.fillDefaults()
				.numColumns(5).margins(10, 10).applyTo(parent);

		Label snapshotTypeLabel = new Label(parent, SWT.None);
		snapshotTypeLabel.setText("Snapshot Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(snapshotTypeLabel);

		Button fullSnapshotButton = new Button(parent, SWT.RADIO);
		fullSnapshotButton.setText("Full");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(fullSnapshotButton);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(fullSnapshotButton))
				.converting(new InvertingBooleanConverter())
				.to(BeanProperties.value(SaveSnapshotWizardPageModel.PROPERTY_DEPLOYMENT_SNAPSHOT).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		Button deploymentSnapshotButton = new Button(parent, SWT.RADIO);
		deploymentSnapshotButton.setText("Deployment");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(deploymentSnapshotButton);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(deploymentSnapshotButton))
				.to(BeanProperties.value(SaveSnapshotWizardPageModel.PROPERTY_DEPLOYMENT_SNAPSHOT).observe(pageModel))
				.in(dbc);

		// horizontal fillers
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(new Composite(parent, SWT.None));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(new Composite(parent, SWT.None));

		// destination
		Label filepathLabel = new Label(parent, SWT.None);
		filepathLabel.setText("Destination:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(filepathLabel);

		Text filepathText = new Text(parent, SWT.BORDER);
		filepathText.setEditable(false);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.CENTER).hint(200, SWT.DEFAULT).grab(true, false).applyTo(filepathText);
		ISWTObservableValue filenameObservable = WidgetProperties.text(SWT.Modify).observe(filepathText);
		ValueBindingBuilder
				.bind(filenameObservable)
				.to(BeanProperties.value(SaveSnapshotWizardPageModel.PROPERTY_FILEPATH).observe(pageModel))
				.in(dbc);

		Button workspaceButton = new Button(parent, SWT.PUSH);
		workspaceButton.setText("Workspace...");
		GridDataFactory.fillDefaults()
				.align(SWT.CENTER, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(workspaceButton);
		workspaceButton.addSelectionListener(onWorkspace());

		Button browseButton = new Button(parent, SWT.PUSH);
		browseButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseButton);
		browseButton.addSelectionListener(onBrowse());

		MultiValidator filenameValidator = new FilenameValidator(filenameObservable);
		dbc.addValidationStatusProvider(filenameValidator);
		ControlDecorationSupport.create(
				filenameValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// vertical filler
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).span(3, 1).grab(true, true).applyTo(new Composite(parent, SWT.None));
	}

	private SelectionAdapter onBrowse() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.None);
				dialog.setText("Choose the destination for your snapshot");
				dialog.setFilterPath(FileUtils.getParent(pageModel.getFilepath()));
				String destination = dialog.open();
				if (!StringUtils.isEmpty(destination)) {
					pageModel.setDestination(destination);
				}
			}
		};
	}

	private SelectionAdapter onWorkspace() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectProjectDialog dialog = new SelectProjectDialog(getShell());
				if (dialog.open() == IDialogConstants.OK_ID) {
					IProject project = dialog.getSelectedProject();
					if (project != null) {
						pageModel.setProject(project);
					}
				}
			}
		};
	}
	
	static class FilenameValidator extends MultiValidator {

		private IObservableValue filenameObservable;

		public FilenameValidator(IObservableValue filenameObservable) {
			this.filenameObservable = filenameObservable;
		}

		@Override
		protected IStatus validate() {
			String filename = (String) filenameObservable.getValue();

			if (StringUtils.isEmpty(filename)) {
				return ValidationStatus.cancel("Please provide a file that we can save your snapshot to.");
			} else {
				File snapshotFile = new File(filename);
				if (snapshotFile.exists()) {
					return ValidationStatus.warning(NLS.bind("File {0} already exists. Saving to it may overwrite it.", filename));
				} else if (snapshotFile.isDirectory() 
						&& !snapshotFile.canWrite()) {
					return ValidationStatus.error(NLS.bind("Cannot write to file {0}. Please check your permissions.", filename));
				}
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(filenameObservable);
			return targets;
		}
	}

}