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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.openshift.internal.common.core.OpenShiftCommonCoreActivator;
import org.jboss.tools.openshift.internal.common.ui.utils.StyledTextUtils;

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
		createControls(true);
	}

	public void createControls(boolean showView) {
		DataBindingUtils.addDisposableValueChangeListener(onDetailViewModelChanged(detailViewModel), detailViewModel, parent);

		parent.setLayout(stackLayout);
		createViewControls(parent, context, dbc);
		if(showView)
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
		IDetailView view = createControls(emptyView);

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
	
	private IDetailView createControls(IDetailView view) {
		if (view == null) {
			return null;
		}
		if (view.getControl() == null) {
			view.createControls(parent, view, dbc);
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

		@Override
		public Composite createControls(Composite parent, Object context, DataBindingContext dbc) {
			Composite container = setControl(new Composite(parent, SWT.NONE));
			GridLayoutFactory.fillDefaults()
					.margins(6, 6).spacing(6, 6).applyTo(container);
			return container;
		}
		
		@Override
		public boolean isViewFor(Object object) {
			return false;
		}

		/**
		 * Helper method that creates Label + read-only StyledText
		 * @param labelText
		 * @param container
		 * @return
		 */
		protected StyledText createLabeledValue(String labelText, Composite container) {
			Label label = new Label(container, SWT.NONE);
			label.setText(labelText);
			GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).applyTo(label);
			return createNonEditableStyledText(container);
		}

		/**
		 * Helper method that creates read-only StyledText for a value to display
		 * @param container
		 * @return
		 */
		protected StyledText createNonEditableStyledText(Composite container) {
			StyledText styledText = new StyledText(container, SWT.READ_ONLY);
			styledText.setAlwaysShowScrollBars(false);
			StyledTextUtils.setTransparent(styledText);
			GridDataFactory.fillDefaults()
					.align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(styledText);
			styledText.addFocusListener(fl);
			return styledText;
		}

		/**
		 * Listener used to remove selection from read-only text widget when focus is lost.
		 * This prevents independent selecting on several widgets. As selection is used
		 * for copying text to the clipboard, it is not needed without focus. 
		 */
		protected FocusListener fl = new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
			}

			@Override
			public void focusLost(FocusEvent e) {
				if(e.getSource() instanceof StyledText) {
					((StyledText)e.getSource()).setSelection(0, 0);
				} else if(e.getSource() instanceof Text) {
					((Text)e.getSource()).setSelection(0, 0);
				}
			}

		};

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
