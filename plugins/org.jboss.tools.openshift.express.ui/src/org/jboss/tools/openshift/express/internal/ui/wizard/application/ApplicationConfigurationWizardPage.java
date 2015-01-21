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
import java.util.ArrayList;

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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
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
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy;
import org.jboss.tools.openshift.express.internal.core.cartridges.EmbedCartridgeStrategy.EmbeddableCartridgeDiff;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.core.util.CartridgeToStringConverter;
import org.jboss.tools.openshift.express.internal.core.util.ExpressResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.EmptyStringToNullConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.IsNotNull2BooleanConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.MultiConverter;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.databinding.TrimmingStringConverter;
import org.jboss.tools.openshift.express.internal.ui.explorer.AbstractLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.utils.DialogChildVisibilityAdapter;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder;
import org.jboss.tools.openshift.express.internal.ui.utils.TableViewerBuilder.IColumnLabelProvider;
import org.jboss.tools.openshift.express.internal.ui.viewer.EmbeddableCartridgeViewerSorter;
import org.jboss.tools.openshift.express.internal.ui.viewer.EqualityComparer;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.ManageDomainsWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.environment.NewEnvironmentVariablesWizard;
import org.jboss.tools.openshift.internal.common.core.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.internal.common.ui.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
public class ApplicationConfigurationWizardPage extends AbstractOpenShiftWizardPage {
	
	public static final int RESULT_CANCEL = 0;
	public static final int RESULT_APPLY = 1;
	public static final int RESULT_IGNORE = 2;

