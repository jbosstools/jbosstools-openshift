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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateListStrategy;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

import com.openshift.express.client.Cartridge;
import com.openshift.express.client.IApplication;
import com.openshift.express.client.ICartridge;
import com.openshift.express.client.NotFoundOpenShiftException;

/**
 * @author Andre Dietisheim
 * @author Xavier Coulon
 */
@Deprecated
public class ApplicationSelectionWizardPage extends AbstractOpenShiftWizardPage {

	private TableViewer tableViewer;
	private final ApplicationSelectionWizardPageModel pageModel;

	private Text existingAppNameText = null;

	public ApplicationSelectionWizardPage(IWizard wizard, IOpenShiftWizardModel wizardModel) {
		super("Application selection", "Click 'next' or select another application to import.",
				"Application selection", wizard);
		this.pageModel = new ApplicationSelectionWizardPageModel(wizardModel);
	}

	@Override
	protected void doCreateControls(Composite container, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		createExistingAppSelectionGroup(container, dbc);
		//createApplicationCreationGroup(container, dbc);
	}

	private Composite createExistingAppSelectionGroup(Composite container, DataBindingContext dbc) {
		Composite existingAppSelectionGroup = new Composite(container, SWT.NONE);
		// projectGroup.setText("Project");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(existingAppSelectionGroup);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(6, 6).applyTo(existingAppSelectionGroup);

		// existing app checkbox
		Button useExistingAppBtn = new Button(existingAppSelectionGroup, SWT.CHECK);
		useExistingAppBtn.setText("Use the existing application");
		useExistingAppBtn.setToolTipText("Select an existing application or uncheck to create a new one.");
		useExistingAppBtn.setFocus();
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.FILL, SWT.CENTER).grab(false, false)
				.applyTo(useExistingAppBtn);
		final IObservableValue useExistingAppObservable = BeanProperties.value(
				ApplicationSelectionWizardPageModel.PROPERTY_USE_EXISTING_APPLICATION).observe(pageModel);
		final ISWTObservableValue useExistingAppBtnSelection = WidgetProperties.selection().observe(useExistingAppBtn);
		dbc.bindValue(useExistingAppBtnSelection, useExistingAppObservable);

		// existing project
		Label existingProjectLabel = new Label(existingAppSelectionGroup, SWT.NONE);
		existingProjectLabel.setText("Use the existing project");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(false, false).indent(10, 0)
				.applyTo(existingProjectLabel);

		existingAppNameText = new Text(existingAppSelectionGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(1, 1).grab(true, false)
				.applyTo(existingAppNameText);
		final IObservableValue appNameModelObservable = BeanProperties.value(
				ApplicationSelectionWizardPageModel.PROPERTY_SELECTED_APPLICATION).observe(pageModel);
		final ISWTObservableValue existingAppNameTextObservable = WidgetProperties.text(SWT.Modify).observe(
				existingAppNameText);
		ValueBindingBuilder.bind(existingAppNameTextObservable).to(appNameModelObservable).in(dbc);
		// disable the project name text when the model state is set to 'new project'
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(existingAppNameText))
				.notUpdating(useExistingAppObservable).converting(new InvertingBooleanConverter()).in(dbc);
		// move focus to the project name text control when choosing the 'Use an existing project' option.
		useExistingAppBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				existingAppNameText.setFocus();
				existingAppNameText.selectAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		// let's provide content assist on the existing project name
		ControlDecoration dec = new ControlDecoration(existingAppNameText, SWT.TOP | SWT.LEFT);
		FieldDecoration contentProposalFieldIndicator = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(contentProposalFieldIndicator.getImage());
		dec.setDescriptionText("Auto-completion is enabled when you start typing a project name.");
		dec.setShowOnlyOnFocus(true);

		AutoCompleteField adapter = new AutoCompleteField(existingAppNameText, new TextContentAdapter(),
				new String[] {});

		//adapter.setProposals(getOpenProjectsInWorkspace());

