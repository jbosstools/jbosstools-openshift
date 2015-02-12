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
package org.jboss.tools.openshift.internal.common.ui.connection;

import java.util.Collection;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.swt.widgets.Composite;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractDetailViews;

/**
 * @author Andre Dietisheim
 */
public class ConnectionUIViews extends AbstractDetailViews {

	private Collection<IConnectionUI<IConnection>> connectionUIs;

	ConnectionUIViews(IObservableValue detailViewModel, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, parent, dbc);
		this.connectionUIs = ConnectionUIs.getInstance().getAll();
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return connectionUIs.toArray(new IConnectionUI[connectionUIs.size()]);
	}
}
