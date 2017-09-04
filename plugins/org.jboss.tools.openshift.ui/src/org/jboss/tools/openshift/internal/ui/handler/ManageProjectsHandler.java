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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.common.ui.wizard.OkButtonWizardDialog;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.wizard.project.ManageProjectsWizard;

import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
public class ManageProjectsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Connection connection = UIUtils.getFirstElement(selection, Connection.class);
        if (connection == null) {
            IResource resource = UIUtils.getFirstElement(selection, IResource.class);
            if (resource != null) {
                connection = ConnectionsRegistryUtil.getConnectionFor(resource);
            }
        }
        if (connection == null) {
            return OpenShiftUIActivator.statusFactory().cancelStatus("No connection selected that we can manage projects for.");
        }
        new OkButtonWizardDialog(HandlerUtil.getActiveShell(event), new ManageProjectsWizard(connection)).open();
        return null;
    }

}
