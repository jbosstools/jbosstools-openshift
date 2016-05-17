/*******************************************************************************
 * boright (c) 2016 Red Hat, Inc.
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
 * @author jeff.cantrill
 */
public class AdvancedConnectionEditorsStackedView extends AbstractStackedDetailViews {

	private static final String EXTENSION = "org.jboss.tools.openshift.ui.connectionEditor.advanced";
	private static final String ATTRIBUTE_CLASS = "class";

	private Collection<IAdvancedConnectionPropertiesEditor> editors;

	@SuppressWarnings("rawtypes")
	protected AdvancedConnectionEditorsStackedView(IObservableValue detailViewModel, ConnectionWizardPageModel model, Composite parent, DataBindingContext dbc) {
		super(detailViewModel, model, parent, dbc);
		this.editors = getEditors();
	}

	@Override
	protected IDetailView[] getDetailViews() {
		return editors.toArray(new IAdvancedConnectionPropertiesEditor[editors.size()]);
	}
	
	private Collection<IAdvancedConnectionPropertiesEditor> getEditors() {
		return ExtensionUtils.getExtensions(EXTENSION, ATTRIBUTE_CLASS);
	}

}
