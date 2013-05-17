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
import org.eclipse.jface.dialogs.ErrorDialog;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils.IWidgetVisitor;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.NewDomainDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.embed.EmbedCartridgeStrategyAdapter;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.NoSSHKeysWizard;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
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
	private Combo newAppCartridgeCombo;
	private Button enableScalingButton;
	private Combo gearProfilesCombo;
	private Group newAppEmbeddableCartridgesGroup;
	private Button checkAllButton;
	private Button uncheckAllButton;

	// private ModifyListener modifyListener;

	public ApplicationConfigurationWizardPage(IWizard wizard, OpenShiftExpressApplicationWizardModel wizardModel) {
		super("Set up OpenShift Application",
				"Select an existing or create a new OpenShift Application.",
				"Set up OpenShift Application", wizard);
		try {
			this.pageModel = new ApplicationConfigurationWizardPageModel(wizardModel);
		} catch (OpenShiftException e) {
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
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).span(1, 1).grab(false, false)
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
				OpenShiftExpressApplicationWizard wizard = (OpenShiftExpressApplicationWizard) getWizard();
				OpenShiftExpressApplicationWizardModel wizardModel = wizard.getModel();
				final ApplicationSelectionDialog appSelectionDialog =
						new ApplicationSelectionDialog(wizard, wizardModel, null, getShell());
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
								}
								return Status.OK_STATUS;
							}
						};
						try {
							WizardUtils.runInWizard(j, getContainer(), dbc);
						} catch (InvocationTargetException ite) {
						} catch (InterruptedException ie) {
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

		final Label newAppNameLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		newAppNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(newAppNameLabel);

		// application name
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
		final NewApplicationNameValidator newApplicationNameValidator =
				new NewApplicationNameValidator(useExistingAppBtnSelection, applicationNameTextObservable);
		dbc.addValidationStatusProvider(newApplicationNameValidator);
		ControlDecorationSupport.create(
				newApplicationNameValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// application type
		final Label newAppTypeLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		newAppTypeLabel.setText("Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(newAppTypeLabel);
		this.newAppCartridgeCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
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
				.converting(new CartridgeToCartridgesIndex()
				).in(dbc);

		// gear profile
		final Label gearProfileLabel = new Label(newAppConfigurationGroup, SWT.NONE);
		gearProfileLabel.setText("Gear profile:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).applyTo(gearProfileLabel);
		this.gearProfilesCombo = new Combo(newAppConfigurationGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false).applyTo(gearProfilesCombo);
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
		this.enableScalingButton = new Button(newAppConfigurationGroup, SWT.CHECK);
		enableScalingButton.setText("Enable scaling");
		IObservableValue enableScalingModelObservable = BeanProperties.value(
				ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_SCALE).observe(pageModel);
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
		this.newAppEmbeddableCartridgesGroup = new Group(newAppConfigurationGroup, SWT.NONE);
		newAppEmbeddableCartridgesGroup.setText("Embeddable Cartridges");
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).span(3, 1)
				.applyTo(newAppEmbeddableCartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(newAppEmbeddableCartridgesGroup);

		Composite tableContainer = new Composite(newAppEmbeddableCartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(1, 2).hint(400, 250)
				.applyTo(tableContainer);
		this.viewer = createTable(tableContainer);
		dbc.bindSet(
				ViewerProperties.checkedElements(IEmbeddableCartridge.class).observe(viewer),
				BeanProperties.set(
						ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES)
						.observe(pageModel));
		// strategy has to be attached after the binding, so that the binding
		// can still add the checked cartridge and the strategy can correct
		viewer.addCheckStateListener(new EmbedCartridgeStrategyAdapter(pageModel, this));

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

	protected CheckboxTableViewer createTable(Composite tableContainer) {
		Table table =
				new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		table.setLinesVisible(true);
		TableColumnLayout tableLayout = new TableColumnLayout();
		tableContainer.setLayout(tableLayout);
		CheckboxTableViewer viewer = new CheckboxTableViewer(table);
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof IEmbeddableCartridge && e2 instanceof IEmbeddableCartridge) {
					return ((IEmbeddableCartridge) e1).getDisplayName().compareTo(((IEmbeddableCartridge) e2).getDisplayName());
				}
				return super.compare(viewer, e1, e2);
			}
		});

		viewer.setComparer(new EqualityComparer());
		viewer.setContentProvider(new ArrayContentProvider());
		createTableColumn("Embeddable Cartridge", 1, new CellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) cell.getElement();
				cell.setText(cartridge.getDisplayName());
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
			return ((IStandaloneCartridge) fromObject).getDisplayName();
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
	private boolean ensureHasDomain() {
		try {
			final Connection connection = pageModel.getConnection();
			if (connection == null
					|| connection.hasDomain()) {
				return true;
			}
			WizardDialog dialog = new WizardDialog(
					Display.getCurrent().getActiveShell(), new NewDomainDialog(connection));
			dialog.create();
			dialog.setBlockOnOpen(true);
			return dialog.open() != Dialog.OK;
		} catch (OpenShiftException e) {
			Logger.error("Failed to refresh OpenShift account info", e);
			return false;
		}
	}

	private boolean ensureHasSSHKeys() {
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
			WizardUtils.runInWizard(new Job("Loading applications, cartridges and gears...") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
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
						return OpenShiftUIActivator.createErrorStatus("Could not load applications, cartridges and gears", e);
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
				return ValidationStatus.cancel(
						"Please choose the application that you want to import");
			}
			if (!StringUtils.isAlphaNumeric(applicationName)) {
				return ValidationStatus.error(
						"The name may only contain letters and digits.");
			}
			if (pageModel.isExistingApplication(applicationName)) {
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

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this,
				dbc);
	}
}
