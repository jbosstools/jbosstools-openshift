/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.tools.openshift.express.internal.core;

import org.eclipse.osgi.util.NLS;

public class ExpressCoreMessages extends NLS {

	private static final String BUNDLE_NAME = ExpressCoreMessages.class.getName();

	static {
		NLS.initializeMessages(BUNDLE_NAME, ExpressCoreMessages.class);
	}

	private ExpressCoreMessages() {
		// Do not instantiate
	}

	public static String OPERATION_CANCELLED;
}
