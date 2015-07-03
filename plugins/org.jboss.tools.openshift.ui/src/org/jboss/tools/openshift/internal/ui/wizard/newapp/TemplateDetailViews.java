/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;
import org.jboss.tools.openshift.internal.common.ui.utils.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftImages;

import com.openshift.restclient.model.template.ITemplate;

/**
 * A details view of templates
 * 
 * @author jeff.cantrill
 *
 */
public class TemplateDetailViews  extends AbstractStackedDetailViews {
	
	private final IDetailView templateView = new TemplateDetailView();
	
	public TemplateDetailViews(IObservableValue detailViewModel, IObservableValue disabled, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, null, parent, dbc);
	}
	
	@Override
	protected void createViewControls(Composite parent, Object context, DataBindingContext dbc) {
		templateView.createControls(parent, context, dbc);
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return new IDetailView[] {templateView };
	}
	
	private class TemplateDetailView extends EmptyView {
		private Binding binding;
		private StyledText txtDescription;
		private CLabel classIcon;
		
		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.None));
			GridLayoutFactory.fillDefaults()
					.margins(8, 2).numColumns(4).spacing(6, 2).applyTo(container);
			
			// icons
			this.classIcon = new CLabel(container, SWT.None);
			GridDataFactory.fillDefaults()
			.grab(false, true)
			.span(1, 3)
			.align(SWT.LEFT, SWT.TOP).applyTo(classIcon);
			
			// description
			this.txtDescription = new StyledText(container, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			txtDescription.setAlwaysShowScrollBars(false);
			txtDescription.setWordWrap(true);
			UIUtils.setTransparent(txtDescription);
			GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.FILL)
				.grab(true, true)
				.span(3, 1)
				.applyTo(txtDescription);
			
			return container;
		}
		
		@Override
		public void onVisible(IObservableValue templateObservable, DataBindingContext dbc) {
			Object value = templateObservable.getValue();
			txtDescription.setText("");
			if (!(value instanceof ITemplate) || DisposeUtils.isDisposed(txtDescription)) {
				return;
			}
			ITemplate template = (ITemplate) value;
			if(template.isAnnotatedWith("provider")) {
				txtDescription.append(NLS.bind("Provider: {0} \n", template.getAnnotation("provider")));
			}
			Map<String, String> annotations = template.getAnnotations();
			addTextFor("description", annotations, txtDescription);
			updateImage(annotations.get("iconClass"));
		}
		
		private void updateImage(String iconClass) {
			Image image = iconClass == null ? null : OpenShiftImages.getAppImage(iconClass);
			classIcon.setImage(image);
		}

		private void addTextFor(String annotation, Map<String, String> annotations,  StyledText text) {
			text.append((String)ObjectUtils.defaultIfNull(annotations.get(annotation),""));
		}

		@Override
		public void onInVisible(IObservableValue templateObservable, DataBindingContext dbc) {
			DataBindingUtils.dispose(binding);
		}
		
		@Override
		public boolean isViewFor(Object object) {
			return object instanceof ITemplate;
		}
	}
}
