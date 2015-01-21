/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.command;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.NewDomainWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;

/**
 * @author Andre Dietisheim
 */
public class NewDomainHandler extends AbstractDomainHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExpressConnection connection = getConnection(event);
		if (connection == null) {
			return null;
		}
		if (WizardUtils.openWizard(new NewDomainWizard(connection), HandlerUtil.getActiveShell(event))) {
			ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection);
		}
		return null;
	}
}
