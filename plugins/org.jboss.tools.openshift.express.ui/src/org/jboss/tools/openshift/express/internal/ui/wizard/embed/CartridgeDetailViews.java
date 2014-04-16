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
package org.jboss.tools.openshift.express.internal.ui.wizard.embed;

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
import org.jboss.tools.openshift.express.core.CodeAnythingCartridge;
import org.jboss.tools.openshift.express.internal.core.util.OpenShiftResourceLabelUtils;
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StyleRangeUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.AbstractDetailViews;

import com.openshift.client.cartridge.ICartridge;

/**
 * @author Andre Dietisheim
 */
public class CartridgeDetailViews extends AbstractDetailViews {

	private IObservableValue canModifyCartridges;

	public CartridgeDetailViews(IObservableValue detailViewModel, IObservableValue canModifyCartridges, 	
			Composite parent, DataBindingContext dbc) {
		super(detailViewModel, parent, dbc);
		this.canModifyCartridges = canModifyCartridges;
	}

	private final IDetailView cartridgeView = new CartridgeDetailsView();
	private final IDetailView downloadableCartridgeView = new DownloadableCartridgeView();
	private final IDetailView codeAnythingCartridgeView = new CodeAnythingDetailsView();

	@Override
	public void createViewControls(Composite parent, DataBindingContext dbc) {
		cartridgeView.createControls(parent, dbc);
		downloadableCartridgeView.createControls(parent, dbc);
		codeAnythingCartridgeView.createControls(parent, dbc);
	}

	protected IDetailView getView(IObservableValue selectedCartridgeObservable) {
		Object value = selectedCartridgeObservable.getValue();
		if (!(value instanceof ICartridge)) {
			return emptyView;
		}
		
		ICartridge cartridge = (ICartridge) value;
		if (cartridge instanceof CodeAnythingCartridge) {
			return codeAnythingCartridgeView;
		} else if (cartridge.isDownloadable()) {
			return downloadableCartridgeView;
		} else {
			return cartridgeView;
		}
	}
	
	private class CartridgeDetailsView extends EmptyView {

		private StyledText nameLabel;
		private Text description;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, dbc));
			GridLayoutFactory.fillDefaults()
					.margins(10, 10).spacing(10, 10).applyTo(container);

			// nameLabel			
			this.nameLabel = new StyledText(container, SWT.None);
			nameLabel.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameLabel);

			// description
			this.description = new Text(container, SWT.MULTI | SWT.WRAP);
			description.setEditable(false);
			description.setBackground(container.getBackground());
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.FILL).grab(true, true).applyTo(description);
			return container;
		}

		@Override
		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			Object value = selectedCartridgeObservable.getValue();
			if (!(value instanceof ICartridge)
					|| DisposeUtils.isDisposed(nameLabel)) {
				return;
			}
			ICartridge embeddableCartridge = (ICartridge) value;
			String name = OpenShiftResourceLabelUtils.toString(embeddableCartridge);
			this.nameLabel.setText(name);
			this.nameLabel.setStyleRange(StyleRangeUtils.createBoldStyleRange(name, description.getBackground()));

			this.description.setText(embeddableCartridge.getDescription());
		}
	}

	private class DownloadableCartridgeView extends CartridgeDetailsView {

		private StyledText name;
		private Text url;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.margins(10, 10).spacing(10, 10).applyTo(container);

			// name
			this.name = new StyledText(container, SWT.None);
			name.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(name);

			// url
			this.url = new Text(container, SWT.WRAP);
			url.setEditable(false);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.TOP).grab(true, false).applyTo(url);

			return container;
		}

		@Override
		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			Object value = selectedCartridgeObservable.getValue();
			if (!(value instanceof ICartridge)
					|| DisposeUtils.isDisposed(name)) {
				return;
			}
			
			ICartridge cartridge = (ICartridge) value;
			String cartridgeLabel = OpenShiftResourceLabelUtils.toString(cartridge);
			this.name.setText(cartridgeLabel);
			this.name.setStyleRange(StyleRangeUtils.createBoldStyleRange(cartridgeLabel, url.getBackground()));
			if (cartridge.getUrl() != null) {
				this.url.setText(cartridge.getUrl().toString());
			}
		}
	}

	private class CodeAnythingDetailsView extends CartridgeDetailsView {

		private StyledText name;
		private Text description;
		private Text urlText;
		private Binding binding;
		
		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.numColumns(2).margins(10, 10).spacing(10, 10).applyTo(container);

			// name
			this.name = new StyledText(container, SWT.None);
			name.setEditable(false);
			GridDataFactory.fillDefaults()
					.span(2,1).align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(name);

			// description
			this.description = new Text(container, SWT.MULTI | SWT.WRAP);
			description.setEditable(false);
			description.setBackground(container.getBackground());
			GridDataFactory.fillDefaults()
					.span(2, 1).align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(description);

			// url
			Label urlLabel = new Label(container, SWT.None);
			urlLabel.setText("Cartridge URL:");
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).applyTo(urlLabel);
			this.urlText = new Text(container, SWT.BORDER);
			GridDataFactory.fillDefaults()
					.align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(urlText);

			ValueBindingBuilder
					.bind(WidgetProperties.enabled().observe(urlText))
					.notUpdatingParticipant()
					.to(canModifyCartridges);

			return container;
		}

		@Override
		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			Object value = selectedCartridgeObservable.getValue();
			if (!(value instanceof CodeAnythingCartridge)
					|| DisposeUtils.isDisposed(name)) {
				return;
			}
			CodeAnythingCartridge cartridge = (CodeAnythingCartridge) value;
			String name = cartridge.getDisplayName();
			this.name.setText(name);
			this.name.setStyleRange(StyleRangeUtils.createBoldStyleRange(name, description.getBackground()));
			this.description.setText(cartridge.getDescription());

			IObservableValue urlTextObservable = WidgetProperties.text(SWT.Modify).observeDelayed(100, urlText);
			this.binding = ValueBindingBuilder
					.bind(urlTextObservable)
					.to(BeanProperties.value(CodeAnythingCartridge.PROPERTY_URL_STRING, String.class)
							.observeDetail(selectedCartridgeObservable))
					.in(dbc);
			CodeAnythingUrlValidator codeAnythingUrlValidator =
					new CodeAnythingUrlValidator(urlTextObservable, selectedCartridgeObservable);
			dbc.addValidationStatusProvider(codeAnythingUrlValidator);
			ControlDecorationSupport.create(codeAnythingUrlValidator,
					SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		}

		@Override
		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			if (DisposeUtils.isDisposed(binding)) {
				return;
			}
			binding.dispose();
		}

		class CodeAnythingUrlValidator extends MultiValidator {

			private IObservableValue url;
			private IObservableValue selectedCartridge;

			private CodeAnythingUrlValidator(IObservableValue url, IObservableValue applicationTemplate) {
				this.url = url;
				this.selectedCartridge = applicationTemplate;
			}

			@Override
			protected IStatus validate() {
				String url = (String) this.url.getValue();
				ICartridge cartridge = (ICartridge) this.selectedCartridge.getValue();
				
				if (!(cartridge instanceof CodeAnythingCartridge)) {
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
