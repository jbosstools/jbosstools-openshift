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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.foundation.ui.util.BrowserUtility;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftImages;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StyleRangeUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.AbstractDetailViews;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplateCategory;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IDownloadableCartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateDetailViews extends AbstractDetailViews {

	private final IDetailView defaultView = new DefaultView();
	private final IDetailView downloadableCartridgeView = new DownloadableCartridgeView();
	private final IDetailView quickstartView = new QuickstartView();

	private IObservableValue disabled;

	ApplicationTemplateDetailViews(IObservableValue detailViewModel, IObservableValue disabled, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, parent, dbc);
		this.disabled = disabled;
	}

	protected void createViewControls(Composite parent, DataBindingContext dbc) {
		downloadableCartridgeView.createControls(parent, dbc);
		quickstartView.createControls(parent, dbc);
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
		} else if (template instanceof IQuickstartApplicationTemplate) {
			return quickstartView;
		} else if (template instanceof IApplicationTemplateCategory) {
			return defaultView;
		} else {
			return emptyView;
		}
	}

	private class DefaultView extends Empty {

		private StyledText nameText;
		private Text descriptionText;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, dbc));
			GridLayoutFactory.fillDefaults()
					.margins(10, 10).spacing(10, 10).applyTo(container);

			// nameLink
			this.nameText = new StyledText(container, SWT.None);
			nameText.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// summaryText
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

	private class DownloadableCartridgeView extends DefaultView {

		private StyledText nameText;
		private Text descriptionText;
		private Text urlText;
		private Binding binding;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(container);

			// nameLink
			this.nameText = new StyledText(container, SWT.None);
			nameText.setEditable(false);
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// summaryText
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

			private DownloadableCartridgeUrlValidator(IObservableValue url, IObservableValue template, IObservableValue disabled) {
				this.url = url;
				this.applicationTemplate = template;
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
	
	private class QuickstartView extends Empty {

		private Link nameLink;
		private CLabel openshiftMaintainedLabel;
		private CLabel securityUpdatesLabel;
		private Text summaryText;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, dbc));
			GridLayoutFactory.fillDefaults()
					.numColumns(4).margins(10, 10).spacing(10, 10).applyTo(container);

			// nameLink
			this.nameLink = new Link(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(nameLink);

			// icons
			this.openshiftMaintainedLabel = new CLabel(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).applyTo(openshiftMaintainedLabel);
			this.securityUpdatesLabel = new CLabel(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).applyTo(securityUpdatesLabel);
			
			// summaryText
			this.summaryText = new Text(container, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			summaryText.setEditable(false);
			summaryText.setBackground(container.getBackground());
			GridDataFactory.fillDefaults()
					.span(3,1).align(SWT.LEFT, SWT.FILL).grab(true, true).applyTo(summaryText);
			return container;
		}

		@Override
		public void onVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			Object value = applicationTemplateObservable.getValue();
			if (!(value instanceof IQuickstartApplicationTemplate)
					|| DisposeUtils.isDisposed(nameLink)) {
				return;
			}
			IQuickstartApplicationTemplate template = (IQuickstartApplicationTemplate) value;
			this.nameLink.setText(new StringBuilder()
					.append("<a>").append(template.getName()).append("</a>").toString());
			nameLink.addSelectionListener(onLinkClicked(template.getHref()));
			updateOpenShiftMaintainedIcon(template);
			updateSecurityUpdatesIcon(template);
			this.summaryText.setText(template.getDescription());
		}

		private void updateOpenShiftMaintainedIcon(IQuickstartApplicationTemplate template) {
			if (template.isOpenShiftMaintained()) {
				setImageAndText(openshiftMaintainedLabel,
						"OpenShift maintained",
						OpenShiftImages.OPENSHIFT_MAINTAINED_IMG);
			} else {
				setImageAndText(openshiftMaintainedLabel,
						"Community created",
						OpenShiftImages.NOT_OPENSHIFT_MAINTAINED_IMG);
			}
		}
		
		private void updateSecurityUpdatesIcon(IQuickstartApplicationTemplate template) {
			if (template.isAutomaticSecurityUpdates()) {
				setImageAndText(securityUpdatesLabel, 
						"automatic security updates",
						OpenShiftImages.SECURITY_UPDATES_IMG);
			} else {
				setImageAndText(securityUpdatesLabel,
						"no automatic security updates",
						OpenShiftImages.NO_SECURITY_UPDATES_IMG);
			}
		}

		private void setImageAndText(CLabel label, String text, Image image) {
			label.setText(text);
			label.setImage(image);
			label.setToolTipText(text);
		}
		
		private SelectionListener onLinkClicked(final String url) {
			return new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					new BrowserUtility().checkedCreateExternalBrowser(url, OpenShiftUIActivator.PLUGIN_ID, OpenShiftUIActivator.getDefault().getLog());
				}

			};
		}
	}

}