	private ApplicationConfigurationWizardPageModel pageModel;
	private Text applicationNameText;
	private OpenShiftApplicationWizardModel wizardModel;
	private Button advancedButton;
	private DialogChildVisibilityAdapter advancedSectionVisibilityAdapter;

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
						// TODO: move to model
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
				if (!(element instanceof IDomain)) {
					return null;
				}
				return ((IDomain) element).getId();
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
					ExpressUIActivator.log(ExpressUIActivator.createErrorStatus(NLS.bind(
							"Could not load applications for domain {0}.",
							domain), e));
				} catch (InterruptedException e) {
					ExpressUIActivator.log(ExpressUIActivator.createErrorStatus(NLS.bind(
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
								return StringUtils.shorten((String) fromObject, 50) ;
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
				return ExpressResourceLabelUtils.toString((IGearProfile) element);			
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
		embeddableCartridgesGroup.setText(getCartridgesListLabel(pageModel.getSelectedApplicationTemplate()));
		BeanProperties
				.value(ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_APPLICATION_TEMPLATE)
				.observe(pageModel)
				.addValueChangeListener(onApplicationTemplateChanged(embeddableCartridgesGroup));
		GridDataFactory.fillDefaults()
				.grab(true, true).align(SWT.FILL, SWT.FILL).span(3, 1).applyTo(embeddableCartridgesGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6).applyTo(embeddableCartridgesGroup);

		Composite tableContainer = new Composite(embeddableCartridgesGroup, SWT.NONE);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(400, SWT.DEFAULT).grab(true, true).span(1,2).applyTo(tableContainer);
		TableViewer embeddableCartridgesTableViewer = createEmbeddableCartridgesViewer(tableContainer);
		embeddableCartridgesTableViewer.setInput(
				BeanProperties.set(
						ApplicationConfigurationWizardPageModel.PROPERTY_EMBEDDED_CARTRIDGES).observe(pageModel));
		ValueBindingBuilder
			.bind(ViewerProperties.singlePostSelection().observe(embeddableCartridgesTableViewer))
			.to(BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(pageModel))
			.in(dbc);
		
		Composite buttonsComposite = createAddRemoveEditButtons(embeddableCartridgesGroup, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).applyTo(buttonsComposite);

		// advanced configurations
		createAdvancedGroup(parent, dbc);
	}

	private IValueChangeListener onApplicationTemplateChanged(final Group cartridgesGroup) {
		return new IValueChangeListener() {
			
			@Override
			public void handleValueChange(ValueChangeEvent event) {
				if (event.diff.getNewValue() instanceof IApplicationTemplate) {
					IApplicationTemplate template = (IApplicationTemplate) event.diff.getNewValue();
					cartridgesGroup.setText(getCartridgesListLabel(template));
					if (template instanceof IQuickstartApplicationTemplate) {
						IQuickstartApplicationTemplate quickstart = (IQuickstartApplicationTemplate) template;
						showAdvancedSection(!StringUtils.isEmpty(quickstart.getInitialGitUrl()));
					} else {
						showAdvancedSection(false);
					}
				}
			}
		};
	}

	private String getCartridgesListLabel(IApplicationTemplate template) {
		if (template instanceof IQuickstartApplicationTemplate) {
			return "All Cartridges";
		} else {
			return "Embedded Cartridges";
		}
	}
	
	/**
	 * Creates a stack layout with 2 different panels:
	 * <ul>
	 * <li>edit buttons</li>
	 * <li>add- and remove buttons</li>
	 * </ul>
	 */
	private Composite createAddRemoveEditButtons(Group parent, DataBindingContext dbc) {
		Composite buttonsContainer = new Composite(parent, SWT.None);
		StackLayout stackLayout = new StackLayout();
		buttonsContainer.setLayout(stackLayout);

		// add, remove
		Composite addRemoveButtons = new Composite(buttonsContainer, SWT.None);
		GridLayoutFactory.fillDefaults().applyTo(addRemoveButtons);
		
		Button addButton = new Button(addRemoveButtons, SWT.PUSH);
		addButton.setText("&Add...");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(110, SWT.DEFAULT).applyTo(addButton);
		addButton.addSelectionListener(onAdd());

		Button removeButton = new Button(addRemoveButtons, SWT.PUSH);
		removeButton.setText("&Remove");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).hint(110, SWT.DEFAULT).applyTo(removeButton);
		removeButton.addSelectionListener(onRemove());
		IObservableValue selectedEmbeddableCartridge =
				BeanProperties
						.value(ApplicationConfigurationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE)
						.observe(pageModel);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(removeButton))
				.notUpdatingParticipant()
				.to(selectedEmbeddableCartridge )
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);

		// edit
		Composite editButtons = new Composite(buttonsContainer, SWT.None);
		GridLayoutFactory.fillDefaults().applyTo(editButtons);

		Button editButton = new Button(editButtons, SWT.PUSH);
		editButton.setText("&Edit...");
		GridDataFactory.fillDefaults()
			.align(SWT.FILL, SWT.FILL).hint(110, SWT.DEFAULT).applyTo(editButton);
		editButton.addSelectionListener(onEdit());
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(editButton))
				.notUpdatingParticipant()
				.to(selectedEmbeddableCartridge)
				.converting(new IsNotNull2BooleanConverter())
				.in(dbc);

		stackLayout.topControl = getEmbeddableCartridgesButtons(
				pageModel.isCanAddRemoveCartridges(), addRemoveButtons, editButtons);

		BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_CAN_ADDREMOVE_CARTRIDGES)
				.observe(pageModel)
				.addValueChangeListener(onCanAddRemoveChanged(addRemoveButtons, editButtons, buttonsContainer, stackLayout));
		
		return buttonsContainer;
	}

	/**
	 * listens to changes in
	 * {@link ApplicationConfigurationWizardPageModel#PROPERTY_CAN_ADDREMOVE_CARTRIDGES}
	 * and shows the edit- or the add- & remove-buttons.
	 */
	private IValueChangeListener onCanAddRemoveChanged(
			final Composite addRemoveButtons, final Composite editButtons, final Composite parent, final StackLayout stackLayout) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				if (!(event.diff.getNewValue() instanceof Boolean)) {
					return;
				}

				stackLayout.topControl = getEmbeddableCartridgesButtons(
						(Boolean) event.diff.getNewValue(), addRemoveButtons, editButtons);
				parent.layout(true);
				
			}
		};
	}

	private Composite getEmbeddableCartridgesButtons(
			boolean canAddRemoveEmbeddableCartridges, Composite addRemoveButtons, Composite editButtons) {
		if (canAddRemoveEmbeddableCartridges) {
			return addRemoveButtons;
		} else {
			return editButtons;
		}
	}
	
	private void createAdvancedGroup(Composite parent, DataBindingContext dbc) {
		// advanced button
		this.advancedButton = new Button(parent, SWT.NONE);
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
		IObservableValue useDefaultSourceButtonObservable = WidgetProperties.selection().observe(useDefaultSourceButton);
		IObservableValue useInitialGitUrlModelObservable =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_USE_INITIAL_GITURL)
						.observe(pageModel);
		ValueBindingBuilder
				.bind(useDefaultSourceButtonObservable)
				.converting(new InvertingBooleanConverter())
				.to(useInitialGitUrlModelObservable)
				.converting(new InvertingBooleanConverter())
				.in(dbc);
		IObservableValue initialGitUrlEditable =
				BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_INITIAL_GITURL_EDITABLE).observe(pageModel);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(useDefaultSourceButton))
				.notUpdatingParticipant()
				.to(initialGitUrlEditable)
				.in(dbc);

		// source code text
		Label sourceUrlLabel = new Label(sourceGroup, SWT.NONE);
		sourceUrlLabel.setText("Source code:");
		GridDataFactory.fillDefaults()
				.align(SWT.BEGINNING, SWT.CENTER).applyTo(sourceUrlLabel);
		final IObservableValue sourceUrlWidgetsEnablement =
				BeanProperties.value(ApplicationConfigurationWizardPageModel.PROPERTY_INITIAL_GITURL_USEREDITABLE)
						.observe(pageModel);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(sourceUrlLabel))
				.notUpdatingParticipant()
				.to(sourceUrlWidgetsEnablement)
				.in(dbc);
		Text sourceUrlText = new Text(sourceGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(sourceUrlText);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(sourceUrlText))
				.notUpdatingParticipant()
				.to(sourceUrlWidgetsEnablement)
				.in(dbc);
		IObservableValue sourcecodeUrlObservable = WidgetProperties.text(SWT.Modify).observe(sourceUrlText);
		ValueBindingBuilder
				.bind(sourcecodeUrlObservable)
				.converting(new MultiConverter(new TrimmingStringConverter(), new EmptyStringToNullConverter()))
				.to(BeanProperties.value(
						ApplicationConfigurationWizardPageModel.PROPERTY_INITIAL_GITURL).observe(pageModel))
				.in(dbc);

		MultiValidator sourceCodeUrlValidator = new SourceCodeUrlValidator(useDefaultSourceButtonObservable,
				sourcecodeUrlObservable);
		dbc.addValidationStatusProvider(sourceCodeUrlValidator);
		ControlDecorationSupport.create(
				sourceCodeUrlValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());

		this.advancedSectionVisibilityAdapter = new DialogChildVisibilityAdapter(advancedComposite, false);
		advancedButton.addSelectionListener(
				onAdvancedClicked());

		// explanation
		StyledText sourceCodeExplanationText = new StyledText(sourceGroup, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
		sourceCodeExplanationText.setAlwaysShowScrollBars(false);
		sourceCodeExplanationText
				.setText("Your application will start with an exact copy of the code and configuration "
						+ "provided in this Git repository instead of the default application.");
		sourceCodeExplanationText.setEnabled(false);
		UIUtils.setTransparent(sourceCodeExplanationText);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER).grab(true, true).span(2, 1).applyTo(sourceCodeExplanationText);
		ValueBindingBuilder
				.bind(WidgetProperties.enabled().observe(sourceCodeExplanationText))
				.notUpdatingParticipant()
				.to(sourceUrlWidgetsEnablement)
				.in(dbc);
		
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
				ExpressConnection connection = pageModel.getConnection();
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
	
	private TableViewer createEmbeddableCartridgesViewer(Composite tableContainer) {
		Table table = new Table(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		TableViewer viewer = new TableViewerBuilder(table, tableContainer)
				.sorter(new EmbeddableCartridgeViewerSorter())
				.comparer(new EqualityComparer())
				.contentProvider(new ObservableSetContentProvider())
				.<ICartridge> column("Name")
					.weight(1)
					.labelProvider(new IColumnLabelProvider<ICartridge>() {
	
						@Override
						public String getValue(ICartridge cartridge) {
							return ExpressResourceLabelUtils.toString(cartridge);
						}
					}).buildColumn()
				.buildViewer();
		return viewer;
	}

	private SelectionListener onAdd() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				AddEmbeddableCartridgesWizard cartridgesWizard = new AddEmbeddableCartridgesWizard(wizardModel);
				if (new OkCancelButtonWizardDialog(getShell(), cartridgesWizard).open()
						== IDialogConstants.OK_ID) {
					pageModel.setEmbeddedCartridges(cartridgesWizard.getCheckedCartridges());
				};
			}

		};
	}

	private SelectionListener onRemove() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				remove(pageModel.getSelectedCartridge());
			}

			private void remove(ICartridge cartridge) {
				EmbedCartridgeStrategy embedCartridgeStrategy = createEmbedCartridgeStrategy(pageModel.getDomain());
				EmbeddableCartridgeDiff additionalOperations =
						embedCartridgeStrategy.remove(cartridge, pageModel.getEmbeddedCartridges());
				int result = RESULT_APPLY;
				if (additionalOperations.hasChanges()) {
					result = executeAdditionalOperations(cartridge, additionalOperations);
				}
				if(result != RESULT_CANCEL){
					wizardModel.removeEmbeddedCartridge(cartridge);
				}
			}

			private EmbedCartridgeStrategy createEmbedCartridgeStrategy(IDomain domain) {
				IOpenShiftConnection connection = domain.getUser().getConnection();
				EmbedCartridgeStrategy embedCartridgeStrategy =
						new EmbedCartridgeStrategy(
								new ArrayList<ICartridge>(connection.getEmbeddableCartridges()),
								new ArrayList<ICartridge>(connection.getStandaloneCartridges()), 
								domain.getApplications());
				return embedCartridgeStrategy;
			}

			protected int executeAdditionalOperations(ICartridge cartridge,
					EmbeddableCartridgeDiff additionalOperations) {
				int result = openAdditionalOperationsDialog("Remove Cartridges",
						new StringBuilder()
								.append(NLS.bind("If you want to remove {0}, it is suggested you:\n",
										new CartridgeToStringConverter()
												.toString(additionalOperations.getCartridge())))
								.append(additionalOperations.toString())
								.append("\n\nDo you want to Apply or Ignore these suggestions??")
								.toString());
				switch (result) {
				case RESULT_APPLY:
					wizardModel.removeEmbeddedCartridges(additionalOperations.getRemovals());
					wizardModel.addEmbeddedCartridges(additionalOperations.getAdditions());
					break;
				case RESULT_CANCEL:
					break;
				case RESULT_IGNORE:
					break;
				}
				return result;
			}
			
			public int openAdditionalOperationsDialog(String title, String message) {
				MessageDialog dialog = new MessageDialog(getShell(),
						title, null, message, MessageDialog.QUESTION, new String[] { "Cancel", "Apply", "Ignore" }, RESULT_APPLY);
				return dialog.open();
			}
		};
	}

	private SelectionListener onEdit() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ICartridge selectedCartridge = pageModel.getSelectedCartridge();
				if (selectedCartridge == null) {
					return;
				}
				IApplicationTemplate selectedTemplate = pageModel.getSelectedApplicationTemplate();
				if (selectedTemplate == null
						|| selectedTemplate.canAddRemoveCartridges()) {
					return;
				}
				IQuickstartApplicationTemplate selectedQuickstart = (IQuickstartApplicationTemplate) selectedTemplate;
				EditAlternativeCartridgesWizard cartridgesWizard =
						new EditAlternativeCartridgesWizard(
								selectedCartridge, selectedQuickstart.getAlternativesFor(selectedCartridge),
								wizardModel);
				if (new OkCancelButtonWizardDialog(getShell(), cartridgesWizard).open()
						== IDialogConstants.OK_ID) {
					ICartridge checkedCartridge = cartridgesWizard.getCheckedCartridge();
					replaceSelectedCartridge(selectedCartridge, checkedCartridge);
				};

			}

			protected void replaceSelectedCartridge(ICartridge selectedCartridge, ICartridge checkedCartridge) {
				if (!selectedCartridge.equals(checkedCartridge)) {
					pageModel.removeEmbeddedCartridges(selectedCartridge);
					pageModel.addEmbeddedCartridges(checkedCartridge);
				}
			}
		};
	}

	private SelectionListener onAdvancedClicked() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				showAdvancedSection(!advancedSectionVisibilityAdapter.isVisible());
			}
		};
	}

	protected void showAdvancedSection(boolean visible) {
		advancedSectionVisibilityAdapter.setVisible(visible);
		advancedButton.setText(getAdvancedButtonLabel(visible));
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
		// fix GTK3 combo boxes too small
		// https://issues.jboss.org/browse/JBIDE-16877,
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=431425
		((Composite) getControl()).layout(true, true);
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
						return ExpressUIActivator.createErrorStatus(
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
				if (UrlUtils.isValid(gitUri)
						|| EGitUtils.isValidGitUrl(gitUri)) {
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
