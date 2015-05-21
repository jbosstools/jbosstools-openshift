/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.connection;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.jboss.tools.openshift.internal.common.ui.connection.ConnectionWizardPageModel.IConnectionAuthenticationProvider;
import org.jboss.tools.openshift.internal.common.ui.detailviews.AbstractStackedDetailViews.IDetailView;

/**
 * @author jeff.cantrill
 */
 interface IConnectionEditorDetailView extends IDetailView {
	 
	 void setSelectedConnection(IObservableValue selectedConnection);
	 
	 IConnectionAuthenticationProvider  getConnectionAuthenticationProvider();
}
