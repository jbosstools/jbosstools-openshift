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
package org.jboss.tools.openshift.internal.ui.handler;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.preferences.IOpenShiftCoreConstants;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.openshift.restclient.capability.resources.IPortForwardable;

/**
 * 
 * @author jeff.cantrill
 *
 */
public abstract class AbstractOpenShiftCliHandler extends AbstractHandler {
	
	protected abstract void handleEvent(ExecutionEvent event);
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//check binary locations
		final String location = OpenShiftUIActivator.getDefault().getCorePreferenceStore().getString(IOpenShiftCoreConstants.OPENSHIFT_CLI_LOC);
		if(StringUtils.isBlank(location)) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "Unknown binary location", "The location to the OpenShift 'oc' binary must be set in your Eclipse preferences.");
			return null;
		}
		System.setProperty(IPortForwardable.OPENSHIFT_BINARY_LOCATION, location);
		handleEvent(event);
		return null;
	}
}
