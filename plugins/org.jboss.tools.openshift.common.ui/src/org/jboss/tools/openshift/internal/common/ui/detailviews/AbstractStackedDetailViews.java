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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;

/**
 * @author Andre Dietisheim
 */
public abstract class AbstractStackedDetailViews {

	protected final IDetailView emptyView = new EmptyView();
	
	private Composite parent;
	private IDetailView currentView = emptyView;
	private final StackLayout stackLayout = new StackLayout();
	private Object context;
	private DataBindingContext dbc;
	private IObservableValue detailViewModel;

	public AbstractStackedDetailViews(IObservableValue detailViewModel, Object context, Composite parent, DataBindingContext dbc) {
		Assert.isLegal(parent != null && !parent.isDisposed());
		this.parent = parent;
		parent.addDisposeListener(onDispose());
		this.context = context;
		Assert.isLegal(dbc != null);
		this.dbc = dbc;
		Assert.isLegal(detailViewModel != null && !detailViewModel.isDisposed());
		this.detailViewModel = detailViewModel;
	}

	public void createControls() {
		DataBindingUtils.addDisposableValueChangeListener(onDetailViewModelChanged(detailViewModel), detailViewModel, parent);

		parent.setLayout(stackLayout);
		createViewControls(parent, context, dbc);
		showView(detailViewModel, getView(detailViewModel), dbc);
	}

	protected abstract IDetailView[] getDetailViews();
	
	private IValueChangeListener onDetailViewModelChanged(final IObservableValue detailViewsModel) {
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
				|| detailViewsModel == null) {
			return;
		}
		currentView.onInVisible(detailViewsModel, dbc);
		this.currentView = view;
		view.onVisible(detailViewsModel, dbc);
		stackLayout.topControl = view.getControl();
		parent.layout(true, true);
	}

	protected void createViewControls(Composite parent, Object context, DataBindingContext dbc) {
		emptyView.createControls(parent, context, dbc);
		for (IDetailView detailView : getDetailViews()) {
			detailView.createControls(parent, context, dbc);
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

		if (view == null) {
			OpenShiftCommonCoreActivator.pluginLog().logWarning(NLS.bind("No view found to display value {0}", value));
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
	
	protected IDetailView getCurrentView() {
		return currentView;
	}

	public class EmptyView extends BaseDetailsView {

		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
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

		public Composite createControls(Composite parent, Object context, DataBindingContext dbc);

		public void onVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public void onInVisible(IObservableValue selectedCartridgeObservable, DataBindingContext dbc);

		public Control getControl();
		
		public boolean isViewFor(Object object);

		public void dispose();
	}
}
