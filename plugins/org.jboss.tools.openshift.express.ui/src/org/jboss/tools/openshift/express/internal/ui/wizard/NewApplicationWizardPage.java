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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateListStrategy;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.client.Cartridge;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.OpenShiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;

/**
 * @author André Dietisheim
 */
public class NewApplicationWizardPage extends AbstractOpenShiftWizardPage {

	private NewApplicationWizardPageModel model;

	public NewApplicationWizardPage(NewApplicationWizardPageModel model, IWizard wizard) {
		super("Create new OpenShift Express application", "Create new OpenShift Express application",
				"Create new OpenShift Express application", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).applyTo(parent);

		Label nameLabel = new Label(parent, SWT.NONE);
		nameLabel.setText("Na&me");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(nameLabel);
		Text nameText = new Text(parent, SWT.BORDER);
		nameText.setTextLimit(13);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(nameText);
		Binding nameBinding = dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(nameText)
				, BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_NAME).observe(model)
				, new UpdateValueStrategy().setAfterGetValidator(new ApplicationNameValidator())
				, null);
		ControlDecorationSupport.create(nameBinding, SWT.LEFT | SWT.TOP);

		Label cartridgeLabel = new Label(parent, SWT.WRAP);
		cartridgeLabel.setText("&Application Type");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(cartridgeLabel);
		Combo cartridgesCombo = new Combo(parent, SWT.BORDER |
				SWT.READ_ONLY);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(cartridgesCombo);
		dbc.bindList(
				WidgetProperties.items().observe(cartridgesCombo)
				, BeanProperties.list(NewApplicationWizardPageModel.PROPERTY_CARTRIDGES).observe(model)
				, new UpdateListStrategy(UpdateListStrategy.POLICY_NEVER)
				, new UpdateListStrategy().setConverter(new Converter(Object.class, String.class) {

					@Override
					public Object convert(Object fromObject) {
						if (!(fromObject instanceof ICartridge)) {
							return null;
						}
						return ((ICartridge) fromObject).getName();
					}
				}));
		Binding comboSelectionBinding = dbc.bindValue(
				WidgetProperties.selection().observe(cartridgesCombo)
				, BeanProperties.value(NewApplicationWizardPageModel.PROPERTY_SELECTED_CARTRIDGE).observe(model)
				, new UpdateValueStrategy().setConverter(new Converter(String.class, ICartridge.class) {
					
					@Override
					public Object convert(Object fromObject) {
						if (fromObject instanceof String
								&& ((String) fromObject).length() > 0) {
							return new Cartridge(((String) fromObject));
						}
						return null;
					}
				}).setAfterGetValidator(
						new IValidator() {

							@Override
							public IStatus validate(Object value) {
								if (!(value instanceof String)
										|| ((String) value).length() == 0) {
									return ValidationStatus.error("You have to select a type");
								} else {
									return ValidationStatus.ok();
								}
							}
						})
				, new UpdateValueStrategy().setConverter(new Converter(ICartridge.class, String.class) {
							
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

	@Override
	protected void onPageActivated(DataBindingContext dbc) {
		try {
			WizardUtils.runInWizard(new Job("Loading cartridges...") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						model.loadCartridges();
					} catch (OpenShiftException e) {
						return new Status(IStatus.ERROR, OpenShiftUIActivator.PLUGIN_ID, "Could not load cartridges", e);
					}
					return Status.OK_STATUS;
				}
			}, getContainer());
		} catch (Exception e) {
			// ignore
		}
	}

	private class ApplicationNameValidator implements IValidator {

		@Override
		public IStatus validate(Object value) {
			String name = (String) value;
			if (name.length() == 0) {
				return ValidationStatus.error("You have to provide a name");
			} else if (model.hasApplication(name)) {
				return ValidationStatus.error(NLS.bind(
						"Names must be unique. You already have an application named \"{0}\"", name));
			}
			return ValidationStatus.ok();
		}
	}
}