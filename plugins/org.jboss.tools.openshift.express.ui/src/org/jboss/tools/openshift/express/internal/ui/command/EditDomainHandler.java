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
import org.jboss.tools.openshift.express.internal.ui.wizard.domain.EditDomainWizard;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.WizardUtils;

import com.openshift.client.IDomain;

/**
 * @author Andre Dietisheim
 */
public class EditDomainHandler extends AbstractDomainHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDomain domain = UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event), IDomain.class);
		if (domain == null) {
			return null;
		}

		WizardUtils.openWizard(new EditDomainWizard(domain), HandlerUtil.getActiveShell(event));
		return null;
	}
	
}
