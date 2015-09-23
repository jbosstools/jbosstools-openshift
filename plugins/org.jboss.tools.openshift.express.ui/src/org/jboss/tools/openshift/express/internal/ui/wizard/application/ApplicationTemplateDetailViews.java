/*******************************************************************************
 * boright (c) 2014 Red Hat, Inc.
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
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
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
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.core.preferences.ExpressCorePreferences;
import org.jboss.tools.openshift.express.internal.ui.ExpressImages;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.ContentProposalUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICartridgeApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.ICodeAnythingApplicationTemplate;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IQuickstartApplicationTemplate;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;

/**
 * @author Andre Dietisheim
 */
public class ApplicationTemplateDetailViews extends AbstractStackedDetailViews {

	private final IDetailView defaultView = new DefaultView();
	private final IDetailView downloadableCartridgeView = new CodeAnthingCartridgeView();
	private final IDetailView quickstartView = new QuickstartView();

	private IObservableValue disabled;

	ApplicationTemplateDetailViews(IObservableValue detailViewModel, IObservableValue disabled, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, null, parent, dbc);
		this.disabled = disabled;
	}

	@Override
	protected void createViewControls(Composite parent, Object context, DataBindingContext dbc) {
		downloadableCartridgeView.createControls(parent, context, dbc);
		quickstartView.createControls(parent, context, dbc);
		defaultView.createControls(parent, context, dbc);
		emptyView.createControls(parent, context, dbc);
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return new IDetailView[] { quickstartView, downloadableCartridgeView, defaultView };
	}

	private class DefaultView extends EmptyView {

		private StyledText nameText;
		private StyledText descriptionText;

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, context, dbc));
			GridLayoutFactory.fillDefaults()
					.margins(8, 2).spacing(6, 2).applyTo(container);

			// nameLink
			this.nameText = new StyledText(container, SWT.READ_ONLY);
			nameText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(nameText);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// summaryText
			this.descriptionText = new StyledText(container, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			descriptionText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(descriptionText);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, true).hint(container.getClientArea().x, SWT.DEFAULT).applyTo(descriptionText);
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
			this.nameText.setStyleRange(StyledTextUtils.createBoldStyle(templateName, null));
			this.descriptionText.setText(applicationTemplate.getDescription());
		}

		@Override
		public boolean isViewFor(Object object) {
			return object instanceof ICartridgeApplicationTemplate;
		}
	}

	private class CodeAnthingCartridgeView extends DefaultView {

		private StyledText nameText;
		private StyledText descriptionText;
		private Text urlText;
		private Binding binding;

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.numColumns(2).margins(8, 2).spacing(6, 2).applyTo(container);

			// nameLink
			this.nameText = new StyledText(container, SWT.READ_ONLY);
			StyledTextUtils.setTransparent(nameText);
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// summaryText
			this.descriptionText = new StyledText(container, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			descriptionText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(descriptionText);
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).hint(container.getClientArea().x, SWT.DEFAULT).applyTo(descriptionText);

			// url
			Label urlLabel = new Label(container, SWT.None);
			urlLabel.setText("Cartridge URL:");
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(urlLabel);
			this.urlText = new Text(container, SWT.BORDER);
			createContentProposal(urlText);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(urlText);

			return container;
		}

		private void createContentProposal(Text text) {
			final ControlDecoration decoration = ContentProposalUtils.createContenProposalDecoration("History available", text);
			ContentProposalUtils.createContentProposal(text, ExpressCorePreferences.INSTANCE.getDownloadableStandaloneCartUrls());
			text.addFocusListener(new FocusAdapter() {

				@Override
				public void focusGained(FocusEvent e) {
					decoration.show();
				}

				@Override
				public void focusLost(FocusEvent e) {
					decoration.hide();
				}
			});
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
			this.nameText.setStyleRange(StyledTextUtils.createBoldStyle(name, null));
			this.descriptionText.setText(applicationTemplate.getDescription());

			IObservableValue urlTextObservable = WidgetProperties.text(SWT.Modify).observe(urlText);
			this.binding = ValueBindingBuilder
					.bind(urlTextObservable)
					.to(BeanProperties.value(ICodeAnythingApplicationTemplate.PROPERTY_CARTRIDGE_URL, String.class)
							.observeDetail(applicationTemplateObservable))
					.in(dbc);
			CodeAnythingCartridgeUrlValidator codeAnythingCartridgeUrlValidator =
					new CodeAnythingCartridgeUrlValidator(urlTextObservable, applicationTemplateObservable, disabled);
			dbc.addValidationStatusProvider(codeAnythingCartridgeUrlValidator);
			ControlDecorationSupport.create(codeAnythingCartridgeUrlValidator,
					SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		}

		
		@Override
		public void onInVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			DataBindingUtils.dispose(binding);
		}


		class CodeAnythingCartridgeUrlValidator extends MultiValidator {

			private IObservableValue url;
			private IObservableValue disabled;
			private IObservableValue applicationTemplate;

			private CodeAnythingCartridgeUrlValidator(IObservableValue url, IObservableValue template, IObservableValue disabled) {
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
						|| !(applicationTemplate instanceof ICodeAnythingApplicationTemplate)) {
					return ValidationStatus.ok();
				}
				
				if (StringUtils.isEmpty(url)) {
					return ValidationStatus
							.cancel("Please provide an url for your cartridge.");
				}
				if (!UrlUtils.isValid(url)
						&& !EGitUtils.isValidGitUrl(url)) {
					return ValidationStatus.error(NLS.bind("{0} is not a valid url.", url));
				}
				return ValidationStatus.ok();
			}
		}

		@Override
		public boolean isViewFor(Object object) {
			return object instanceof ICodeAnythingApplicationTemplate;
		}

	}
	
	private class QuickstartView extends EmptyView {

		private Link nameLink;
		private CLabel openshiftMaintainedLabel;
		private CLabel securityUpdatesLabel;
		// use styled text to have vertical scrollbars hidden/visible correctly
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=180027#c11
		private StyledText summaryText;
		private IQuickstartApplicationTemplate template;

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.margins(8, 2).numColumns(4).spacing(6, 2).applyTo(container);

			// nameLink
			this.nameLink = new Link(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(nameLink);
			nameLink.addSelectionListener(onLinkClicked());


			// icons
			this.openshiftMaintainedLabel = new CLabel(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).applyTo(openshiftMaintainedLabel);
			this.securityUpdatesLabel = new CLabel(container, SWT.None);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).applyTo(securityUpdatesLabel);
			
			// summaryText
			this.summaryText = new StyledText(container, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			summaryText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(summaryText);
			GridDataFactory.fillDefaults()
					.span(3, 1).align(SWT.FILL, SWT.FILL).grab(true, true).hint(container.getClientArea().x, SWT.DEFAULT).applyTo(summaryText);
			return container;
		}

		@Override
		public void onVisible(IObservableValue applicationTemplateObservable, DataBindingContext dbc) {
			Object value = applicationTemplateObservable.getValue();
			if (!(value instanceof IQuickstartApplicationTemplate)
					|| DisposeUtils.isDisposed(nameLink)) {
				return;
			}
			this.template = (IQuickstartApplicationTemplate) value;
			this.nameLink.setText(new StringBuilder()
					.append("<a>").append(template.getName()).append("</a>").toString());
			nameLink.setEnabled(template.hasPageUrl());
			updateOpenShiftMaintainedIcon(template);
			updateSecurityUpdatesIcon(template);
			this.summaryText.setText(template.getDescription());
		}
		
		private void updateOpenShiftMaintainedIcon(IQuickstartApplicationTemplate template) {
			if (template.isOpenShiftMaintained()) {
				setImageAndTooltip(openshiftMaintainedLabel,
						"OpenShift maintained",
						ExpressImages.OPENSHIFT_MAINTAINED_IMG);
			} else {
				setImageAndTooltip(openshiftMaintainedLabel,
						"Community created",
						ExpressImages.NOT_OPENSHIFT_MAINTAINED_IMG);
			}
		}
		
		private void updateSecurityUpdatesIcon(IQuickstartApplicationTemplate template) {
			if (template.isAutomaticSecurityUpdates()) {
				setImageAndTooltip(securityUpdatesLabel, 
						"automatic security updates",
						ExpressImages.SECURITY_UPDATES_IMG);
			} else {
				setImageAndTooltip(securityUpdatesLabel,
						"no automatic security updates",
						ExpressImages.NO_SECURITY_UPDATES_IMG);
			}
		}

		private void setImageAndTooltip(CLabel label, String text, Image image) {
			// label.setText(text);
			label.setImage(image);
			label.setToolTipText(text);
		}
		
		private SelectionListener onLinkClicked() {
			return new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					if (template == null) {
						return;
					}
					new BrowserUtility().checkedCreateExternalBrowser(template.getPageUrl(), ExpressUIActivator.PLUGIN_ID, ExpressUIActivator.getDefault().getLog());
				}

			};
		}

		@Override
		public boolean isViewFor(Object object) {
			return object instanceof IQuickstartApplicationTemplate;
		}
	}
}
