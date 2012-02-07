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

import java.util.Collection;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateListStrategy;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.CreationLogDialog.LogEntry;

import com.openshift.express.client.Cartridge;
import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.IEmbeddableCartridge;
import com.openshift.express.client.NotFoundOpenShiftException;
import com.openshift.express.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationConfigurationWizardPage extends AbstractOpenShiftWizardPage {

	private CheckboxTableViewer viewer;
	private ApplicationConfigurationWizardPageModel pageModel;

	public ApplicationConfigurationWizardPage(IWizard wizard, CreateNewApplicationWizardModel wizardModel) {
		super("Application Configuration", "Configure the application you want to create.",
				"Application configuration", wizard);
		this.pageModel = new ApplicationConfigurationWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);
		createApplicationConfigurationGroup(container, dbc);
	}

	private void createApplicationConfigurationGroup(Composite parent, DataBindingContext dbc) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);

		Label applicationNameLabel = new Label(container, SWT.NONE);
		applicationNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(applicationNameLabel);
		Text applicationNameText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(applicationNameText);
		UIUtils.selectAllOnFocus(applicationNameText);
		final IObservableValue applicationNameModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_NAME).observe(pageModel);
		final ISWTObservableValue applicationNameTextObservable = WidgetProperties.text(SWT.Modify).observe(
				applicationNameText);
		dbc.bindValue(applicationNameTextObservable, applicationNameModelObservable);
		final IObservableValue applicationNameStatusObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_NAME_STATUS).observe(pageModel);

		Label applicationTypeLabel = new Label(container, SWT.NONE);
		applicationTypeLabel.setText("Type:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(applicationTypeLabel);
		Combo cartridgesCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(cartridgesCombo);
		fillCartridgesCombo(dbc, cartridgesCombo);
		final ISWTObservableValue cartridgesComboObservable = WidgetProperties.selection().observe(cartridgesCombo);
		final IObservableValue selectedCartridgeModelObservable = BeanProperties.value(
				NewApplicationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(pageModel);
		dbc.bindValue(cartridgesComboObservable, selectedCartridgeModelObservable,
				new UpdateValueStrategy().setConverter(new StringToCartridgeConverter()),
				new UpdateValueStrategy().setConverter(new CartridgeToStringConverter()));
		final ApplicationInputValidator applicationInputValidator =
				new ApplicationInputValidator(applicationNameTextObservable, cartridgesComboObservable);
		dbc.addValidationStatusProvider(applicationInputValidator);
		final ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(
				applicationNameStatusObservable, applicationNameTextObservable);
		dbc.addValidationStatusProvider(applicationNameValidator);
		ControlDecorationSupport.create(applicationNameValidator, SWT.LEFT | SWT.TOP);

		// embeddable cartridges
		Group cartridgesGroup = new Group(container, SWT.NONE);
		cartridgesGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).span(2, 1).applyTo(cartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(cartridgesGroup);

		Composite tableContainer = new Composite(cartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 2).hint(SWT.DEFAULT, 250).applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		Button checkAllButton = new Button(cartridgesGroup, SWT.PUSH);
		checkAllButton.setText("&Select All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).grab(false, false).align(SWT.FILL, SWT.TOP).applyTo(checkAllButton);
		checkAllButton.addSelectionListener(onCheckAll());

		Button uncheckAllButton = new Button(cartridgesGroup, SWT.PUSH);
		uncheckAllButton.setText("&Deselect All");
		GridDataFactory.fillDefaults().
				hint(110, SWT.DEFAULT).grab(false, true).align(SWT.FILL, SWT.TOP).applyTo(uncheckAllButton);
		uncheckAllButton.addSelectionListener(onUncheckAll());
		// bottom filler
		Composite spacer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(spacer);

	}

	private void fillCartridgesCombo(DataBindingContext dbc, Combo cartridgesCombo) {
		dbc.bindList(WidgetProperties.items().observe(cartridgesCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_CARTRIDGES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new CartridgeToStringConverter()));
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.addCheckStateListener(onCartridgeChecked());
		createTableColumn("Embeddable Cartridge", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) cell.getElement();
				cell.setText(cartridge.getName());
			}
		}, viewer, tableLayout);
		return viewer;
	}

	private void createTableColumn(String name, int weight, CellLabelProvider cellLabelProvider, TableViewer viewer,
			TableColumnLayout layout) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
		column.getColumn().setText(name);
		column.setLabelProvider(cellLabelProvider);
		layout.setColumnData(column.getColumn(), new ColumnWeightData(weight, true));
	}

	private ICheckStateListener onCartridgeChecked() {
		return new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				try {
					IEmbeddableCartridge cartridge = (IEmbeddableCartridge) event.getElement();
					if (event.getChecked()) {
						if (IEmbeddableCartridge.PHPMYADMIN_34.equals(cartridge)) {
							addPhpMyAdminCartridge(cartridge);
						} else if (IEmbeddableCartridge.JENKINS_14.equals(cartridge)) {
							addJenkinsCartridge(cartridge);
						} else {
							addCartridge(cartridge);
						}
					} else {
						if (IEmbeddableCartridge.MYSQL_51.equals(cartridge)) {
							removeMySQLCartridge(cartridge);
						} else {
							removeCartridge(cartridge);
						}
					}
				} catch (OpenShiftException e) {
					OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
				}
			}
		};
	}

	private void addJenkinsCartridge(final IEmbeddableCartridge cartridge) throws OpenShiftException {
		if (pageModel.hasApplication(ICartridge.JENKINS_14)) {
			pageModel.getSelectedEmbeddableCartridges().add(cartridge);
		} else {
			final JenkinsApplicationDialog dialog = new JenkinsApplicationDialog(getShell());
			if (dialog.open() == Dialog.OK) {
				createJenkinsApplication(cartridge, dialog.getValue());
			} else {
				viewer.setChecked(cartridge, false);
			}
		}
	}

	private void createJenkinsApplication(final IEmbeddableCartridge cartridge, final String name) {
		try {
			WizardUtils.runInWizard(new Job(NLS.bind("Creating jenkins application \"{0}\"...", name)) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						IApplication jenkinsApplication = pageModel.createJenkinsApplication(name, monitor);
						pageModel.getSelectedEmbeddableCartridges().add(cartridge);

						openLogDialog(jenkinsApplication);

						return Status.OK_STATUS;
					} catch (Exception e) {
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								viewer.setChecked(cartridge, false);
							}
						});
						return OpenShiftUIActivator.createErrorStatus("Could not load embeddable cartridges", e);
					}
				}

			}, getContainer(), getDataBindingContext());
		} catch (Exception e) {
			// ignore
		}
	}

	private void addPhpMyAdminCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		if (!viewer.getChecked(IEmbeddableCartridge.MYSQL_51)) {
			if (MessageDialog.openQuestion(getShell(), "Embed mysql cartridge",
					"To embed phpmyadmin, you'd also have to embed mysql.")) {
				pageModel.getSelectedEmbeddableCartridges().add(IEmbeddableCartridge.MYSQL_51);
				pageModel.getSelectedEmbeddableCartridges().add(cartridge);
				viewer.setChecked(IEmbeddableCartridge.MYSQL_51, true);
			} else {
				viewer.setChecked(cartridge, false);
			}
		} else {
			pageModel.getSelectedEmbeddableCartridges().add(cartridge);
		}
	}

	private void addCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		pageModel.getSelectedEmbeddableCartridges().add(cartridge);
	}

	private void removeMySQLCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		List<IEmbeddableCartridge> checkedCartridges = pageModel.getSelectedEmbeddableCartridges();
		if (viewer.getChecked(IEmbeddableCartridge.PHPMYADMIN_34)) {
			if (MessageDialog.openQuestion(getShell(), "Remove phpmyadmin cartridge",
					"If you remove the mysql cartridge, you'd also have to remove phpmyadmin.")) {
				checkedCartridges.remove(IEmbeddableCartridge.PHPMYADMIN_34);
				checkedCartridges.remove(cartridge);
				viewer.setChecked(IEmbeddableCartridge.PHPMYADMIN_34, false);
			} else {
				viewer.setChecked(cartridge, true);
			}
		} else {
			checkedCartridges.remove(cartridge);
		}
	}

	private void removeCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		pageModel.getSelectedEmbeddableCartridges().remove(cartridge);
	}

	private SelectionListener onCheckAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(true);
				try {
					addJenkinsCartridge(IEmbeddableCartridge.JENKINS_14);
				} catch (OpenShiftException ex) {
					OpenShiftUIActivator.log("Could not select jenkins cartridge", ex);
				}
			}

		};
	}

	private SelectionListener onUncheckAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.setAllChecked(false);
			}

		};
	}

	private void setViewerInput(final Collection<IEmbeddableCartridge> cartridges) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.setInput(cartridges);
			}
		});
	}

	private static final class CartridgeToStringConverter extends Converter {
		private CartridgeToStringConverter() {
			super(Object.class, String.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof ICartridge)) {
				return null;
			}
			return ((ICartridge) fromObject).getName();
		}
	}

	private static final class StringToCartridgeConverter extends Converter {
		private StringToCartridgeConverter() {
			super(String.class, ICartridge.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (fromObject instanceof String && ((String) fromObject).length() > 0) {
				return new Cartridge(((String) fromObject));
			}
			return null;
		}
	}

	private static class JenkinsApplicationDialog extends InputDialog {

		public JenkinsApplicationDialog(Shell shell) {
			super(shell, "New Jenkins application",
					"To embed jenkins into your application, you'd first have to create a jenkins application.\n"
							+ "Please provide a name for your jenkins application:", null, new JenkinsNameValidator());
		}

		private static class JenkinsNameValidator implements IInputValidator {

			@Override
			public String isValid(String input) {
				if (StringUtils.isEmpty(input)) {
					return "You have to provide a name for the jenkins application";
				}
				return null;
			}
		}
	}

	private void openLogDialog(final IApplication application) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), new LogEntry(application.getName(), application.getCreationLog()))
						.open();
			}
		});
	}

	/**
	 * Viewer element comparer based on #equals(). The default implementation in
	 * CheckboxTableViewer compares elements based on instance identity.
	 * <p>
	 * We need this since the available cartridges (item listed in the viewer)
	 * are not the same instance as the ones in the embedded application (items
	 * to check in the viewer).
	 */
	private static class EqualityComparer implements IElementComparer {

		@Override
		public boolean equals(Object thisObject, Object thatObject) {
			if (thisObject == null) {
				return thatObject != null;
			}

			if (thatObject == null) {
				return false;
			}

			return thisObject.equals(thatObject);
		}

		@Override
		public int hashCode(Object element) {
			return element.hashCode();
		}

	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading existing applications...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						pageModel.loadExistingApplications();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not load applications",
								e);
					}
				}

			}, getContainer(), getDataBindingContext());

			WizardUtils.runInWizard(new Job("Loading application cartridges...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						pageModel.loadCartridges();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Could not load application cartridges", e);
					}
				}
			}, getContainer(), getDataBindingContext());
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setViewerInput(pageModel.loadEmbeddableCartridges());
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID,
								"Could not load embeddable cartridges", e);
					}
				}
			}, getContainer(), getDataBindingContext());
		} catch (Exception ex) {
			// ignore
		}
	}

	class ApplicationNameValidator extends MultiValidator {

		private final IObservableValue applicationNameStatusObservable;

		private final ISWTObservableValue applicationNameTextObservable;

		public ApplicationNameValidator(IObservableValue applicationNameStatusObservable,
				ISWTObservableValue applicationNameTextObservable) {
			this.applicationNameStatusObservable = applicationNameStatusObservable;
			this.applicationNameTextObservable = applicationNameTextObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus applicationNameStatus = (IStatus) applicationNameStatusObservable.getValue();
			if (applicationNameStatus != null) {
				return applicationNameStatus;
			}
			return ValidationStatus.ok();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.databinding.validation.MultiValidator#getTargets()
		 */
		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(applicationNameTextObservable);
			return targets;
		}

	}

	class ApplicationInputValidator extends MultiValidator {

		private final ISWTObservableValue applicationNameTextObservable, cartridgesComboObservable;

		public ApplicationInputValidator(ISWTObservableValue applicationNameTextObservable,
				ISWTObservableValue cartridgesComboObservable) {
			this.applicationNameTextObservable = applicationNameTextObservable;
			this.cartridgesComboObservable = cartridgesComboObservable;
		}

		@Override
		protected IStatus validate() {
			final String applicationName = (String) applicationNameTextObservable.getValue();
			final String applicationType = (String) cartridgesComboObservable.getValue();
			if (applicationName == null || applicationName.isEmpty() || applicationType == null
					|| applicationType.isEmpty()) {
				return ValidationStatus
						.cancel("Please enter a name and select a type for the OpenShift application you want to create.");
			}
			return ValidationStatus.ok();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.databinding.validation.MultiValidator#getTargets()
		 */
		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(applicationNameTextObservable);
			return targets;
		}

	}
}
