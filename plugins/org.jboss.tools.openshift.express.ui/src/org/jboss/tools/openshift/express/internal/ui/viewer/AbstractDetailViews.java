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
package org.jboss.tools.openshift.express.internal.ui.viewer;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractDetailViews {

	protected final IDetailView emptyView = new Empty();
	
	private Composite parent;
	private IDetailView currentView = emptyView;
	private final StackLayout stackLayout = new StackLayout();
	private DataBindingContext dbc;
	private IObservableValue detailViewModel;

	public AbstractDetailViews(IObservableValue detailViewModel, Composite parent, DataBindingContext dbc) {
		Assert.isLegal(parent != null && !parent.isDisposed());
		this.parent = parent;
		Assert.isLegal(dbc != null);
		this.dbc = dbc;
		Assert.isLegal(detailViewModel != null && !detailViewModel.isDisposed());
		this.detailViewModel = detailViewModel;
	}

	public void createControls() {
		detailViewModel.addValueChangeListener(onDetailViewModelChanged());
		parent.setLayout(stackLayout);
		createViewControls(parent, dbc);
		showView(null, currentView, dbc);
	}

	private IValueChangeListener onDetailViewModelChanged() {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				showView(event.getObservableValue(), dbc);
			}
		};
	}

	protected void showView(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
		showView(selectedCartridgeObservable, getView(selectedCartridgeObservable), dbc);
	}

	protected void showView(IObservableValue selectedCartridgeObservable, IDetailView view, DataBindingContext dbc) {
		if (view == null
				|| view.getControl() == null) {
			return;
		}
		if (selectedCartridgeObservable == null) {
			return;
		}
		currentView.onInVisible(selectedCartridgeObservable, dbc);
		view.onVisible(selectedCartridgeObservable, dbc);
		stackLayout.topControl = view.getControl();
		parent.layout(true, true);
	}

	protected class Empty extends BaseDetailsView {

		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.NONE));
			GridLayoutFactory.fillDefaults()
					.margins(6, 6).spacing(6, 6).applyTo(container);
			return container;
		}
	}

	private abstract class BaseDetailsView implements IDetailView {

		private Composite control;

		@Override
		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
		}

		@Override
		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc) {
		}

		@Override
		public Control getControl() {
			return control;
		}

		protected Composite setControl(Composite composite) {
			this.control = composite;
			return composite;
		}
	}
	
	protected abstract void createViewControls(Composite parent, DataBindingContext dbc);

	protected abstract IDetailView getView(IObservableValue selectedCartridgeObservable);

	public interface IDetailView {

		public Composite createControls(Composite parent, DataBindingContext dbc);

		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public Control getControl();

	}
}
