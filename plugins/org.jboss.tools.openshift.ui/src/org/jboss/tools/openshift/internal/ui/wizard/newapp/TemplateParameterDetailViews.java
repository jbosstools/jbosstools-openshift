/*******************************************************************************
 * boright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.newapp;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;
import org.jboss.tools.openshift.internal.common.ui.utils.DisposeUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;

import com.openshift.restclient.model.template.IParameter;

/**
 * Views that show the details for a given {@code IParameter}.
 * 
 * @author Andre Dietisheim
 */
public class TemplateParameterDetailViews extends AbstractStackedDetailViews {

	private final IDetailView parameterView = new ParameterView();

	TemplateParameterDetailViews(IObservableValue parameterObservable, Composite parent, DataBindingContext dbc) {
		super(parameterObservable, null, parent, dbc);
	}

	@Override
	protected void createViewControls(Composite parent, Object context, DataBindingContext dbc) {
		parameterView.createControls(parent, context, dbc);
		emptyView.createControls(parent, context, dbc);
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return new IDetailView[] { emptyView, parameterView };
	}

	private class ParameterView extends EmptyView {

		private StyledText nameText;
		private StyledText descriptionText;

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(super.createControls(parent, context, dbc));
			GridLayoutFactory.fillDefaults().margins(8, 2).spacing(6, 2).applyTo(container);

			// nameLink
			this.nameText = new StyledText(container, SWT.READ_ONLY);
			nameText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(nameText);
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(nameText);

			// descriptionText
			this.descriptionText = new StyledText(container, SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
			descriptionText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(descriptionText);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true)
					.hint(container.getClientArea().x, SWT.DEFAULT).applyTo(descriptionText);
			return container;
		}

		@Override
		public void onVisible(IObservableValue parameterObservable, DataBindingContext dbc) {
			Object value = parameterObservable.getValue();
			if (!(value instanceof IParameter) || DisposeUtils.isDisposed(nameText)) {
				return;
			}
			IParameter parameter = (IParameter) value;
			String name = parameter.getName();
			this.nameText.setText(name);
			this.nameText.setStyleRange(StyledTextUtils.createBoldStyleRange(name, null));
			this.descriptionText.setText(parameter.getDescription());
		}

		@Override
		public boolean isViewFor(Object object) {
			return object instanceof IParameter;
		}
	}

}
