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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableSetContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy;
import org.jboss.tools.openshift.express.internal.core.EmbedCartridgeStrategy.EmbeddableCartridgeDiff;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.EmbeddableCartridgeToStringConverter;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.EmptyStringToNullConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.MultiConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.express.internal.ui.explorer.AbstractLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.utils.DialogChildToggleAdapter;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.EmbeddableCartridgeViewerSorter;
import org.jboss.tools.openshift.express.internal.ui.viewer.EqualityComparer;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.ManageDomainsWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.environment.NewEnvironmentVariablesWizard;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationConfigurationWizardPage extends AbstractOpenShiftWizardPage {

	private ApplicationConfigurationWizardPageModel pageModel;
	private Text applicationNameText;
	private OpenShiftApplicationWizardModel wizardModel;

	ApplicationConfigurationWizardPage(IWizard wizard, OpenShiftApplicationWizardModel wizardModel) {
		super("New or existing OpenShift Application", "", "New or existing OpenShift Application, wizard", wizard);
		setDescription("Create a new OpenShift Application.");
		this.wizardModel = wizardModel;
		this.pageModel = new ApplicationConfigurationWizardPageModel(wizardModel);
		setupWizardModelListeners(wizardModel);
	}

	private void setupWizardModelListeners(OpenShiftApplicationWizardModel wizardModel) {
		wizardModel.addPropertyChangeListener(IOpenShiftApplicationWizardModel.PROP_CONNECTION,
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent event) {
						pageModel.setResourcesLoaded(false);
					}
				});
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridDataFactory.fillDefaults()
				.grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(container);
		GridLayoutFactory.fillDefaults()
				.margins(10, 10).numColumns(3).applyTo(container);

		createDomainControls(container, dbc);
		createApplicationControls(container, dbc);
	}

	private void createDomainControls(Composite parent, DataBindingContext dbc) {
		// domain
		final Label domainLabel = new Label(parent, SWT.NONE);
		domainLabel.setText("Domain:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(domainLabel);
		ComboViewer domainViewer = new ComboViewer(parent);
		domainViewer.setContentProvider(new ObservableListContentProvider());
		domainViewer.setLabelProvider(new AbstractLabelProvider() {

			@Override
			public String getText(Object element) {
				return OpenShiftResourceUtils.toString((IDomain) element);			
			}
		});
		domainViewer.setInput(
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_DOMAINS).observe(pageModel));
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(domainViewer.getControl());		

		IObservableValue selectedDomainObservable = ViewerProperties.singlePostSelection().observe(domainViewer);
		ValueBindingBuilder
			.bind(selectedDomainObservable)
			.notUpdating(BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_DOMAIN).observe(pageModel))
			.validatingAfterGet(new IValidator() {
				
				@Override
				public IStatus validate(Object value) {
					if (!(value instanceof IDomain)) {
						return ValidationStatus.error("Please choose a domain.");
					}
					return ValidationStatus.ok();
				}
			})
			.in(dbc);
		
		selectedDomainObservable.addValueChangeListener(onDomainChanged(dbc));
		
		// manage domain
		Link manageDomainsLink = new Link(parent, SWT.NONE);
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
				if (pageModel.isCurrentDomain(domain)) {
					return;
				}
				
				try {
					WizardUtils.runInWizard(new AbstractDelegatingMonitorJob(NLS.bind("Loading applications for domain {0}...", domain.getId())) {

						@Override
						protected IStatus doRun(IProgressMonitor monitor) {
							pageModel.setDomain(domain);
							pageModel.loadExistingApplications();
							return Status.OK_STATUS;
						}
					}, getContainer(), dbc);
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

	private void createApplicationControls(Composite parent, DataBindingContext dbc) {
		// application name
		final Label newAppNameLabel = new Label(parent, SWT.NONE);
		newAppNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(newAppNameLabel);

		this.applicationNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.grab(true, false).span(2, 1).align(SWT.FILL, SWT.FILL).applyTo(applicationNameText);
		UIUtils.selectAllOnFocus(applicationNameText);
		final IObservableValue applicationNameTextObservable =
				WidgetProperties.text(SWT.Modify).observe(applicationNameText);
		final IObservableValue applicationNameModelObservable =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_NAME).observe(pageModel);
		ValueBindingBuilder
				.bind(applicationNameTextObservable).
				to(applicationNameModelObservable)
				.in(dbc);

		final NewApplicationNameValidator newApplicationNameValidator =
				new NewApplicationNameValidator(
						BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_USE_EXISTING_APPLICATION).observe(pageModel),
						BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_RESOURCES_LOADED).observe(pageModel), 
						applicationNameTextObservable);
		dbc.addValidationStatusProvider(newApplicationNameValidator);
		ControlDecorationSupport.create(
				newApplicationNameValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		// application type
		final Label applicationTypeLabel = new Label(parent, SWT.NONE);
		applicationTypeLabel.setText("Type:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(applicationTypeLabel);

		Label selectedApplicationTemplateLabel = new Label(parent, SWT.None);
		GridDataFactory.fillDefaults()
				.span(2,1).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(selectedApplicationTemplateLabel);
		IObservableValue selectedApplicationTemplate =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_APPLICATION_TEMPLATE).observe(pageModel);
		ValueBindingBuilder
				.bind(BeanProperties.value(
						IApplicationTemplate.PROPERTY_NAME).observeDetail(selectedApplicationTemplate))
						.converting(new Converter(String.class, String.class) {

							@Override
							public Object convert(Object fromObject) {
								if (!(fromObject instanceof String)) {
									return fromObject;
								}
								return StringUtils.shorten((String) fromObject, 80) ;
							}
							
						})
				.to(WidgetProperties.text().observe(selectedApplicationTemplateLabel))
				.notUpdatingParticipant()
				.in(dbc);
						
		// gear profile
		final Label gearProfileLabel = new Label(parent, SWT.NONE);
		gearProfileLabel.setText("Gear profile:");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).applyTo(gearProfileLabel);

		ComboViewer gearViewer = new ComboViewer(parent);
		gearViewer.setContentProvider(new ObservableListContentProvider());
		gearViewer.setLabelProvider(new AbstractLabelProvider() {

			@Override
			public String getText(Object element) {
				return OpenShiftResourceUtils.toString((IGearProfile) element);			
			}
		});
		gearViewer.setInput(
				BeanProperties.list(ApplicationConfigurationWizardPageModel.PROPERTY_GEAR_PROFILES)
						.observe(pageModel));
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(gearViewer.getControl());

		// scaling
		Button enableScalingButton = new Button(parent, SWT.CHECK);
		enableScalingButton.setText("Enable scaling");
		ValueBindingBuilder
				.bind(WidgetProperties.selection().observe(enableScalingButton))
				.converting(new BooleanToApplicationScaleConverter())
				.to(BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_APPLICATION_SCALE)
						.observe(pageModel))
				.converting(new ApplicationScaleToBooleanConverter())
				.in(dbc);

		// embeddable cartridges
		Group embeddableCartridgesGroup = new Group(parent, SWT.NONE);
		embeddableCartridgesGroup.setText("Embedded Cartridges");
		GridDataFactory.fillDefaults()
				.grab(true, true).align(SWT.FILL, SWT.FILL).span(3, 1).applyTo(embeddableCartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(embeddableCartridgesGroup);

		Composite tableContainer = new Composite(embeddableCartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(400, SWT.DEFAULT).grab(true, true).span(1,2).applyTo(tableContainer);
		TableViewer embeddableCartridgesTableViewer = createEmbeddableCartridgesViewer(tableContainer, dbc);

		// add
		Button addButton = new Button(embeddableCartridgesGroup, SWT.PUSH);
		addButton.setText("&Add...");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(addButton);
		addButton.addSelectionListener(onAdd());

		// delete
		Button removeButton = new Button(embeddableCartridgesGroup, SWT.PUSH);
		removeButton.setText("&Remove");
		GridDataFactory.fillDefaults()
				.hint(110, SWT.DEFAULT).align(SWT.FILL, SWT.TOP).applyTo(removeButton);
		IObservableValue selectedEmbeddableCartridge = ViewerProperties.singleSelection().observe(embeddableCartridgesTableViewer);
		removeButton.addSelectionListener(onRemove(selectedEmbeddableCartridge));
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.notUpdatingParticipant()
				.to(selectedEmbeddableCartridge)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);
		
		// advanced configurations
		createAdvancedGroup(parent, dbc);
	}

	private void createAdvancedGroup(Composite parent, DataBindingContext dbc) {
		// advanced button
		Button advancedButton = new Button(parent, SWT.NONE);
		advancedButton.setText(getAdvancedButtonLabel(false));
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

		DialogChildToggleAdapter toggleAdapter = new DialogChildToggleAdapter(advancedComposite, false);
		advancedButton.addSelectionListener(
				onAdvancedClicked(advancedButton, toggleAdapter, sourceUrlText, sourceCodeUrlLabel));

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
		ValueBindingBuilder
				.bind(WidgetProperties.visible().observe(environmentVariablesButton))
				.notUpdatingParticipant()
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_ENVIRONMENT_VARIABLES_SUPPORTED)
						.observe(pageModel))
				.in(dbc);
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
					pageModel.setDomains(domainWizard.getDomains());
					pageModel.setDomain(domainWizard.getDomain());
				}
			}
		};
	}

	private SelectionListener onAdvancedClicked(final Button toggleButton, final DialogChildToggleAdapter adapter,
			final Text sourceUrlText, final Label sourceCodeUrlLabel) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				adapter.toggle();
				sourceUrlText.setEnabled(!pageModel.isDefaultSourcecode());
				sourceCodeUrlLabel.setEnabled(!pageModel.isDefaultSourcecode());
				toggleButton.setText(getAdvancedButtonLabel(adapter.isVisible()));
			}
		};
	}

	protected String getAdvancedButtonLabel(boolean visible) {
		if (visible) {
			return " << Advanced ";
		} else {
			return " Advanced >> ";
		}
	}

	private SelectionListener onBrowseEnvironmentVariables(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				NewEnvironmentVariablesWizard environmentVariablesWizard = 
						new NewEnvironmentVariablesWizard(pageModel.getEnvironmentVariables(), pageModel.getDomain());
				if (new OkButtonWizardDialog(getShell(), environmentVariablesWizard).open() == Dialog.OK) {
					pageModel.setEnvironmentVariables(environmentVariablesWizard.getEnvironmentVariables());
				}
			}
		};
	}

	private TableViewer createEmbeddableCartridgesViewer(Composite tableContainer, DataBindingContext dbc) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.sorter(new EmbeddableCartridgeViewerSorter())
				.comparer(new EqualityComparer())
				.contentProvider(new ObservableSetContentProvider())
				.<IEmbeddableCartridge> column("Name")
					.weight(1)
					.labelProvider(new IColumnLabelProvider<IEmbeddableCartridge>() {
	
						@Override
						public String getValue(IEmbeddableCartridge cartridge) {
							return OpenShiftResourceUtils.toString(cartridge);
						}
					}).buildColumn()
				.buildViewer();
		
		viewer.setInput(
				BeanProperties.set(
						ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_EMBEDDABLE_CARTRIDGES).observe(pageModel));

		return viewer;
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				AddEmbeddableCartridgesWizard cartridgesWizard = new AddEmbeddableCartridgesWizard(wizardModel);
				if (new OkCancelButtonWizardDialog(getShell(), cartridgesWizard).open()
						== IDialogConstants.OK_ID) {
					pageModel.setSelectedEmbeddableCartridges(cartridgesWizard.getCheckedEmbeddableCartridges());
				};
			}

		};
	}

	private SelectionListener onRemove(final IObservableValue selectedEmbeddableCartridgeObservable) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Object value = selectedEmbeddableCartridgeObservable.getValue();
				if (!(value instanceof IEmbeddableCartridge)) {
					return;
				}
				IEmbeddableCartridge cartridge = (IEmbeddableCartridge) value;
				remove(cartridge);
			}

			private void remove(IEmbeddableCartridge cartridge) {
				EmbedCartridgeStrategy embedCartridgeStrategy = createEmbedCartridgeStrategy(pageModel.getDomain());
				EmbeddableCartridgeDiff additionalOperations =
						embedCartridgeStrategy.remove(cartridge, pageModel.getSelectedEmbeddableCartridges());
				if (additionalOperations.hasChanges()) {
					executeAdditionalOperations(cartridge, additionalOperations);
				}
				wizardModel.removeSelectedEmbeddableCartridge(cartridge);
			}

			private EmbedCartridgeStrategy createEmbedCartridgeStrategy(IDomain domain) {
				IOpenShiftConnection connection = domain.getUser().getConnection();
				EmbedCartridgeStrategy embedCartridgeStrategy =
						new EmbedCartridgeStrategy(
								connection.getEmbeddableCartridges(),
								connection.getStandaloneCartridges(), 
								domain.getApplications());
				return embedCartridgeStrategy;
			}

			protected void executeAdditionalOperations(IEmbeddableCartridge cartridge,
					EmbeddableCartridgeDiff additionalOperations) {
				int result = openAdditionalOperationsDialog("Remove Cartridges",
						new StringBuilder()
								.append(NLS.bind("If you want to remove {0}, it is suggested you:\n",
										new EmbeddableCartridgeToStringConverter()
												.toString(additionalOperations.getCartridge())))
								.append(additionalOperations.toString())
								.append("\n\nDo you want to Apply or Ignore these suggestions??")
								.toString());
				switch (result) {
				case 1:
					wizardModel.removeSelectedEmbeddableCartridges(additionalOperations.getRemovals());
					wizardModel.addSelectedEmbeddableCartridges(additionalOperations.getAdditions());
					break;
				case 0:
					break;
				case 2:
					// user has chosen to ignore additional requirements
					break;
				}
			}
			
			public int openAdditionalOperationsDialog(String title, String message) {
				MessageDialog dialog = new MessageDialog(getShell(),
						title, null, message, MessageDialog.QUESTION, new String[] { "Cancel", "Apply", "Ignore" }, 0);
				return dialog.open();
			}
		};
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

	@Override
	protected void onPageWillGetActivated(Direction direction, PageChangingEvent event, DataBindingContext dbc) {
		if (pageModel.isUseExistingApplication()) {
			// skip page if we're importing an existing application
			getContainer().showPage(direction.getFollowingPage(this));
			event.doit = false;
			return;
		}
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		try {
			// needs to be done before loading resources, otherwise:
			// dbc.updateModels() will be called and old data could be
			// restored
			loadOpenshiftResources(dbc);
			applicationNameText.setFocus();
		} catch (OpenShiftException e) {
			Logger.error("Failed to reset page fields", e);
		}
	}
	
	protected void loadOpenshiftResources(final DataBindingContext dbc) {
		try {

			if (pageModel.isResourcesLoaded()) {
				return;
			}
			
			WizardUtils.runInWizard(new AbstractDelegatingMonitorJob("Loading applications, cartridges and gears...") {

				@Override
				protected IStatus doRun(IProgressMonitor monitor) {
					try {
						pageModel.loadResources();
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

	class NewApplicationNameValidator extends MultiValidator {

		private final IObservableValue applicationNameObservable;
		private IObservableValue existingApplicationsLoadedObservable;
		private IObservableValue useExistingApplicationObservable;

		NewApplicationNameValidator(IObservableValue useExistingApplication,
				IObservableValue existingApplicationsLoaded, IObservableValue applicationName) {
			this.useExistingApplicationObservable = useExistingApplication;
			this.existingApplicationsLoadedObservable = existingApplicationsLoaded; 
			this.applicationNameObservable = applicationName;
		}

		@Override
		protected IStatus validate() {
			final boolean useExistingApplication = (Boolean) useExistingApplicationObservable.getValue();
			final String applicationName = (String) applicationNameObservable.getValue();
			final Boolean existingApplicationsLoaded = (Boolean) existingApplicationsLoadedObservable.getValue();
			
			if (useExistingApplication) {
				return ValidationStatus.ok();
			} else if (applicationName.isEmpty()) {
				return ValidationStatus.cancel(
						"Please choose a name for your new application.");
			} else if (!StringUtils.isAlphaNumeric(applicationName)) {
				return ValidationStatus.error(
						"The name may only contain letters and digits.");
			} else if (existingApplicationsLoaded != null
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

	static class SourceCodeUrlValidator extends MultiValidator {

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
				if (UrlUtils.isValid(gitUri)) {
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
}
