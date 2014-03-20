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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StyleRangeUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.AbstractDetailViews;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplateCategory;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IDownloadableCartridgeApplicationTemplate;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateDetailViews extends AbstractDetailViews {

	private final IDetailView defaultView = new Default();
	private final IDetailView downloadableCartridgeView = new DownloadableCartridge();

	private IObservableValue disabled;

	ApplicationTemplateDetailViews(IObservableValue detailViewModel, IObservableValue disabled, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, parent, dbc);
		this.disabled = disabled;
	}

	protected void createViewControls(Composite parent, DataBindingContext dbc) {
		downloadableCartridgeView.createControls(parent, dbc);
		defaultView.createControls(parent, dbc);
		emptyView.createControls(parent, dbc);
	}

	protected IDetailView getView(IObservableValue applicationTemplateObservable) {
		Object value = applicationTemplateObservable.getValue();
		if (!(value instanceof IApplicationTemplate)) {
			return emptyView;
		}
		
		IApplicationTemplate template = (IApplicationTemplate) value;
		if (template instanceof IDownloadableCartridgeApplicationTemplate) {
			return downloadableCartridgeView;
		} else if (template instanceof ICartridgeApplicationTemplate) {
			return defaultView;
		} else if (template instanceof IApplicationTemplateCategory) {
			return defaultView;
		} else {
			return emptyView;
		}
	}

	private class Default extends Empty {

		private StyledText nameText;
		private Text descriptionText;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, dbc));
			GridLayoutFactory.fillDefaults()
					.margins(10, 10).spacing(10, 10).applyTo(container);

			// nameText
			this.nameText = new StyledText(container, SWT.None);
			nameText.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// descriptionText
			this.descriptionText = new Text(container, SWT.MULTI | SWT.WRAP);
			descriptionText.setEditable(false);
			descriptionText.setBackground(container.getBackground());
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.FILL).grab(true, true).applyTo(descriptionText);
			return container;
		}

		@Override
		public void onVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			Object value = applicationTemplateObservable.getValue();
			if (!(value instanceof IApplicationTemplate)
					|| DisposeUtils.isDisposed(nameText)) {
				return;
			}
			IApplicationTemplate applicationTemplate = (IApplicationTemplate) value;
			String templateName = applicationTemplate.getName();
			this.nameText.setText(templateName);
			this.nameText.setStyleRange(StyleRangeUtils.createBoldStyleRange(templateName, descriptionText.getBackground()));
			this.descriptionText.setText(applicationTemplate.getDescription());
		}
	}

	private class DownloadableCartridge extends Default {

		private StyledText nameText;
		private Text descriptionText;
		private Text urlText;
		private Binding binding;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(container);

			// nameText
			this.nameText = new StyledText(container, SWT.None);
			nameText.setEditable(false);
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// descriptionText
			this.descriptionText = new Text(container, SWT.MULTI | SWT.WRAP);
			descriptionText.setEditable(false);
			descriptionText.setBackground(container.getBackground());
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.LEFT, SWT.FILL).grab(true, true).applyTo(descriptionText);

			// url
			Label urlLabel = new Label(container, SWT.None);
			urlLabel.setText("Cartridge URL:");
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(urlLabel);
			this.urlText = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(urlText);

			return container;
		}

		@Override
		public void onVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			Object value = applicationTemplateObservable.getValue();
			if (!(value instanceof IApplicationTemplate)
					|| DisposeUtils.isDisposed(nameText)) {
				return;
			}
			IApplicationTemplate applicationTemplate = (IApplicationTemplate) value;
			String name = applicationTemplate.getName();
			this.nameText.setText(name);
			this.nameText.setStyleRange(StyleRangeUtils.createBoldStyleRange(name, descriptionText.getBackground()));
			this.descriptionText.setText(applicationTemplate.getDescription());

			IObservableValue urlTextObservable = WidgetProperties.text(SWT.Modify).observe(urlText);
			this.binding = ValueBindingBuilder
					.bind(urlTextObservable)
					.to(BeanProperties.value(IDownloadableCartridgeApplicationTemplate.PROPERTY_CARTRIDGE_URL, String.class)
							.observeDetail(applicationTemplateObservable))
					.in(dbc);
			DownloadableCartridgeUrlValidator downloadableCartridgeUrlValidator =
					new DownloadableCartridgeUrlValidator(urlTextObservable, applicationTemplateObservable, disabled);
			dbc.addValidationStatusProvider(downloadableCartridgeUrlValidator);
			ControlDecorationSupport.create(downloadableCartridgeUrlValidator,
					SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		}

		
		@Override
		public void onInVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			if (DisposeUtils.isDisposed(binding)) {
				return;
			}
			binding.dispose();
		}


		class DownloadableCartridgeUrlValidator extends MultiValidator {

			private IObservableValue url;
			private IObservableValue disabled;
			private IObservableValue applicationTemplate;

			private DownloadableCartridgeUrlValidator(IObservableValue url, IObservableValue applicationTemplate, IObservableValue disabled) {
				this.url = url;
				this.applicationTemplate = applicationTemplate;
				this.disabled = disabled;
			}

			@Override
			protected IStatus validate() {
				String url = (String) this.url.getValue();
				IApplicationTemplate applicationTemplate = (IApplicationTemplate) this.applicationTemplate.getValue();
				Boolean disabled = (Boolean) this.disabled.getValue();
				
				if (Boolean.valueOf(disabled)) {
					return ValidationStatus.ok();
				}
				
				if (applicationTemplate == null
						|| !(applicationTemplate instanceof IDownloadableCartridgeApplicationTemplate)) {
					return ValidationStatus.ok();
				}
				
				if (StringUtils.isEmpty(url)) {
					return ValidationStatus
							.cancel("Please provide an url for your cartridge.");
				}
				if (!UrlUtils.isValid(url)) {
					return ValidationStatus.error(NLS.bind("{0} is not a valid url.", url));
				}
				return ValidationStatus.ok();
			}
			
		}
	
	}
}
