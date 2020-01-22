/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.core.odo.Odo;

/**
 * @author Red Hat Developers
 *
 */
public abstract class OdoModel extends ObservableUIPojo {
	private final Odo odo;
	
	public OdoModel(Odo odo) {
		this.odo = odo;
	}

	/**
	 * @return the odo
	 */
	public Odo getOdo() {
		return odo;
	}
}
