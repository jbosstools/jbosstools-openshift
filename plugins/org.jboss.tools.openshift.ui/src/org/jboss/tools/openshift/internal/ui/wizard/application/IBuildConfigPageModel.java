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
package org.jboss.tools.openshift.internal.ui.wizard.application;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.common.ui.wizard.IConnectionAware;

import com.openshift.restclient.model.IBuildConfig;

/**
 * @author Andre Dietisheim
 */
public interface IBuildConfigPageModel extends IConnectionAware<Connection> {
	
	public String PROPERTY_SELECTED_ITEM = "selectedItem";

	public Object getSelectedItem();

	public void setSelectedItem(Object selectedItem);
	
	public IBuildConfig getSelectedBuildConfig();

}
