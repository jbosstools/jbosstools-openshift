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
import org.jboss.tools.openshift.common.core.utils.ExtensionUtils;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews;

/**
 * @author Andre Dietisheim
 */
public class ConnectionEditorsStackedView extends AbstractStackedDetailViews {

	private static final String CONNECTION_EDITORS_EXTENSION = "org.jboss.tools.openshift.ui.connectionEditor";
	private static final String ATTRIBUTE_CLASS = "class";

	private Collection<IConnectionEditor> connectionEditors;

	ConnectionEditorsStackedView(IObservableValue<?> detailViewModel, ConnectionWizardPage wizardPage, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, wizardPage, parent, dbc);
		this.connectionEditors = getConnectionEditors();
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return connectionEditors.toArray(new IConnectionEditor[connectionEditors.size()]);
	}
	
	private Collection<IConnectionEditor> getConnectionEditors() {
		return ExtensionUtils.getExtensions(CONNECTION_EDITORS_EXTENSION, ATTRIBUTE_CLASS);
	}

}
