/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.IApplicationPropertiesProvider;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.EmptyStringToNullConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.MultiConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.DialogChildToggleAdapter;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftResourceUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils.IWidgetVisitor;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.ManageDomainsWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.NewDomainWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeStrategyAdapter;
import org.jboss.tools.openshift.express.internal.ui.wizard.environment.EnvironmentVariablesWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.NoSSHKeysWizard;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

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
	private Button checkAllButton;
	private Button uncheckAllButton;

	ApplicationConfigurationWizardPage(IWizard wizard, OpenShiftApplicationWizardModel wizardModel) {
		super("New or existing OpenShift Application", "", "New or existing OpenShift Application, wizard", wizard);
		this.pageModel = new ApplicationConfigurationWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		setWizardPageDescription(pageModel.isUseExistingApplication());

		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);

		createDomainGroup(container, dbc);
		createApplicationSelectionGroup(container, dbc);
		createApplicationConfigurationGroup(container, dbc);
	}

	private void createDomainGroup(Composite container, DataBindingContext dbc) {
		Composite domainGroup = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(domainGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(3).margins(6, 6).applyTo(domainGroup);

		// domain
		final Label domainLabel = new Label(domainGroup, SWT.NONE);
		domainLabel.setText("Domain:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(domainLabel);
		Combo domainCombo = new Combo(domainGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(domainCombo);

		dbc.bindList(WidgetProperties.items().observe(domainCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_DOMAINS).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new DomainToStringConverter()));

		final ISWTObservableValue selectedDomainIndexObservable =
				WidgetProperties.singleSelectionIndex().observe(domainCombo);
		final IObservableValue selectedDomainModelObservable =
				BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_DOMAIN).observe(pageModel);
		ValueBindingBuilder.bind(selectedDomainIndexObservable)
				.converting(new DomainsIndexToDomain())
				.to(selectedDomainModelObservable)
				.converting(new DomainToDomainsIndex())
				.in(dbc);

		IObservableValue domainModelObservable =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_DOMAIN).observe(pageModel);
		DataBindingUtils.addDisposableValueChangeListener(
				onDomainChanged(dbc), domainModelObservable, domainCombo);

		Link manageDomainsLink = new Link(domainGroup, SWT.NONE);
		manageDomainsLink
				.setText("<a>Manage Domains</a>");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(manageDomainsLink);
		manageDomainsLink.addSelectionListener(onManageDomains());
	}

	private IValueChangeListener onDomainChanged(final DataBindingContext dbc) {
		return new IValueChangeListener() {
			
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				Object value = event.getObservableValue().getValue();
				if (!(value instanceof IDomain)) {
					return;
				}
				final IDomain domain = (IDomain) value;
				AbstractDelegatingMonitorJob job = new AbstractDelegatingMonitorJob(NLS.bind("Loading applications for domain {0}...", domain.getId())) {

					@Override
					protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.loadExistingApplications();
							
						if (!pageModel.isUseExistingApplication()) {
							return Status.OK_STATUS;
						}

						String existingApplicationName = pageModel.getExistingApplicationName();
						if (!StringUtils.isEmpty(existingApplicationName)
								&& !domain.hasApplicationByName(existingApplicationName)) {
							pageModel.setExistingApplication(domain);
						}
						return Status.OK_STATUS;
					}
				};

				try {
					WizardUtils.runInWizard(job, getContainer(), dbc);
				} catch (InvocationTargetException e) {
					OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not load applications for domain {0}.",
							domain), e));
				} catch (InterruptedException e) {
					OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
							"Could not load applications for domain {0}.",
							domain), e));
				}
			}
		};
	}

	private Composite createApplicationSelectionGroup(Composite container, DataBindingContext dbc) {
		Group existingAppSelectionGroup = new Group(container, SWT.NONE);
		existingAppSelectionGroup.setText("Existing Application");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(existingAppSelectionGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(existingAppSelectionGroup);

		// existing app checkbox
		useExistingAppBtn = new Button(existingAppSelectionGroup, SWT.CHECK);
		useExistingAppBtn.setText("Use existing application:");
		useExistingAppBtn.setToolTipText("Select an existing application or uncheck to create a new one.");
		useExistingAppBtn.setFocus();
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(false, false).applyTo(useExistingAppBtn);
		IObservableValue useExistingAppObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_USE_EXISTING_APPLICATION).observe(pageModel);
		final IObservableValue useExistingAppBtnSelection = WidgetProperties.selection().observe(useExistingAppBtn);
		dbc.bindValue(useExistingAppBtnSelection, useExistingAppObservable);

		// existing app name
		this.existingAppNameText = new Text(existingAppSelectionGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(existingAppNameText);
		IObservableValue existingAppNameTextObservable = WidgetProperties.text(SWT.Modify).observe(existingAppNameText);
		IObservableValue existingAppNameModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_EXISTING_APPLICATION_NAME).observe(pageModel);
		ValueBindingBuilder
				.bind(existingAppNameTextObservable)
				.to(existingAppNameModelObservable)
				.in(dbc);
		UIUtils.focusOnSelection(useExistingAppBtn, existingAppNameText);
		createExistingAppNameContentAssist(existingAppNameText, pageModel.getApplicationNames());

		// observe the list of application, get notified once they have been
		// loaded
		IObservableValue existingApplicationsLoaded =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_EXISTING_APPLICATIONS_LOADED)
						.observe(pageModel);
		final ApplicationToSelectNameValidator existingAppValidator =
				new ApplicationToSelectNameValidator(
						useExistingAppBtnSelection, existingAppNameTextObservable, existingApplicationsLoaded);
		dbc.addValidationStatusProvider(existingAppValidator);
		ControlDecorationSupport.create(
				existingAppValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		useExistingAppBtnSelection.addValueChangeListener(
				onUseExistingApplication(
						newAppConfigurationGroup, existingAppNameText, browseAppsButton));

		// browse button
		this.browseAppsButton = new Button(existingAppSelectionGroup, SWT.NONE);
		browseAppsButton.setText("Browse...");
		browseAppsButton.addSelectionListener(onBrowseApps(dbc));
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).grab(false, false)
				.applyTo(browseAppsButton);
		return existingAppSelectionGroup;
	}

	private void createExistingAppNameContentAssist(Text existingAppNameText, String[] applicationNames) {
		ControlDecoration dec = new ControlDecoration(existingAppNameText, SWT.TOP | SWT.LEFT);
		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing an application name.");
		dec.setShowOnlyOnFocus(true);

		AutoCompleteField adapter =
				new AutoCompleteField(existingAppNameText, new TextContentAdapter(), new String[] {});
		adapter.setProposals(applicationNames);
	}

	private SelectionListener onBrowseApps(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				OpenShiftApplicationWizard wizard = (OpenShiftApplicationWizard) getWizard();
				OpenShiftApplicationWizardModel wizardModel = wizard.getModel();
				final ApplicationSelectionDialog appSelectionDialog =
						new ApplicationSelectionDialog(wizard, wizardModel, null, getShell());
				if (appSelectionDialog.open() == IDialogConstants.OK_ID) {
					final IApplication selectedApplication = appSelectionDialog.getSelectedApplication();
					if (selectedApplication != null) {
						// This setter may be long-running
						Job j = new AbstractDelegatingMonitorJob("Setting Application...") {
							@Override
							protected IStatus doRun(IProgressMonitor monitor) {
								try {
									pageModel.setExistingApplicationName(selectedApplication.getName());
								} catch (OpenShiftException ex) {
									OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
											"Could not get embedded cartridges for application {0}",
											selectedApplication.getName()), ex));
								}
								return Status.OK_STATUS;
							}
						};
						try {
							WizardUtils.runInWizard(j, getContainer(), dbc);
						} catch (InvocationTargetException ite) {
							OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
									"Could use application {0} as existing application.",
									selectedApplication.getName()), ite));
						} catch (InterruptedException ie) {
							OpenShiftUIActivator.log(OpenShiftUIActivator.createErrorStatus(NLS.bind(
									"Could use application {0} as existing application.",
									selectedApplication.getName()), ie));
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

		// application name
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
		ValueBindingBuilder
				.bind(applicationNameTextObservable).
				to(applicationNameModelObservable)
				.in(dbc);

		final ISWTObservableValue useExistingAppBtnSelection = WidgetProperties.selection().observe(useExistingAppBtn);
		IObservableValue existingApplicationsLoaded =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_EXISTING_APPLICATIONS_LOADED)
						.observe(pageModel);
		final NewApplicationNameValidator newApplicationNameValidator =
				new NewApplicationNameValidator(
						useExistingAppBtnSelection, existingApplicationsLoaded, applicationNameTextObservable);
		dbc.addValidationStatusProvider(newApplicationNameValidator);
		ControlDecorationSupport.create(
				newApplicationNameValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// application type
		final Label newAppTypeLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		newAppTypeLabel.setText("Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(newAppTypeLabel);
		Combo newAppCartridgeCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(2, 1).grab(true, false).applyTo(newAppCartridgeCombo);

		dbc.bindList(WidgetProperties.items().observe(newAppCartridgeCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_CARTRIDGES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new CartridgeToStringConverter()));

		final ISWTObservableValue selectedCartridgeIndexObservable =
				WidgetProperties.singleSelectionIndex().observe(newAppCartridgeCombo);
		final IObservableValue selectedCartridgeModelObservable =
				BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(pageModel);
		ValueBindingBuilder.bind(selectedCartridgeIndexObservable)
				.converting(new CartridgesIndexToCartridge())
				.to(selectedCartridgeModelObservable)
				.converting(new CartridgeToCartridgesIndex())
				.in(dbc);

		// gear profile
		final Label gearProfileLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		gearProfileLabel.setText("Gear profile:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(gearProfileLabel);
		Combo gearProfilesCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(gearProfilesCombo);
		dbc.bindList(WidgetProperties.items().observe(gearProfilesCombo),
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_GEAR_PROFILES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy() {

					/**
					 * Needed to avoid buggy list update strategy in
					 * ListBinding. The bug appears if the model list changes
					 * its ordering and the strategy then tries to apply the
					 * move in the target (widget). It does not apply the
					 * conversion and ends up in a class cast exception when
					 * updating the target (widget) items list.
					 * 
					 * @see https://issues.jboss.org/browse/JBIDE-11954
					 */
					protected boolean useMoveAndReplace() {
						return false;
					}

				}.setConverter(new GearProfileToStringConverter()));

		final ISWTObservableValue selectedGearProfileComboObservable =
				WidgetProperties.selection().observe(gearProfilesCombo);
		final IObservableValue selectedGearProfileModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_GEAR_PROFILE).observe(pageModel);
		ValueBindingBuilder.bind(selectedGearProfileComboObservable)
				.converting(new StringToGearProfileConverter())
				.to(selectedGearProfileModelObservable)
				.converting(new GearProfileToStringConverter())
				.in(dbc);

		// scaling
		Button enableScalingButton = new Button(newAppConfigurationGroup, SWT.CHECK);
		enableScalingButton.setText("Enable scaling");
		IObservableValue enableScalingModelObservable =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_SCALE)
						.observe(pageModel);
		final IObservableValue enableScalingButtonSelection = WidgetProperties.selection().observe(enableScalingButton);
		ValueBindingBuilder
				.bind(enableScalingButtonSelection).converting(new BooleanToApplicationScaleConverter())
				.to(enableScalingModelObservable).converting(new ApplicationScaleToBooleanConverter())
				.in(dbc);

		final NewApplicationTypeValidator newApplicationTypeValidator =
				new NewApplicationTypeValidator(useExistingAppBtnSelection, selectedCartridgeIndexObservable);
		dbc.addValidationStatusProvider(newApplicationTypeValidator);
		ControlDecorationSupport.create(newApplicationTypeValidator, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());

		// embeddable cartridges
		Group newAppEmbeddableCartridgesGroup = new Group(newAppConfigurationGroup, SWT.NONE);
		newAppEmbeddableCartridgesGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults()
				.grab(true, true).align(SWT.FILL, SWT.FILL).span(3, 1).applyTo(newAppEmbeddableCartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(newAppEmbeddableCartridgesGroup);

		Composite tableContainer = new Composite(newAppEmbeddableCartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 2).hint(400, 250).applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(IEmbeddableCartridge.class).observe(viewer),
				BeanProperties.set(
						ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES)
						.observe(pageModel));
		// strategy has to be attached after the binding, so that the binding
		// can still add the checked cartridge and the strategy can correct
		viewer.addCheckStateListener(new EmbedCartridgeStrategyAdapter(pageModel, this,
				new ApplicationFormPropertiesProvider()));

		this.checkAllButton = new Button(newAppEmbeddableCartridgesGroup, SWT.PUSH);
		checkAllButton.setText("&Select All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).grab(false, false).align(SWT.FILL, SWT.TOP).applyTo(checkAllButton);
		checkAllButton.addSelectionListener(onCheckAll());

		this.uncheckAllButton = new Button(newAppEmbeddableCartridgesGroup, SWT.PUSH);
		uncheckAllButton.setText("&Deselect All");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).grab(false, false).align(SWT.FILL, SWT.TOP).applyTo(uncheckAllButton);
		uncheckAllButton.addSelectionListener(onUncheckAll());

		// advanced configurations
		createAdvancedGroup(newAppConfigurationGroup, dbc);
	}

	private void createAdvancedGroup(Composite parent, DataBindingContext dbc) {
		// advanced button
		Button advancedButton = new Button(parent, SWT.NONE);
		advancedButton.setText(" Advanced >> ");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).span(3, 1).applyTo(advancedButton);

		// advanced composite
		Composite advancedComposite = new Composite(parent, SWT.NONE);
		GridData advancedCompositeGridData = GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).span(3, 1).create();
		advancedComposite.setLayoutData(advancedCompositeGridData);
		GridLayoutFactory.fillDefaults().applyTo(advancedComposite);

		// source group
		Group sourceGroup = new Group(advancedComposite, SWT.NONE);
		sourceGroup.setText("Source Code");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(sourceGroup);
		GridLayoutFactory.fillDefaults()
				.numColumns(2).margins(6, 6).applyTo(sourceGroup);

		// use default source checkbox
		Button useDefaultSourceButton = new Button(sourceGroup, SWT.CHECK);
		useDefaultSourceButton.setText("Use default source code");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).span(2, 1).applyTo(useDefaultSourceButton);
		IObservableValue defaultSourceCodeObservable = WidgetProperties.selection().observe(useDefaultSourceButton);
		ValueBindingBuilder
				.bind(defaultSourceCodeObservable)
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_DEFAULT_SOURCECODE).observe(pageModel))
				.in(dbc);

		// source code text
		Label sourceCodeUrlLabel = new Label(sourceGroup, SWT.NONE);
		sourceCodeUrlLabel.setText("Source code:");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).applyTo(sourceCodeUrlLabel);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(sourceCodeUrlLabel))
				.notUpdatingParticipant()
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_DEFAULT_SOURCECODE).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);
		Text sourceUrlText = new Text(sourceGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(sourceUrlText);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(sourceUrlText))
				.notUpdatingParticipant()
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_DEFAULT_SOURCECODE).observe(pageModel))
				.converting(new InvertingBooleanConverter())
				.in(dbc);
		IObservableValue sourcecodeUrlObservable = WidgetProperties.text(SWT.Modify).observe(sourceUrlText);
		ValueBindingBuilder
				.bind(sourcecodeUrlObservable)
				.converting(new MultiConverter(new TrimmingStringConverter(), new EmptyStringToNullConverter()))
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_INITIAL_GITURL).observe(pageModel))
				.in(dbc);

		MultiValidator sourceCodeUrlValidator = new SourceCodeUrlValidator(defaultSourceCodeObservable,
				sourcecodeUrlObservable);
		dbc.addValidationStatusProvider(sourceCodeUrlValidator);
		ControlDecorationSupport.create(
				sourceCodeUrlValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		DialogChildToggleAdapter toggleAdapter = new DialogChildToggleAdapter(advancedComposite, getShell(), false);
		advancedButton.addSelectionListener(onAdvancedClicked(advancedButton, toggleAdapter, sourceUrlText,
				sourceCodeUrlLabel));

		// explanation
		Text sourceCodeExplanationText = new Text(sourceGroup, SWT.WRAP);
		sourceCodeExplanationText
				.setText("Your application will start with an exact copy of the code and configuration "
						+ "provided in this Git repository instead of the default application.");
		sourceCodeExplanationText.setEnabled(false);
		UIUtils.copyBackground(sourceGroup, sourceCodeExplanationText);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, true).span(2, 1).applyTo(sourceCodeExplanationText);

		// environment variables		
		Button environmentVariablesButton = new Button(advancedComposite, SWT.NONE);
		environmentVariablesButton.setText("Environment Variables... ");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).applyTo(environmentVariablesButton);
		environmentVariablesButton.addSelectionListener(onBrowseEnvironmentVariables(dbc));
	}

	protected SelectionListener onManageDomains() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Connection connection = pageModel.getConnection();
				if (connection == null) {
					return;
				}
				ManageDomainsWizard domainWizard =
						new ManageDomainsWizard("Choose domain", "Please choose the domain for your new application"
								, pageModel.getDomain(), connection);
				if (new OkButtonWizardDialog(getShell(), domainWizard).open() == Dialog.OK) {
					pageModel.setDomain(domainWizard.getDomain());
				}
				;
			}
		};
	}

	private SelectionListener onAdvancedClicked(final Button toggleButton, final DialogChildToggleAdapter adapter,
			final Text sourceUrlText, final Label sourceCodeUrlLabel) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!adapter.isVisible()) {
					toggleButton.setText(" << Advanced ");
				} else {
					toggleButton.setText(" Advanced >> ");
				}
				sourceUrlText.setEnabled(!pageModel.isDefaultSourcecode());
				sourceCodeUrlLabel.setEnabled(!pageModel.isDefaultSourcecode());
				adapter.toggle();
			}
		};
	}

	private SelectionListener onBrowseEnvironmentVariables(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				EnvironmentVariablesWizard environmentVariablesWizard = 
						new EnvironmentVariablesWizard(pageModel.getEnvironmentVariables());
				if (new OkButtonWizardDialog(getShell(), environmentVariablesWizard).open() == Dialog.OK) {
					pageModel.setEnvironmentVariables(environmentVariablesWizard.getEnvironmentVariables());
				}
			}
		};
	}
	
	/**
	 * Triggered when the user checks "use existing application". It will
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
					setWizardPageDescription(useExisting);
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

	private CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IEmbeddableCartridge
						&& e2 instanceof IEmbeddableCartridge) {
					return ((IEmbeddableCartridge) e1).getDisplayName().compareTo((
							(IEmbeddableCartridge) e2).getDisplayName());
				}
				return super.compare(viewer, e1, e2);
			}
		});

		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());
		createTableColumn("Name", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) cell.getElement();
				cell.setText(OpenShiftResourceUtils.toString(cartridge));
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

	private SelectionListener onCheckAll() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// viewer.setAllChecked(true);
				// try {
				// addJenkinsCartridge(IEmbeddableCartridge.JENKINS_14);
				// } catch (OpenShiftException ex) {
				// OpenShiftUIActivator.log("Could not select jenkins cartridge",
				// ex);
				// } catch (SocketTimeoutException ex) {
				// OpenShiftUIActivator.log("Could not select jenkins cartridge",
				// ex);
				// }
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
			if (!(fromObject instanceof IStandaloneCartridge)) {
				return null;
			}
			return OpenShiftResourceUtils.toString((IStandaloneCartridge) fromObject);
		}
	}

	private static final class DomainToStringConverter extends Converter {
		private DomainToStringConverter() {
			super(Object.class, String.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof IDomain)) {
				return null;
			}
			return OpenShiftResourceUtils.toString((IDomain) fromObject);
		}
	}

	private final class DomainsIndexToDomain extends Converter {

		public DomainsIndexToDomain() {
			super(Integer.class, IDomain.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof Integer)) {
				return null;
			}

			int index = ((Integer) fromObject).intValue();
			List<IDomain> domains = pageModel.getDomains();
			if (domains == null
					|| index >= domains.size()
					|| index == -1) {
				return null;
			}
			return domains.get(index);
		}
	}

	private final class DomainToDomainsIndex extends Converter {

		public DomainToDomainsIndex() {
			super(IDomain.class, Integer.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof IDomain)) {
				return null;
			}

			IDomain domain = ((IDomain) fromObject);
			List<IDomain> domains = pageModel.getDomains();
			if (domains == null
					|| domains.isEmpty()) {
				return -1;
			}
			return domains.indexOf(domain);
		}
	}

	private final class CartridgesIndexToCartridge extends Converter {

		public CartridgesIndexToCartridge() {
			super(Integer.class, IStandaloneCartridge.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof Integer)) {
				return null;
			}

			int index = ((Integer) fromObject).intValue();
			List<IStandaloneCartridge> cartridges = pageModel.getCartridges();
			if (index >= cartridges.size()
					|| index == -1) {
				return null;
			}
			return cartridges.get(index);
		}
	}

	private final class CartridgeToCartridgesIndex extends Converter {

		public CartridgeToCartridgesIndex() {
			super(IStandaloneCartridge.class, Integer.class);
		}

		@Override
		public Object convert(Object fromObject) {
			if (!(fromObject instanceof IStandaloneCartridge)) {
				return null;
			}

			IStandaloneCartridge cartridge = ((IStandaloneCartridge) fromObject);
			List<IStandaloneCartridge> cartridges = pageModel.getCartridges();
			return cartridges.indexOf(cartridge);
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
		if (!ensureHasDomain()
					|| !ensureHasSSHKeys()) {
				dispose();
				org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils.close(getWizard());
			return;
		}

		try {
			pageModel.reset();
			// needs to be done before loading resources, otherwise:
			// dbc.updateModels() will be called and old data could be
			// restored
			loadOpenshiftResources(dbc);
			dbc.updateTargets();
			enableApplicationWidgets(pageModel.isUseExistingApplication());
			createExistingAppNameContentAssist(existingAppNameText, pageModel.getApplicationNames());
			this.newAppNameText.setFocus();
		} catch (OpenShiftException e) {
			Logger.error("Failed to reset page fields", e);
		}
	}
	
	/**
	 * Checks that the user has a domain, opens the creation dialog in case he
	 * hasn't, closes the wizard if the user does not create a domain (required
	 * for any application creation). Otherwise, returns true.
	 */
	protected boolean ensureHasDomain() {
		try {
			final Connection connection = pageModel.getConnection();
			if (connection == null
					|| connection.hasDomain()) {
				return true;
			}
			NewDomainWizard domainWizard = new NewDomainWizard(connection); 
			WizardDialog dialog = new WizardDialog(
					Display.getCurrent().getActiveShell(), domainWizard);
			dialog.create();
			dialog.setBlockOnOpen(true);
			return dialog.open() == Dialog.OK;
		} catch (OpenShiftException e) {
			Logger.error("Failed to refresh OpenShift account info", e);
			return false;
		}
	}

	protected boolean ensureHasSSHKeys() {
		try {
			final Connection connection = pageModel.getConnection();
			if (connection == null
					|| connection.hasSSHKeys()) {
				return true;
			}
			WizardDialog dialog = new WizardDialog(
					Display.getCurrent().getActiveShell(), new NoSSHKeysWizard(connection));
			dialog.create();
			dialog.setBlockOnOpen(true);
			return dialog.open() == Dialog.OK;
		} catch (OpenShiftException e) {
			Logger.error("Failed to refresh OpenShift account info", e);
			return false;
		}
	}
	
	protected void loadOpenshiftResources(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new AbstractDelegatingMonitorJob("Loading applications, cartridges and gears...") {

				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						monitor.setTaskName("Loading domains...");
						pageModel.loadDomains();
						monitor.setTaskName("Loading existing applications...");
						pageModel.loadExistingApplications();
						monitor.setTaskName("Loading application cartridges...");
						pageModel.loadStandaloneCartridges();
						monitor.setTaskName("Loading embeddable cartridges...");
						setViewerInput(pageModel.loadEmbeddedCartridges());
						monitor.setTaskName("Loading gear profiles...");
						pageModel.loadGearProfiles();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						return Status.OK_STATUS;
					} catch (Exception e) {
						return OpenShiftUIActivator.createErrorStatus(
								"Could not load applications, cartridges and gears", e);
					}
				}
			}, getContainer(), dbc);
		} catch (Exception ex) {
			// ignore
		}
	}

	private void setWizardPageDescription(boolean useExisting) {
		if (useExisting) {
			setDescription("Import an existing OpenShift Application.");
		} else {
			setDescription("Create a new OpenShift Application.");
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

			if (!StringUtils.isAlphaNumeric(appName)) {
				return ValidationStatus.error(
						"The name may only contain letters and digits.");
			}

			if (existingApplicationsLoaded != null
					&& !existingApplicationsLoaded) {
				return ValidationStatus.cancel("Existing applications are not loaded yet.");
			} else {
				try {
					if (!pageModel.hasApplication(appName)) {
						return ValidationStatus.error(NLS.bind("The application \"{0}\" does not exist.", appName));
					}
				} catch (OpenShiftException e) {
					return ValidationStatus.error(NLS.bind("The application \"{0}\" existance could not be verified.",
							appName));
				}
			}

			return ValidationStatus.ok();

		}

		@Override
		public IObservableList getTargets() {
			IObservableList targets = new WritableList();
			targets.add(existingAppNameTextObservable);
			return targets;
		}
	}

	class NewApplicationNameValidator extends MultiValidator {

		private final IObservableValue useExistingAppObservable;
		private final IObservableValue applicationNameObservable;
		private IObservableValue existingApplicationsLoadedObservable;

		public NewApplicationNameValidator(IObservableValue useExistingAppObservable,
				IObservableValue existingApplicationsLoadedObservable, IObservableValue applicationNameObservable) {
			this.useExistingAppObservable = useExistingAppObservable;
			this.existingApplicationsLoadedObservable = existingApplicationsLoadedObservable; 
			this.applicationNameObservable = applicationNameObservable;
		}

		@Override
		protected IStatus validate() {
			final String applicationName = (String) applicationNameObservable.getValue();
			final boolean useExistingApp = (Boolean) useExistingAppObservable.getValue();
			final Boolean existingApplicationsLoaded = (Boolean) existingApplicationsLoadedObservable.getValue();
			if (useExistingApp) {
				return ValidationStatus.ok();
			}
			if (applicationName.isEmpty()) {
				return ValidationStatus.cancel(
						"Please choose a name for your new application.");
			}
			if (!StringUtils.isAlphaNumeric(applicationName)) {
				return ValidationStatus.error(
						"The name may only contain letters and digits.");
			}
			if (existingApplicationsLoaded != null
					&& !existingApplicationsLoaded) {
				return ValidationStatus.cancel("Existing applications are not loaded yet.");
			} else if (pageModel.isExistingApplication(applicationName)) {
				return ValidationStatus.error(
						"An application with the same name already exists on OpenShift.");
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(applicationNameObservable);
			return targets;
		}

	}

	/**
	 * Validates that the new application type is selected
	 * 
	 * @author Xavier Coulon
	 * 
	 */
	class NewApplicationTypeValidator extends MultiValidator {

		private final IObservableValue useExistingAppBtnObservable;
		private final IObservableValue selectedApplicationTypeObservable;

		public NewApplicationTypeValidator(IObservableValue useExistingAppBtnbservable,
				IObservableValue selectedApplicationTypeObservable) {
			this.useExistingAppBtnObservable = useExistingAppBtnbservable;
			this.selectedApplicationTypeObservable = selectedApplicationTypeObservable;
		}

		@Override
		protected IStatus validate() {
			final boolean useExistingApp = (Boolean) useExistingAppBtnObservable.getValue();
			final Integer selectedCartridgeIndex = (Integer) selectedApplicationTypeObservable.getValue();
			if (useExistingApp) {
				return ValidationStatus.ok();
			}
			if (selectedCartridgeIndex == null
					|| selectedCartridgeIndex == -1) {
				return ValidationStatus.cancel(getDescription());
			}
			return ValidationStatus.ok();
		}
	}

	static class SourceCodeUrlValidator extends MultiValidator {

		private static final Pattern PROTO_GITURI_PATTERN =
				Pattern.compile("(\\w+://)(.+@)*([\\w\\d\\.]+)(:[\\d]+){0,1}/*(.*)");
		private IObservableValue defaultSourcecodeObservable;
		private IObservableValue sourcecodeUrlObservable;

		public SourceCodeUrlValidator(IObservableValue defaultSourcecodeObservable,
				IObservableValue sourcecodeUrlObservable) {
			this.defaultSourcecodeObservable = defaultSourcecodeObservable;
			this.sourcecodeUrlObservable = sourcecodeUrlObservable;
		}

		@Override
		protected IStatus validate() {
			if (Boolean.TRUE.equals(defaultSourcecodeObservable.getValue())) {
				return ValidationStatus.ok();
			}

			Object value = sourcecodeUrlObservable.getValue();
			if (value instanceof String) {
				String gitUri = StringUtils.trim((String) value);
				if (StringUtils.isEmpty(gitUri)) {
					return ValidationStatus.cancel("Please provide a git url for your source code");
				}
				if (PROTO_GITURI_PATTERN.matcher(gitUri).matches()) {
					return ValidationStatus.ok();
				}
			}
			return ValidationStatus.error("You have to provide a valid git url.");
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(sourcecodeUrlObservable);
			return targets;
		}
	}

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this,
				dbc);
	}

	private class ApplicationFormPropertiesProvider implements IApplicationPropertiesProvider {

		@Override
		public ApplicationScale getApplicationScale() {
			return pageModel.getScale();
		}

		@Override
		public IStandaloneCartridge getCartridge() {
			return pageModel.getSelectedCartridge();
		}

		@Override
		public String getName() {
			return pageModel.getApplicationName();
		}

	}

}
