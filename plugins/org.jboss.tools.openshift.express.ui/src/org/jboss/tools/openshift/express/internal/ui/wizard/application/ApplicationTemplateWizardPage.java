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
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.property.list.IListProperty;
import org.eclipse.core.databinding.property.list.MultiListProperty;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider;
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
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.ParametrizableWizardPageSupport;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.connection.Connection;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.job.AbstractDelegatingMonitorJob;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;
import org.jboss.tools.openshift.express.internal.ui.utils.UIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.OkCancelButtonWizardDialog;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.NewDomainWizard;
import org.jboss.tools.openshift.express.internal.ui.wizard.ssh.NoSSHKeysWizard;

import com.openshift.client.IApplication;
import com.openshift.client.IQuickstart;
import com.openshift.client.NotFoundOpenShiftException;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateWizardPage extends AbstractOpenShiftWizardPage {

	private ApplicationTemplateWizardPageModel pageModel;
	private Text existingAppNameText;
	private OpenShiftApplicationWizardModel wizardModel;

	ApplicationTemplateWizardPage(IWizard wizard, OpenShiftApplicationWizardModel wizardModel) {
		super("Existing or new application", "", "Select an existing or create a new application", wizard);
		this.pageModel = new ApplicationTemplateWizardPageModel(this.wizardModel = wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults()
				.numColumns(2)
				.margins(10, 10)
				.applyTo(container);

		setWizardPageDescription(pageModel.isUseExistingApplication());

		SelectObservableValue useExitingApplication = new SelectObservableValue();
		ValueBindingBuilder
			.bind(useExitingApplication)
			.to(BeanProperties.value(
				ApplicationTemplateWizardPageModel.PROPERTY_USE_EXISTING_APPLICATION).observe(pageModel))
			.in(dbc);
		
		createExistingApplicationControls(container, useExitingApplication, dbc);
		createNewApplicationControls(container, useExitingApplication, dbc);
	}

	private void createExistingApplicationControls(Composite parent, SelectObservableValue useExitingApplication, DataBindingContext dbc) {
		// existing app radio
		Button useExistingApplicationButton = new Button(parent, SWT.RADIO);
		useExistingApplicationButton.setText("Use my existing OpenShift application:");
		useExistingApplicationButton.setToolTipText("If selected we will import your existing application.");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.span(2, 1)
				.grab(true, false)
				.applyTo(useExistingApplicationButton);

		useExitingApplication.addOption(Boolean.TRUE, WidgetProperties.selection().observe(useExistingApplicationButton));

		// existing app explanatory label
		Label existingAppLabel = new Label(parent, SWT.None);
		existingAppLabel.setText("We will clone and import your existing application to a workspace project.");
		existingAppLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(existingAppLabel);

		// existing app name
		this.existingAppNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(existingAppNameText);
		IObservableValue existingAppNameTextObservable = WidgetProperties.text(SWT.Modify).observe(existingAppNameText);
		ValueBindingBuilder
				.bind(existingAppNameTextObservable)
				.to(BeanProperties.value(
						ApplicationTemplateWizardPageModel.PROPERTY_EXISTING_APPLICATION_NAME).observe(pageModel))
				.in(dbc);
		createExistingAppNameContentAssist(existingAppNameText);
		existingAppNameText.addFocusListener(onExistingApplicationNameFocused());
		
		IObservableValue existingApplicationsLoaded =
				BeanProperties.value(ApplicationTemplateWizardPageModel.PROPERTY_RESOURCES_LOADED)
						.observe(pageModel);
		IObservableValue existingApplicationObservable =
				BeanProperties.value(ApplicationTemplateWizardPageModel.PROPERTY_EXISTING_APPLICATION).observe(pageModel);
		final ExistingApplicationNameValidator existingAppValidator =
				new ExistingApplicationNameValidator(
						useExitingApplication, 
						existingAppNameTextObservable, 
						existingApplicationObservable , 
						existingApplicationsLoaded);
		dbc.addValidationStatusProvider(existingAppValidator);
		ControlDecorationSupport.create(
				existingAppValidator, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		// browse button
		Button browseAppsButton = new Button(parent, SWT.NONE);
		browseAppsButton.setText("Browse...");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).grab(false, false)
				.applyTo(browseAppsButton);
		browseAppsButton.addSelectionListener(onBrowseExistingApps(dbc));
	}

	protected FocusListener onExistingApplicationNameFocused() {
		return new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				pageModel.setUseExistingApplication(true);
			}

		};
	}

	private AutoCompleteField createExistingAppNameContentAssist(Text existingAppNameText) {
		final AutoCompleteField autoCompleteField =
				new AutoCompleteField(existingAppNameText, new TextContentAdapter(), new String[]{});

		pageModel.addPropertyChangeListener(
				ApplicationTemplateWizardPageModel.PROPERTY_EXISTING_APPLICATIONS,
				onExistingApplicationsChanged(autoCompleteField));
		
		ControlDecoration dec = new ControlDecoration(existingAppNameText, SWT.TOP | SWT.RIGHT);
		FieldDecoration contentProposalFieldIndicator =
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing an application name.");
		dec.setShowOnlyOnFocus(true);
		
		return autoCompleteField;
	}

	protected PropertyChangeListener onExistingApplicationsChanged(final AutoCompleteField autoCompleteField) {
		return new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				autoCompleteField.setProposals(pageModel.getExistingApplicationNames());
			}
		};
	}

	private SelectionListener onBrowseExistingApps(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				pageModel.setUseExistingApplication(true);

				SelectApplicationWizard selectApplicationWizard = new SelectApplicationWizard(wizardModel);
				if (new OkCancelButtonWizardDialog(getShell(), selectApplicationWizard).open()
						== IDialogConstants.CANCEL_ID) {
					return;
				}
				final IApplication selectedApplication = selectApplicationWizard.getSelectedApplication();
				if (selectedApplication != null){
					pageModel.setExistingApplication(selectedApplication);
				}
			}
		};
	}

	private void createNewApplicationControls(Composite parent, SelectObservableValue useExitingApplication, DataBindingContext dbc) {
		// existing app radio
		Button newApplicationButton = new Button(parent, SWT.RADIO);
		newApplicationButton.setText("Create a new OpenShift application:");
		newApplicationButton.setToolTipText("If selected we will create a new application in OpenShift.");
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.indent(0, 20)
				.applyTo(newApplicationButton);

		useExitingApplication.addOption(Boolean.FALSE, WidgetProperties.selection().observe(newApplicationButton));
		
		// new app explanatory label
		Label existingAppLabel = new Label(parent, SWT.None);
		existingAppLabel
				.setText("You can create an application form scratch or handpick from existing cartridges you need.");
		existingAppLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.CENTER)
				.grab(true, false)
				.applyTo(existingAppLabel);

		Composite applicationTemplatesTreeComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.span(2, 1)
				.align(SWT.FILL, SWT.FILL)
				.grab(true, true)
				.applyTo(applicationTemplatesTreeComposite);
		GridLayoutFactory.fillDefaults().spacing(2, 2).applyTo(applicationTemplatesTreeComposite);

		// filter text
		Text templateFilterText = UIUtils.createSearchText(applicationTemplatesTreeComposite);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(templateFilterText);

		// application templates tree
		final TreeViewer applicationTemplatesViewer = createApplicationTemplatesViewer(applicationTemplatesTreeComposite, templateFilterText);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL).grab(true, true)
				.hint(400, 250)
				.applyTo(applicationTemplatesViewer.getControl());

		templateFilterText.addModifyListener(onFilterTextModified(applicationTemplatesViewer));
		
		IObservableValue selectedApplicationTemplateViewerObservable =
				ViewerProperties.singleSelection().observe(applicationTemplatesViewer);
		IObservableValue selectedApplicationTemplateModelObservable = 
				BeanProperties.value(
				ApplicationTemplateWizardPageModel.PROPERTY_SELECTED_APPLICATION_TEMPLATE).observe(pageModel);
		ValueBindingBuilder
				.bind(selectedApplicationTemplateViewerObservable)
				.to(selectedApplicationTemplateModelObservable)
				.in(dbc);
		
		ApplicationTemplateValidator selectedApplicationTemplateValidator =
				new ApplicationTemplateValidator(
						useExitingApplication, selectedApplicationTemplateViewerObservable);
		dbc.addValidationStatusProvider(selectedApplicationTemplateValidator);
		ControlDecorationSupport.create(
				selectedApplicationTemplateValidator, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater(true));

		
		// selected application template details
		Group detailsContainer = new Group(applicationTemplatesTreeComposite, SWT.NONE);
		detailsContainer.setText("Details");
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.FILL)
				.hint(SWT.DEFAULT, 140)
				.applyTo(detailsContainer);
				
		new ApplicationTemplateDetailViews(selectedApplicationTemplateModelObservable, useExitingApplication,
				detailsContainer, dbc)
				.createControls();
	}

	protected ModifyListener onFilterTextModified(final TreeViewer applicationTemplatesViewer) {
		return new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				applicationTemplatesViewer.refresh();
				applicationTemplatesViewer.expandAll();
			}
		};
	}

	private TreeViewer createApplicationTemplatesViewer(Composite parent, Text filterText) {
		TreeViewer applicationTemplatesViewer =
				new TreeViewer(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		IListProperty childrenProperty = new MultiListProperty(
				new IListProperty[] {
						BeanProperties.list(ApplicationTemplateWizardPageModel.PROPERTY_APPLICATION_TEMPLATES),
						BeanProperties.list(IApplicationTemplate.PROPERTY_CHILDREN) });
		ObservableListTreeContentProvider contentProvider =
				new ObservableListTreeContentProvider(childrenProperty.listFactory(), null);
		applicationTemplatesViewer.setContentProvider(contentProvider);
		applicationTemplatesViewer.setLabelProvider(new ApplicationTemplateViewLabelProvider());
		applicationTemplatesViewer.addFilter(new ApplicationTemplateViewerFilter(filterText));
		applicationTemplatesViewer.setInput(pageModel);
		return applicationTemplatesViewer;
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
			loadOpenshiftResources(existingAppNameText, dbc);
		} catch (OpenShiftException e) {
			Logger.error("Failed to load openshift resources.", e);
		}
	}

	/**
	 * Checks that the user has a domain, opens the creation dialog in case he
	 * hasn't, closes the wizard if the user does not create a domain (required
	 * for any application creation). Otherwise, returns true.
	 */
	protected boolean ensureHasDomain() {
		final Connection connection = pageModel.getConnection();
		try {
			if (connection == null
					|| connection.hasDomain()) {
				return true;
			}
			WizardDialog dialog = new WizardDialog(
					Display.getCurrent().getActiveShell(), new NewDomainWizard(connection));
			dialog.create();
			dialog.setBlockOnOpen(true);
			return dialog.open() == Dialog.OK;
		} catch (OpenShiftException e) {
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not lookup domain in OpenShift connection {0}", connection.getId()),
					status, IStatus.ERROR)
					.open();
			OpenShiftUIActivator.log(status);
			return false;
		}
	}

	protected boolean ensureHasSSHKeys() {
		final Connection connection = pageModel.getConnection();
		try {
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
			IStatus status = OpenShiftUIActivator.createErrorStatus(e.getMessage(), e);
			new ErrorDialog(getShell(), "Error",
					NLS.bind("Could not lookup ssh keys in OpenShift connection {0}", connection.getId()),
					status, IStatus.ERROR)
					.open();
			OpenShiftUIActivator.log(status);;
			return false;
		}
	}

	protected void loadOpenshiftResources(final Text existingAppNameText, final DataBindingContext dbc) {
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
		} catch (Exception e) {
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

	@Override
	protected void setupWizardPageSupport(DataBindingContext dbc) {
		ParametrizableWizardPageSupport.create(
				IStatus.ERROR | IStatus.INFO | IStatus.CANCEL, this, dbc);
	}
	
	class ExistingApplicationNameValidator extends MultiValidator {

		private final IObservableValue useExistingApplicationObservable;
		private final IObservableValue applicationNameTextObservable;
		private final IObservableValue existingApplicationObservable;
		private final IObservableValue existingApplicationsLoadedObservable;

		public ExistingApplicationNameValidator(IObservableValue useExistingAppBtnbservable,
				IObservableValue existingApplicationNameTextObservable, IObservableValue existingApplicationObservable, 
				IObservableValue existingApplicationsLoadedObservable) {
			this.useExistingApplicationObservable = useExistingAppBtnbservable;
			this.applicationNameTextObservable = existingApplicationNameTextObservable;
			this.existingApplicationObservable = existingApplicationObservable;
			this.existingApplicationsLoadedObservable = existingApplicationsLoadedObservable;
		}

		@Override
		protected IStatus validate() {
			final Boolean useExistingApplication = (Boolean) useExistingApplicationObservable.getValue();
			final String applicationName = (String) applicationNameTextObservable.getValue();
			final IApplication application = (IApplication) existingApplicationObservable.getValue();
			final Boolean existingApplicationsLoaded = (Boolean) existingApplicationsLoadedObservable.getValue();

			if (useExistingApplication == null 
				|| !useExistingApplication) {
				return ValidationStatus.ok();
			}

			if (StringUtils.isEmpty(applicationName)) {
				return ValidationStatus.cancel("Please select an existing OpenShift application");
			}

			if (existingApplicationsLoaded != null
					&& !existingApplicationsLoaded) {
				return ValidationStatus.cancel("Existing applications are not loaded yet.");
			}

			if (application == null) {
				return ValidationStatus.error(NLS.bind("You dont have an application called {0} in your domains.", applicationName));
			}
			return ValidationStatus.ok();

		}

		@Override
		public IObservableList getTargets() {
			IObservableList targets = new WritableList();
			targets.add(applicationNameTextObservable);
			return targets;
		}
	}

	class ApplicationTemplateValidator extends MultiValidator {

		private final IObservableValue useExistingApplicationObservable;
		private final IObservableValue selectedApplicationTemplateObservable;

		public ApplicationTemplateValidator(IObservableValue useExistingApplicationObservable,
				IObservableValue selectedApplicationTemplateObservable) {
			this.useExistingApplicationObservable = useExistingApplicationObservable;
			this.selectedApplicationTemplateObservable = selectedApplicationTemplateObservable;
		}

		@Override
		protected IStatus validate() {
			final Boolean useExistingApplication = (Boolean) useExistingApplicationObservable.getValue();
			final IApplicationTemplate selectedApplicationTemplate = (IApplicationTemplate) selectedApplicationTemplateObservable.getValue();

			if (useExistingApplication == null
					|| useExistingApplication) {
				return ValidationStatus.ok();
			}

			if (selectedApplicationTemplate == null
					|| !selectedApplicationTemplate.canCreateApplication()) {
				return ValidationStatus.cancel("Please select an existing cartridge, a quickstart or provide your own framework.");
			}

			return ValidationStatus.ok();

		}
	}

	private class ApplicationTemplateViewLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (!(element instanceof IApplicationTemplate)) {
				return;
			}

			StyledString text = new StyledString();
			if (element instanceof ICartridgeApplicationTemplate) {
				createCartridgeTemplatelabel(text, (ICartridgeApplicationTemplate) element);
			} else if (element instanceof IQuickstartApplicationTemplate) {
				createQuickstartTemplatelabel(text, (IQuickstartApplicationTemplate) element);
			} else {
				createApplicationTemplateLabel(text, (IApplicationTemplate) element);
			}

			cell.setText(text.toString());
			cell.setStyleRanges(text.getStyleRanges());
			super.update(cell);
		}

		private void createCartridgeTemplatelabel(StyledString text, ICartridgeApplicationTemplate cartridgeTemplate) {
			createApplicationTemplateLabel(text, cartridgeTemplate);
			ICartridge cartridge = cartridgeTemplate.getStandaloneCartridge();
			if (cartridge != null
					&& !StringUtils.isEmpty(cartridge.getName())) {
				text.append(" ", StyledString.DECORATIONS_STYLER);
				text.append(cartridge.getName(), StyledString.DECORATIONS_STYLER);
			}
		}

		private void createQuickstartTemplatelabel(StyledString text, IQuickstartApplicationTemplate quickstartTemplate) {
			createApplicationTemplateLabel(text, quickstartTemplate);
			if (!StringUtils.isEmpty(quickstartTemplate.getName())) {
				text.append(" ", StyledString.DECORATIONS_STYLER);
				text.append(quickstartTemplate.getTagsString(), StyledString.DECORATIONS_STYLER);
			}
		}

		private void createApplicationTemplateLabel(StyledString text, IApplicationTemplate applicationTemplate) {
			text.append(applicationTemplate.getName());
		}
	}

	class ApplicationTemplateViewerFilter extends ViewerFilter {

		private Text filterText;

		public ApplicationTemplateViewerFilter(Text filterText) {
			Assert.isLegal(!DisposeUtils.isDisposed(filterText));
			this.filterText = filterText;
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (!(element instanceof IApplicationTemplate)) {
				return false;
			}
			return ((IApplicationTemplate) element).isMatching(filterText.getText());
		}
		
	}
	
}
