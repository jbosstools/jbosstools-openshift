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

import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateListStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
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
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.console.UserDelegate;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils.IWidgetVisitor;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationConfigurationWizardPage extends AbstractOpenShiftWizardPage {

	private CheckboxTableViewer viewer;
	private ApplicationConfigurationWizardPageModel pageModel;
	private Button useExistingAppBtn;
	private Text existingAppNameText;
	private Button browseAppsButton;
	private Group newAppConfigurationGroup;
	private Text newAppNameText;
	private Combo newAppCartridgeCombo;
	private Button enableScalingButton;
	private Combo gearProfilesCombo;
	private Group newAppEmbeddableCartridgesGroup;
	private Button checkAllButton;
	private Button uncheckAllButton;

	public ApplicationConfigurationWizardPage(IWizard wizard, OpenShiftExpressApplicationWizardModel wizardModel) {
		super("Setup OpenShift Application",
				"Select an existing or create a new OpenShift Application.",
				"Setup OpenShift Application", wizard);
		try {
			this.pageModel = new ApplicationConfigurationWizardPageModel(wizardModel);
		} catch (OpenShiftException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			OpenShiftUIActivator.log(status);
			ErrorDialog.openError(getShell(), "Error", "Error initializing application configuration page", status);
		} catch (SocketTimeoutException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			OpenShiftUIActivator.log(status);
			ErrorDialog.openError(getShell(), "Error", "Error initializing application configuration page", status);
		}
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);
		createApplicationSelectionGroup(container, dbc);
		createApplicationConfigurationGroup(container, dbc);
	}

	private Composite createApplicationSelectionGroup(Composite container, DataBindingContext dbc) {
		Composite existingAppSelectionGroup = new Composite(container, SWT.NONE);
		// projectGroup.setText("Project");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(existingAppSelectionGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(existingAppSelectionGroup);

		// existing app checkbox
		useExistingAppBtn = new Button(existingAppSelectionGroup, SWT.CHECK);
		useExistingAppBtn.setText("Use existing application:");
		useExistingAppBtn.setToolTipText("Select an existing application or uncheck to create a new one.");
		useExistingAppBtn.setFocus();
		GridDataFactory.fillDefaults()
				.span(1, 1).align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(useExistingAppBtn);
		IObservableValue useExistingAppObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_USE_EXISTING_APPLICATION).observe(pageModel);
		final IObservableValue useExistingAppBtnSelection = WidgetProperties.selection().observe(useExistingAppBtn);
		dbc.bindValue(useExistingAppBtnSelection, useExistingAppObservable);

		// existing app name
		this.existingAppNameText = new Text(existingAppSelectionGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(existingAppNameText);
		IObservableValue existingAppNameTextObservable = WidgetProperties.text(SWT.Modify).observe(existingAppNameText);
		IObservableValue existingAppNameModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_EXISTING_APPLICATION_NAME).observe(pageModel);
		ValueBindingBuilder.bind(existingAppNameTextObservable).to(existingAppNameModelObservable).in(dbc);
		UIUtils.focusOnSelection(useExistingAppBtn, existingAppNameText);
		createExistingAppNameContentAssist();

		this.browseAppsButton = new Button(existingAppSelectionGroup, SWT.NONE);
		browseAppsButton.setText("Browse...");
		browseAppsButton.addSelectionListener(onBrowseApps(dbc));
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).span(1, 1).grab(false, false)
				.applyTo(browseAppsButton);
		// observe the list of application, get notified once they have been
		IObservableValue existingApplicationsLoaded =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_EXISTING_APPLICATIONS_LOADED)
						.observe(pageModel);
		final ApplicationToSelectNameValidator existingAppValidator =
				new ApplicationToSelectNameValidator(
						useExistingAppBtnSelection, existingAppNameTextObservable, existingApplicationsLoaded);
		dbc.addValidationStatusProvider(existingAppValidator);
		ControlDecorationSupport.create(
				existingAppValidator, SWT.LEFT | SWT.TOP, null, new CustomControlDecorationUpdater(false));
		return existingAppSelectionGroup;
	}

	private void createExistingAppNameContentAssist() {
		ControlDecoration dec = new ControlDecoration(existingAppNameText, SWT.TOP | SWT.LEFT);
		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing an application name.");
		dec.setShowOnlyOnFocus(true);

		AutoCompleteField adapter =
				new AutoCompleteField(existingAppNameText, new TextContentAdapter(), new String[] {});
		adapter.setProposals(pageModel.getApplicationNames());
	}

	private SelectionListener onBrowseApps(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final ApplicationSelectionDialog appSelectionDialog = new ApplicationSelectionDialog(
						(OpenShiftExpressApplicationWizard) getWizard(), null, getShell());
				final int result = appSelectionDialog.open();
				if (result == IDialogConstants.OK_ID) {
					final IApplication selectedApplication = appSelectionDialog.getSelectedApplication();
					if (selectedApplication != null) {
						// This setter may be long-running
						Job j = new Job("Setting Application") {
							protected IStatus run(IProgressMonitor monitor) {
								try {
									pageModel.setExistingApplicationName(selectedApplication.getName());
								} catch (OpenShiftException ex) {
									OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
											"Could not get embedded cartridges for application {0}",
											selectedApplication.getName()), ex));
								} catch (SocketTimeoutException ex) {
									OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
											"Could not get embedded cartridges for application {0}",
											selectedApplication.getName()), ex));
								}
								return Status.OK_STATUS;
							}
						};
						try {
							WizardUtils.runInWizard(j, getContainer(), dbc);
						} catch(InvocationTargetException ite) {
						} catch(InterruptedException ie) {
						}
					}
				}
			}
		};
	}

	private void createApplicationConfigurationGroup(Composite parent, DataBindingContext dbc) {
		this.newAppConfigurationGroup = new Group(parent, SWT.NONE);
		newAppConfigurationGroup.setText("New application");
		GridLayoutFactory.fillDefaults()
				.numColumns(3).margins(6, 6).applyTo(newAppConfigurationGroup);
		GridDataFactory.fillDefaults()
				.grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(newAppConfigurationGroup);

		IObservableValue useExistingApplication = WidgetProperties.selection().observe(useExistingAppBtn);

		useExistingApplication.addValueChangeListener(
				onUseExistingApplication(
						newAppConfigurationGroup, existingAppNameText, browseAppsButton));

		final Label newAppNameLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		newAppNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(newAppNameLabel);
		this.newAppNameText = new Text(newAppConfigurationGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).align(SWT.FILL, SWT.FILL).applyTo(newAppNameText);
		UIUtils.selectAllOnFocus(newAppNameText);
		final IObservableValue applicationNameTextObservable =
				WidgetProperties.text(SWT.Modify).observe(newAppNameText);
		final IObservableValue applicationNameModelObservable =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_NAME).observe(
						pageModel);
		ValueBindingBuilder.bind(applicationNameTextObservable).to(applicationNameModelObservable).in(dbc);

		final Label newAppTypeLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		newAppTypeLabel.setText("Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(newAppTypeLabel);
		this.newAppCartridgeCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(newAppCartridgeCombo);
		fillCartridgesCombo(dbc, newAppCartridgeCombo);
		final ISWTObservableValue selectedCartridgeComboObservable =
				WidgetProperties.selection().observe(newAppCartridgeCombo);
		final IObservableValue selectedCartridgeModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(pageModel);
		ValueBindingBuilder.bind(selectedCartridgeComboObservable)
				.converting(new StringToCartridgeConverter())
				.to(selectedCartridgeModelObservable)
				.converting(new CartridgeToStringConverter())
				.in(dbc);

		final ISWTObservableValue useExistingAppBtnSelection = WidgetProperties.selection().observe(useExistingAppBtn);
		final NewApplicationNameValidator newApplicationNameValidator =
				new NewApplicationNameValidator(useExistingAppBtnSelection, applicationNameTextObservable);
		dbc.addValidationStatusProvider(newApplicationNameValidator);
		ControlDecorationSupport.create(
				newApplicationNameValidator, SWT.LEFT | SWT.TOP, null, new CustomControlDecorationUpdater());
		final NewApplicationTypeValidator newApplicationTypeValidator =
				new NewApplicationTypeValidator(useExistingAppBtnSelection, selectedCartridgeComboObservable);
		dbc.addValidationStatusProvider(newApplicationTypeValidator);
		ControlDecorationSupport.create(newApplicationTypeValidator, SWT.LEFT | SWT.TOP, null,
				new CustomControlDecorationUpdater());

		// gear size
		final Label gearProfileLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		gearProfileLabel.setText("Gear profile:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(gearProfileLabel);
		this.gearProfilesCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(gearProfilesCombo);
		fillGearProfilesCombo(dbc, gearProfilesCombo);
		final ISWTObservableValue gearSizeComboObservable =
				WidgetProperties.selection().observe(gearProfilesCombo);
		final IObservableValue selectedGearProfileModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_GEAR_PROFILE).observe(pageModel);
		ValueBindingBuilder.bind(gearSizeComboObservable)
				.converting(new StringToGearProfileConverter())
				.to(selectedGearProfileModelObservable)
				.converting(new GearProfileToStringConverter())
				.in(dbc);

		// scaling
		this.enableScalingButton = new Button(newAppConfigurationGroup, SWT.CHECK);
		enableScalingButton.setText("Enable scaling");
		IObservableValue enableScalingModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_SCALE).observe(pageModel);
		final IObservableValue enableScalingButtonSelection = WidgetProperties.selection().observe(enableScalingButton);
		ValueBindingBuilder
				.bind(enableScalingButtonSelection).converting(new BooleanToApplicationScaleConverter())
				.to(enableScalingModelObservable).converting(new ApplicationScaleToBooleanConverter())
				.in(dbc);

		// embeddable cartridges
		this.newAppEmbeddableCartridgesGroup = new Group(newAppConfigurationGroup, SWT.NONE);
		newAppEmbeddableCartridgesGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).span(3, 1)
				.applyTo(newAppEmbeddableCartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(newAppEmbeddableCartridgesGroup);

		Composite tableContainer = new Composite(newAppEmbeddableCartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 2).hint(400, 250)
				.applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		dbc.bindSet(ViewerProperties.checkedElements(IEmbeddableCartridge.class).observe(viewer),
				BeanProperties.set(ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES)
						.observe(pageModel));

		this.checkAllButton = new Button(newAppEmbeddableCartridgesGroup, SWT.PUSH);
		checkAllButton.setText("&Select All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).grab(false, false).align(SWT.FILL, SWT.TOP).applyTo(checkAllButton);
		checkAllButton.addSelectionListener(onCheckAll());

		this.uncheckAllButton = new Button(newAppEmbeddableCartridgesGroup, SWT.PUSH);
		uncheckAllButton.setText("&Deselect All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).grab(false, true).align(SWT.FILL, SWT.TOP).applyTo(uncheckAllButton);
		uncheckAllButton.addSelectionListener(onUncheckAll());

		// bottom filler
		Composite spacer = new Composite(newAppConfigurationGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(spacer);
	}

	/**
	 * Trieggered when the user checks "use existing application". It will
	 * enable/disable the application widgets and reset existing values.
	 * 
	 * @param applicationConfigurationGroup
	 * @param applicationNameText
	 * @param applicationBrowseButton
	 * @return
	 */
	private IValueChangeListener onUseExistingApplication(final Group applicationConfigurationGroup,
			final Text applicationNameText, final Button applicationBrowseButton) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				Object newValue = event.diff.getNewValue();
				if (newValue instanceof Boolean) {
					Boolean useExisting = (Boolean) newValue;
					// if (!useExisting) {
					// resetExistingApplication();
					// }
					enableApplicationWidgets(useExisting);
				}
			}
		};
	}

	/**
	 * Enables/disables the given widgets based on the flag to use an existing
	 * app or create a new application.
	 * 
	 * @param useExisting
	 * @param applicationConfigurationGroup
	 * @param applicationNameText
	 * @param applicationBrowseButton
	 */
	private void enableApplicationWidgets(final Boolean useExisting) {
		existingAppNameText.setEnabled(useExisting);
		browseAppsButton.setEnabled(useExisting);

		UIUtils.doForAllChildren(new IWidgetVisitor() {

			@Override
			public void visit(Control control) {
				control.setEnabled(!useExisting);
			}
		}, newAppConfigurationGroup);
	}

	private void fillCartridgesCombo(DataBindingContext dbc, Combo cartridgesCombo) {
		dbc.bindList(WidgetProperties.items().observe(cartridgesCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_CARTRIDGES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new CartridgeToStringConverter()));
	}

	private void fillGearProfilesCombo(DataBindingContext dbc, Combo gearSizesCombo) {
		dbc.bindList(WidgetProperties.items().observe(gearSizesCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_GEAR_PROFILES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new GearProfileToStringConverter()));
	}

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
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
				} catch (SocketTimeoutException e) {
					OpenShiftUIActivator.log("Could not process embeddable cartridges", e);
				}
			}
		};
	}

	private void addJenkinsCartridge(final IEmbeddableCartridge cartridge) throws OpenShiftException,
			SocketTimeoutException {
		if (pageModel.hasApplicationOfType(ICartridge.JENKINS_14)) {
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

			}, getContainer());
		} catch (Exception e) {
			// ignore
		}
	}

	private void addPhpMyAdminCartridge(IEmbeddableCartridge cartridge) throws OpenShiftException {
		if (!viewer.getChecked(IEmbeddableCartridge.MYSQL_51)) {
			if (MessageDialog.openQuestion(getShell(), "Embed mysql cartridge",
					"To embed phpmyadmin, you'd also have to embed mysql.")) {
				pageModel.selectEmbeddedCartridges(IEmbeddableCartridge.MYSQL_51);
				pageModel.selectEmbeddedCartridges(cartridge);
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
		Set<IEmbeddableCartridge> checkedCartridges = pageModel.getSelectedEmbeddableCartridges();
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
				} catch (SocketTimeoutException ex) {
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
				if (viewer != null) {
					viewer.setInput(cartridges);
				}
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

	private final class StringToCartridgeConverter extends Converter {
		private StringToCartridgeConverter() {
			super(String.class, ICartridge.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (fromObject instanceof String) {
				return pageModel.getCartridgeByName((String) fromObject);
			}
			return null;
		}
	}

	private static final class GearProfileToStringConverter extends Converter {
		private GearProfileToStringConverter() {
			super(Object.class, String.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof IGearProfile)) {
				return null;
			}
			return ((IGearProfile) fromObject).getName();
		}
	}

	private final class StringToGearProfileConverter extends Converter {
		private StringToGearProfileConverter() {
			super(String.class, IGearProfile.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (fromObject instanceof String) {
				return pageModel.getGearProfileByName((String) fromObject);
			}
			return null;
		}
	}

	private static final class ApplicationScaleToBooleanConverter extends Converter {
		private ApplicationScaleToBooleanConverter() {
			super(Object.class, Boolean.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof ApplicationScale)) {
				return null;
			}
			switch ((ApplicationScale) fromObject) {
			case SCALE:
				return Boolean.TRUE;
			default:
				return Boolean.FALSE;
			}
		}
	}

	private final class BooleanToApplicationScaleConverter extends Converter {
		private BooleanToApplicationScaleConverter() {
			super(Boolean.class, ApplicationScale.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (fromObject instanceof Boolean) {
				return ((Boolean) fromObject).booleanValue() ? ApplicationScale.SCALE : ApplicationScale.NO_SCALE;
			}
			return null;
		}
	}

	private static class JenkinsApplicationDialog extends InputDialog {

		public JenkinsApplicationDialog(Shell shell) {
			super(
					shell,
					"New Jenkins application",
					"To embed Jenkins into your application, you first have to create a separate Jenkins application. "
							+ "Please provide a name for this new Jenkins application (lower-case letters and digits only):"
					, null, new JenkinsNameValidator());
		}

		private static class JenkinsNameValidator implements IInputValidator {

			@Override
			public String isValid(String input) {
				if (StringUtils.isEmpty(input)) {
					return "You have to provide a name for the jenkins application";
				}

				if (!StringUtils.isAlphaNumeric(input)) {
					return "The name may only contain lower-case letters and digits.";
				}
				return null;
			}
		}
	}

	private void openLogDialog(final IApplication application) {
		getShell().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				new CreationLogDialog(getShell(), application).open();
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
		// This is needed for some strange freezing issues when
		// launching the wizard from the console view. The UI seems to freeze
		new Thread() {
			public void run() {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						loadOpenshiftResources(dbc);
						enableApplicationWidgets(pageModel.isUseExistingApplication());
						createExistingAppNameContentAssist();
						// this is needed because of weird issues with UI not
						// reacting to model changes while wizard runnable is
						// run. We force another update
						dbc.updateModels();
					}
				});
			}
		}.start();
	}

	@Override
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (direction == Direction.BACKWARDS) {
			return;
		}
		try {
			final UserDelegate user = this.pageModel.getUser();
			if (user != null && !user.hasDomain()) {
				IWizard domainDialog = new NewDomainDialog(user);
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), domainDialog);
				dialog.create();
				dialog.setBlockOnOpen(true);
				int result = dialog.open();
				if (result != Dialog.OK) {
					final IWizardContainer container = getWizard().getContainer();
					if (container instanceof WizardDialog) {
						event.doit = false;
						((WizardDialog) container).close();
					}
				}
			}
		} catch (OpenShiftException e) {
			Logger.error("Failed to refresh OpenShift account info", e);
		} catch (SocketTimeoutException e) {
			Logger.error("Failed to refresh OpenShift account info", e);
		}
	}

	protected void loadOpenshiftResources(final DataBindingContext dbc) {
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
						return OpenShiftUIActivator.createErrorStatus("Could not load applications", e);
					}
				}

			}, getContainer(), dbc);

			WizardUtils.runInWizard(new Job("Loading application cartridges...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						pageModel.loadStandaloneCartridges();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus("Could not load application cartridges", e);
					}
				}
			}, getContainer(), dbc);
			WizardUtils.runInWizard(new Job("Loading embeddable cartridges...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						setViewerInput(pageModel.loadEmbeddedCartridges());
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus("Could not load embeddable cartridges", e);
					}
				}
			}, getContainer(), dbc);
			WizardUtils.runInWizard(new Job("Loading gear sizes...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						pageModel.loadGearProfiles();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus("Could not load gear sizes", e);
					}
				}
			}, getContainer(), dbc);
		} catch (Exception ex) {
			// ignore
		}
	}

	class ApplicationToSelectNameValidator extends MultiValidator {

		private final IObservableValue useExistingAppBtnbservable;
		private final IObservableValue existingAppNameTextObservable;
		private final IObservableValue existingApplicationsLoadedObservable;

		public ApplicationToSelectNameValidator(IObservableValue useExistingAppBtnbservable,
				IObservableValue existingAppNameTextObservable, IObservableValue existingApplicationsLoadedObservable) {
			this.useExistingAppBtnbservable = useExistingAppBtnbservable;
			this.existingAppNameTextObservable = existingAppNameTextObservable;
			this.existingApplicationsLoadedObservable = existingApplicationsLoadedObservable;
		}

		@Override
		protected IStatus validate() {
			final boolean useExistingApp = (Boolean) useExistingAppBtnbservable.getValue();
			final String appName = (String) existingAppNameTextObservable.getValue();
			final Boolean existingApplicationsLoaded = (Boolean) existingApplicationsLoadedObservable.getValue();

			if (!useExistingApp) {
				return ValidationStatus.ok();
			}

			if (StringUtils.isEmpty(appName)) {
				return ValidationStatus.cancel("Please select an existing OpenShift application");
			}

			if (existingApplicationsLoaded != null
					&& !existingApplicationsLoaded) {
				return ValidationStatus.cancel("Existing applications are not loaded yet.");
			} else {
				try {
					if (!pageModel.hasApplication(appName)) {
						return ValidationStatus.error(NLS.bind("The application \"{0}\" does not exist.", appName));
					}
				} catch (SocketTimeoutException e) {
					return ValidationStatus.error(NLS.bind("The application \"{0}\" existance could not be verified.",
							appName));
				} catch (OpenShiftException e) {
					return ValidationStatus.error(NLS.bind("The application \"{0}\" existance could not be verified.",
							appName));
				}
			}

			return ValidationStatus.ok();

		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(existingAppNameTextObservable);
			return targets;
		}

	}

	class NewApplicationNameValidator extends MultiValidator {

		private final IObservableValue useExistingAppbservable;
		private final IObservableValue applicationNameObservable;

		public NewApplicationNameValidator(IObservableValue useExistingAppObservable,
				IObservableValue applicationNameObservable) {
			this.useExistingAppbservable = useExistingAppObservable;
			this.applicationNameObservable = applicationNameObservable;
		}

		@Override
		protected IStatus validate() {
			final String applicationName = (String) applicationNameObservable.getValue();
			final boolean useExistingApp = (Boolean) useExistingAppbservable.getValue();
			if (useExistingApp) {
				return ValidationStatus.ok();
			}
			if (applicationName.isEmpty()) {
				return ValidationStatus.cancel(getDescription());
			}
			if (!StringUtils.isAlphaNumeric(applicationName)) {
				return ValidationStatus.error(
						"The name may only contain lower-case letters and digits.");
			}
			if (pageModel.isExistingApplication(applicationName)) {
				return ValidationStatus.error(
						"An application with the same name already exists on OpenShift.");
			}
			if (isExistingProject(applicationName)) {
				return ValidationStatus
						.warning("A project already exists with the same application name. This can cause problems when importing.");
			}
			return ValidationStatus.ok();
		}

		private boolean isExistingProject(String name) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			return project.exists();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(applicationNameObservable);
			return targets;
		}
	}

	class NewApplicationTypeValidator extends MultiValidator {

		private final IObservableValue useExistingAppBtnbservable;
		private final IObservableValue selectedCartridgeObservable;

		public NewApplicationTypeValidator(IObservableValue useExistingAppBtnbservable,
				IObservableValue selectedCartridgeObservable) {
			this.useExistingAppBtnbservable = useExistingAppBtnbservable;
			this.selectedCartridgeObservable = selectedCartridgeObservable;
		}

		@Override
		protected IStatus validate() {
			final boolean useExistingApp = (Boolean) useExistingAppBtnbservable.getValue();
			final String cartridge = (String) selectedCartridgeObservable.getValue();
			if (useExistingApp) {
				return ValidationStatus.ok();
			}
			if (StringUtils.isEmpty(cartridge)) {
				return ValidationStatus.cancel(getDescription());
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(selectedCartridgeObservable);
			return targets;
		}
	}

	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this,
				dbc);
	}

}