		Button browseAppsButton = new Button(existingAppSelectionGroup, SWT.NONE);
		browseAppsButton.setText("Browse");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).span(1, 1).grab(false, false)
				.applyTo(browseAppsButton);
		browseAppsButton.addSelectionListener(onBrowseApps());
		ValueBindingBuilder.bind(WidgetProperties.enabled().observe(browseAppsButton))
				.notUpdating(useExistingAppObservable).converting(new InvertingBooleanConverter()).in(dbc);

		final IObservableValue existingProjectValidityObservable = BeanProperties.value(
				ProjectAndServerAdapterSettingsWizardPageModel.PROPERTY_EXISTING_PROJECT_VALIDITY).observe(pageModel);
		final UseExistingApplicationValidator existingProjectValidator = new UseExistingApplicationValidator(
				existingProjectValidityObservable);
		dbc.addValidationStatusProvider(existingProjectValidator);
		ControlDecorationSupport.create(existingProjectValidator, SWT.LEFT | SWT.TOP);

		return existingAppSelectionGroup;
	}

	private SelectionListener onBrowseApps() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Creates the SWT Group in which a table which will display the existing applications with their corresponding
	 * type. Before each application, a radio button will let the user choose which application to import in his
	 * workspace.
	 * 
	 * @param container
	 * @param dbc
	 */
	private void createApplicationCreationGroup(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(parent);

		Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText("Na&me");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);
		Text nameText = new Text(parent, SWT.BORDER);
		nameText.setTextLimit(13);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(nameText);
		Binding nameBinding = dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(nameText),
				BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_NAME).observe(pageModel),
				new UpdateValueStrategy().setAfterGetValidator(new ApplicationNameValidator()), null);
		ControlDecorationSupport.create(nameBinding, SWT.LEFT | SWT.TOP);

		Label cartridgeLabel = new Label(parent, SWT.WRAP);
		cartridgeLabel.setText("&Application Type");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(cartridgeLabel);
		Combo cartridgesCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cartridgesCombo);
		dbc.bindList(WidgetProperties.items().observe(cartridgesCombo),
				BeanProperties.list(NewApplicationWizardPageModel.PROPERTY_CARTRIDGES).observe(pageModel),
				new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER),
				new UpdateListStrategy().setConverter(new Converter(Object.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (!(fromObject instanceof ICartridge)) {
							return null;
						}
						return ((ICartridge) fromObject).getName();
					}
				}));
		Binding comboSelectionBinding = dbc.bindValue(WidgetProperties.selection().observe(cartridgesCombo),
				BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(pageModel),
				new UpdateValueStrategy().setConverter(new Converter(String.class, ICartridge.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof String && ((String) fromObject).length() > 0) {
							return new Cartridge(((String) fromObject));
						}
						return null;
					}
				}).setAfterGetValidator(new IValidator() {

					@Override
					public IStatus validate(Object value) {
						if (!(value instanceof String) || ((String) value).length() == 0) {
							return ValidationStatus.error("You have to select a type");
						} else {
							return ValidationStatus.ok();
						}
					}
				}), new UpdateValueStrategy().setConverter(new Converter(ICartridge.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof ICartridge) {
							return ((ICartridge) fromObject).getName();
						}
						return null;
					}
				}));
		ControlDecorationSupport.create(comboSelectionBinding, SWT.LEFT | SWT.TOP);

	}

	private SelectionAdapter onRefresh(final DataBindingContext dbc) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					WizardUtils.runInWizard(new Job("Loading applications...") {

						@Override
						protected IStatus run(IProgressMonitor monitor) {
							return Status.OK_STATUS;
						}

					}, getContainer(), getDataBindingContext());

				} catch (Exception e) {
					// ignore
				}
			}
		};
	}

	@Override
	protected void onPageActivated(final DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading applications...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						final Collection<IApplication> applications = pageModel.getApplications();
						return Status.OK_STATUS;
					} catch (NotFoundOpenShiftException e) {
						// no domain and therefore no applications present
						return Status.OK_STATUS;
					} catch (Exception e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not load applications",
								e);
					}
				}

			}, getContainer(), getDataBindingContext());

		} catch (Exception ex) {
			// ignore
		}
	}

	class UseExistingApplicationValidator extends MultiValidator {

		private final IObservableValue existingProjectValidityObservable;

		public UseExistingApplicationValidator(IObservableValue existingProjectValidityObservable) {
			this.existingProjectValidityObservable = existingProjectValidityObservable;
		}

		@Override
		protected IStatus validate() {
			final IStatus existingProjectValidityStatus = (IStatus) existingProjectValidityObservable.getValue();

			if (existingProjectValidityStatus != null) {
				final IStatus existingProjectValidity = pageModel.getExistingApplicationValidity();
				return existingProjectValidity;
			}
			return ValidationStatus.ok();
		}

	}
	
	private class ApplicationNameValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			String name = (String) value;
			if (name.length() == 0) {
				return ValidationStatus.error("You have to provide a name");
			} else if (pageModel.hasApplication(name)) {
				return ValidationStatus.error(NLS.bind(
						"Names must be unique. You already have an application named \"{0}\"", name));
			}
			return ValidationStatus.ok();
		}
	}

}
