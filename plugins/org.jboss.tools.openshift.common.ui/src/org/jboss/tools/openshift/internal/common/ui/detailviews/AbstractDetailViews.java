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
package org.jboss.tools.openshift.internal.common.ui.detailviews;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractDetailViews {

	protected final IDetailView emptyView = new EmptyView();
	
	private Composite parent;
	private IDetailView currentView = emptyView;
	private final StackLayout stackLayout = new StackLayout();
	private DataBindingContext dbc;
	private IObservableValue detailViewModel;

	public AbstractDetailViews(IObservableValue detailViewModel, Composite parent, DataBindingContext dbc) {
		Assert.isLegal(parent != null && !parent.isDisposed());
		this.parent = parent;
		parent.addDisposeListener(onDispose());
		Assert.isLegal(dbc != null);
		this.dbc = dbc;
		Assert.isLegal(detailViewModel != null && !detailViewModel.isDisposed());
		this.detailViewModel = detailViewModel;
	}

	public void createControls() {
		DataBindingUtils.addDisposableValueChangeListener(onDetailViewModelChanged(), detailViewModel, parent);

		detailViewModel.addValueChangeListener(onDetailViewModelChanged());
		parent.setLayout(stackLayout);
		createViewControls(parent, dbc);
		showView(detailViewModel, getView(detailViewModel), dbc);
	}

	protected abstract IDetailView[] getDetailViews();
	
	private IValueChangeListener onDetailViewModelChanged() {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				showView(event.getObservableValue(), dbc);
			}
		};
	}

	protected void showView(IObservableValue detailViewsModel, DataBindingContext dbc) {
		showView(detailViewsModel, getView(detailViewsModel), dbc);
	}

	protected void showView(IObservableValue detailViewsModel, IDetailView view, DataBindingContext dbc) {
		if (view == null
				|| view.getControl() == null
				|| detailViewsModel == null
				|| view.equals(currentView)) {
			return;
		}
		currentView.onInVisible(detailViewsModel, dbc);
		this.currentView = view;
		view.onVisible(detailViewsModel, dbc);
		stackLayout.topControl = view.getControl();
		parent.layout(true, true);
	}

	protected void createViewControls(Composite parent, DataBindingContext dbc) {
		emptyView.createControls(parent, dbc);
		for (IDetailView detailView : getDetailViews()) {
			detailView.createControls(parent, dbc);
		}
	};

	protected IDetailView getView(IObservableValue detailViewsModel) {
		return getViewFor(detailViewsModel, getDetailViews());
	};

	protected IDetailView getViewFor(IObservableValue detailViewsModel, IDetailView... detailViews) {
		Object value = detailViewsModel.getValue();
		IDetailView view = emptyView;

		for(IDetailView detailView : detailViews) {
			if (detailView.isViewFor(value)) {
				view = detailView;
				break;
			}
		}
		
		return view;
	}
	
	private DisposeListener onDispose() {
		return new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();
			}
		};
	}

	private void dispose() {
		for (IDetailView view : getDetailViews()) {
			if (view != null) {
				view.dispose();
			}
		}
	}

	public class EmptyView extends BaseDetailsView {

		public Composite createControls(Composite parent, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.NONE));
			GridLayoutFactory.fillDefaults()
					.margins(6, 6).spacing(6, 6).applyTo(container);
			return container;
		}
		
		public boolean isViewFor(Object object) {
			return false;
		}
	}
	
	public interface IDetailView {

		public Composite createControls(Composite parent, DataBindingContext dbc);

		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public Control getControl();
		
		public boolean isViewFor(Object object);

		public void dispose();
	}
}
