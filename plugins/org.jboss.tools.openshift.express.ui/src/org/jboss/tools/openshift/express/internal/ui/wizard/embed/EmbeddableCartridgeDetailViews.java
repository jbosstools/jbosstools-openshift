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
import org.jboss.tools.openshift.express.internal.core.util.StringUtils;
import org.jboss.tools.openshift.express.internal.core.util.UrlUtils;
import org.jboss.tools.openshift.express.internal.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.express.internal.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.OpenShiftResourceUtils;
import org.jboss.tools.openshift.express.internal.ui.utils.StyleRangeUtils;
import org.jboss.tools.openshift.express.internal.ui.viewer.AbstractDetailViews;

import com.openshift.client.cartridge.IEmbeddableCartridge;

/**
 * @author Andre Dietisheim
 */
public class EmbeddableCartridgeDetailViews extends AbstractDetailViews {

	public EmbeddableCartridgeDetailViews(IObservableValue detailViewModel, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, parent, dbc);
	}

	private final IDetailView cartridgeView = new CartridgeDetailsView();
	private final IDetailView downloadableCartridgeView = new DownloadableCartridge();

	@Override
	public void createViewControls(Composite parent, DataBindingContext dbc) {
		downloadableCartridgeView.createControls(parent, dbc);
		cartridgeView.createControls(parent, dbc);
	}

	protected IDetailView getView(IObservableValue selectedCartridgeObservable) {
		Object value = selectedCartridgeObservable.getValue();
		if (!(value instanceof IEmbeddableCartridge)) {
			return emptyView;
		}
		
		IEmbeddableCartridge cartridge = (IEmbeddableCartridge) value;
		if (cartridge.isDownloadable()) {
			return downloadableCartridgeView;
		} else {
			return cartridgeView;
		}
	}
	private class CartridgeDetailsView extends Empty {

		private StyledText nameLabel;
		private Text description;

		@Override
		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, dbc));
			GridLayoutFactory.fillDefaults()
					.margins(10, 10).spacing(10, 10).applyTo(container);

			// nameLabel			
			this.nameLabel = new StyledText(container, SWT.None);
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
			if (!(value instanceof IEmbeddableCartridge)
					|| DisposeUtils.isDisposed(nameLabel)) {
				return;
			}
			IEmbeddableCartridge embeddableCartridge = (IEmbeddableCartridge) value;
			String name = OpenShiftResourceUtils.toString(embeddableCartridge);
			this.nameLabel.setText(name);
			this.nameLabel.setStyleRange(StyleRangeUtils.createBoldStyleRange(name, description.getBackground()));

			this.description.setText(embeddableCartridge.getDescription());
		}
	}

	private class DownloadableCartridge extends CartridgeDetailsView {

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

			return container;
		}

		@Override
		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			Object value = selectedCartridgeObservable.getValue();
			if (!(value instanceof CodeAnythingCartridge)
					|| DisposeUtils.isDisposed(name)) {
				return;
			}
			CodeAnythingCartridge codeAnythingCartridge = (CodeAnythingCartridge) value;
			String name = codeAnythingCartridge.getDisplayName();
			this.name.setText(name);
			this.name.setStyleRange(StyleRangeUtils.createBoldStyleRange(name, description.getBackground()));
			this.description.setText(codeAnythingCartridge.getDescription());

			IObservableValue urlTextObservable = WidgetProperties.text(SWT.Modify).observeDelayed(100, urlText);
			this.binding = ValueBindingBuilder
					.bind(urlTextObservable)
					.to(BeanProperties.value(CodeAnythingCartridge.PROPERTY_URL_STRING, String.class)
							.observeDetail(selectedCartridgeObservable))
					.in(dbc);
			DownloadableCartridgeUrlValidator downloadableCartridgeUrlValidator =
					new DownloadableCartridgeUrlValidator(urlTextObservable, selectedCartridgeObservable);
			dbc.addValidationStatusProvider(downloadableCartridgeUrlValidator);
			ControlDecorationSupport.create(downloadableCartridgeUrlValidator,
					SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater());
		}

		
		@Override
		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
			if (DisposeUtils.isDisposed(binding)) {
				return;
			}
			binding.dispose();
		}


		class DownloadableCartridgeUrlValidator extends MultiValidator {

			private IObservableValue url;
			private IObservableValue selectedCartridge;

			private DownloadableCartridgeUrlValidator(IObservableValue url, IObservableValue applicationTemplate) {
				this.url = url;
				this.selectedCartridge = applicationTemplate;
			}

			@Override
			protected IStatus validate() {
				String url = (String) this.url.getValue();
				IEmbeddableCartridge embeddableCartridge = (IEmbeddableCartridge) this.selectedCartridge.getValue();
				
				if (embeddableCartridge == null
						|| !(embeddableCartridge instanceof IEmbeddableCartridge)
						|| !((IEmbeddableCartridge) embeddableCartridge).isDownloadable()) {
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
