/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.detailviews;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractDetailViews.IDetailView;

public abstract class BaseDetailsView implements IDetailView {

	private Composite control;

	@Override
	public void onVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
	}

	@Override
	public void onInVisible(IObservableValue detailsViewModel, DataBindingContext dbc) {
	}

	@Override
	public Control getControl() {
		return control;
	}

	protected <T extends Composite> T setControl(T composite) {
		this.control = composite;
		return (T) composite;
	}

	@Override
	public abstract boolean isViewFor(Object object);
	
	@Override
	public void dispose() {
	}

}