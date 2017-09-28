/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.internal.core;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.openshift.io.core.TokenProvider;
import org.jboss.tools.openshift.io.core.AccountService;

/**
 * Delegates to the account service as we don't control the lifecycle of a token provider
 */
public class DefaultTokenProvider implements TokenProvider {

	@Override
	public String apply(IResource t) {
		return AccountService.getDefault().getToken(t);
	}
}
