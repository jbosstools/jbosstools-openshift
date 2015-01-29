/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.internal.common.ui.connection;

import org.eclipse.jface.viewers.IElementComparer;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.ICredentialsConnection;
import org.jboss.tools.openshift.common.core.connection.NewConnectionMarker;
import org.jboss.tools.openshift.common.core.utils.StringUtils;

/**
 * @author Andre Dietisheim
 */
public class NewConnectionAwareConnectionComparer implements IElementComparer {

	@Override
	public int hashCode(Object element) {
		return 0;
	}

	@Override
	public boolean equals(Object thisObject, Object thatObject) {
		if (thisObject == null) {
			if (thatObject == null) {
				return true;
			} else if (thatObject instanceof NewConnectionMarker) {
				return true;
			} else {
				return false;
			}
		} else if (thisObject instanceof NewConnectionMarker) {
			if (thatObject == null
					|| thatObject instanceof NewConnectionMarker) {
				return true;
			} else {
				return false;
			}
		} else {
			if (thatObject == null) {
				return false;
			} else if (thatObject instanceof NewConnectionMarker) {
				return true;
			}

			IConnection thisConnection = (IConnection) thisObject;
			IConnection thatConnection = (IConnection) thatObject;
			boolean hostEquals = StringUtils.areEqual(thisConnection.getHost(), thatConnection.getHost());
			if (!ICredentialsConnection.class.isAssignableFrom(thisConnection.getClass())) {
				return hostEquals;
			} else {
				return hostEquals 
						&& StringUtils.areEqual(
								((ICredentialsConnection) thisConnection).getUsername(), 
								((ICredentialsConnection) thatConnection).getUsername());
			}
		}
	}
}
