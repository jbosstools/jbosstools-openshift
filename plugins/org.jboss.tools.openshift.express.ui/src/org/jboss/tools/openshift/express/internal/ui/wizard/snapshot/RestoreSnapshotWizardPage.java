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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.FileUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author André Dietisheim
 */
public class RestoreSnapshotWizardPage extends AbstractOpenShiftWizardPage {

	private RestoreSnapshotWizardPageModel pageModel;

	public RestoreSnapshotWizardPage(RestoreSnapshotWizardModel wizardModel, IWizard wizard) {
		this("Restore/Deploy Snapshot Cartridges",
				NLS.bind("Please choose the snapshot type and file that we will restore/deploy to application {0}",
						StringUtils.null2emptyString(wizardModel.getApplication().getName())),
				wizardModel, wizard);
	}

	protected RestoreSnapshotWizardPage(String title, String description, RestoreSnapshotWizardModel wizardModel,
			IWizard wizard) {
		super(title, description, title, wizard);
		this.pageModel = new RestoreSnapshotWizardPageModel(wizardModel);
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
				.to(BeanProperties.value(RestoreSnapshotWizardPageModel.PROPERTY_DEPLOYMENT_SNAPSHOT)
						.observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);

		Button deploymentSnapshotButton = new Button(parent, SWT.RADIO);
		deploymentSnapshotButton.setText("Deployment");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(deploymentSnapshotButton);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(deploymentSnapshotButton))
				.to(BeanProperties.value(RestoreSnapshotWizardPageModel.PROPERTY_DEPLOYMENT_SNAPSHOT)
						.observe(pageModel))
				.in(dbc);

		// horizontal filler
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(new Composite(parent, SWT.None));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(new Composite(parent, SWT.None));

		// file
		Label filepathLabel = new Label(parent, SWT.None);
		filepathLabel.setText("File:");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(filepathLabel);

		Text filepathText = new Text(parent, SWT.BORDER);
		filepathText.setEditable(false);
		GridDataFactory.fillDefaults()
				.span(3, 1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(filepathText);
		ISWTObservableValue filenameObservable = WidgetProperties.text(SWT.Modify).observe(filepathText);
		ValueBindingBuilder
				.bind(filenameObservable)
				.to(BeanProperties.value(RestoreSnapshotWizardPageModel.PROPERTY_FILEPATH).observe(pageModel))
				.in(dbc);
		Button browseButton = new Button(parent, SWT.PUSH);
		browseButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseButton);
		browseButton.addSelectionListener(onBrowse());

		// hot-deploy
		Button hotDeployButton = new Button(parent, SWT.CHECK);
		hotDeployButton.setText("Use Hot Deployment");
		GridDataFactory.fillDefaults()
				.span(5, 1).align(SWT.FILL, SWT.CENTER).applyTo(hotDeployButton);
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(hotDeployButton))
				.to(BeanProperties.value(RestoreSnapshotWizardPageModel.PROPERTY_HOT_DEPLOY)
						.observe(pageModel))
				.in(dbc);

		MultiValidator filenameValidator = new FilepathValidator(filenameObservable);
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
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				dialog.setText("Choose your snapshot file");
				dialog.setFilterPath(FileUtils.getParent(pageModel.getFilepath()));
				String filepath = dialog.open();
				if (!StringUtils.isEmpty(filepath)) {
					pageModel.setFilepath(filepath);
				}
			}
		};
	}

	static class FilepathValidator extends MultiValidator {

		private IObservableValue filepathObservable;

		public FilepathValidator(IObservableValue filenameObservable) {
			this.filepathObservable = filenameObservable;
		}

		@Override
		protected IStatus validate() {

			String filepath = (String) filepathObservable.getValue();

			if (StringUtils.isEmpty(filepath)) {
				return ValidationStatus.cancel("Please provide a file that we can restore/deploy.");
			} else {
//				try {
					File snapshotFile = new File(filepath);
					if (!snapshotFile.exists()) {
						return ValidationStatus.error(NLS.bind("File {0} is not existing.", filepath));
					} else if (!snapshotFile.canRead()) {
						return ValidationStatus.error(NLS.bind(
								"File {0} is not readable. Please check your permissions.",
								filepath));
//					} else if (!isTarGz(filepath)) {
//						return ValidationStatus.error(NLS.bind(
//								"File {0} is not a tar gz archive.",
//								filepath));
					}
//				} catch (IOException e) {
//					OpenShiftUIActivator.log(e);
//					return ValidationStatus.error(NLS.bind(
//							"Unknown error accessing file {0}. Please check log for details.", filepath));
//				}
			}
			return ValidationStatus.ok();
		}

//		private boolean isTarGz(String filepath) throws IOException {
//			FileInputStream snapshotFileIn = new FileInputStream(filepath);
//			try {
//				return TarFileUtils.isTarGz(snapshotFileIn);
//			} finally {
//				StreamUtils.close(snapshotFileIn);
//			}
//		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(filepathObservable);
			return targets;
		}
	}

}