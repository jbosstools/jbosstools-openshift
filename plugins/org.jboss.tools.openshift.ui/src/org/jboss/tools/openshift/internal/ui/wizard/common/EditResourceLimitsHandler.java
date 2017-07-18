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
package org.jboss.tools.openshift.internal.ui.wizard.common;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.core.util.ResourceUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIMessages;
import org.jboss.tools.openshift.internal.ui.handler.EditResourceLimitsPage;
import org.jboss.tools.openshift.internal.ui.utils.ResourceWrapperUtils;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IReplicationController;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.IService;

/**
 * Handler for editing resource limits
 */
public class EditResourceLimitsHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IReplicationController dcOrRc = getReplicationControllerOrDeploymentConfig(
                getSelectedElement(event, IResource.class));
        if (dcOrRc == null) {
            IResource resource = ResourceWrapperUtils
                    .getResource(UIUtils.getFirstElement(HandlerUtil.getCurrentSelection(event)));
            return OpenShiftUIActivator.statusFactory()
                    .errorStatus(NLS.bind(
                            "Could not edit resources {0}: Could not find deployment config or replication controller",
                            resource == null ? "" : resource.getName()));
        }
        editResources(event, dcOrRc, dcOrRc.getName());
        return null;
    }

    protected <T> T getSelectedElement(ExecutionEvent event, Class<T> klass) {
        ISelection selection = UIUtils.getCurrentSelection(event);
        return UIUtils.getFirstElement(selection, klass);
    }

    private IReplicationController getReplicationControllerOrDeploymentConfig(IResource resource) {
        if (resource == null) {
            return null;
        }

        Connection connection = ConnectionsRegistryUtil.getConnectionFor(resource);
        IReplicationController rcOrDc = ResourceUtils.getDeploymentConfigFor(resource, connection);
        if (null == rcOrDc) {
        	if (resource instanceof IService) {
        		rcOrDc = ResourceUtils.getReplicationControllerFor((IService) resource, resource.getProject().getResources(ResourceKind.REPLICATION_CONTROLLER));
        	} else if (resource instanceof IReplicationController) {
        		rcOrDc = (IReplicationController) resource;
        	} else if (resource instanceof IPod) {
        		rcOrDc = ResourceUtils.getDeploymentConfigOrReplicationControllerFor((IPod) resource);
        		if (null == rcOrDc) {
            		rcOrDc = ResourceUtils.getReplicationControllerFor((IPod) resource, resource.getProject().getResources(ResourceKind.REPLICATION_CONTROLLER));
        		}
        	}
        }
        return rcOrDc;
    }

    protected void editResources(ExecutionEvent event, IReplicationController rc, String name) {
        EditResourceLimitsPageModel model = new EditResourceLimitsPageModel(rc);
        EditResourceLimitsWizard wizard = new EditResourceLimitsWizard(model, "Edit resource limits");
        WizardUtils.openWizardDialog(wizard, HandlerUtil.getActiveShell(event));
     }
    
    class EditResourceLimitsWizard extends Wizard {
        private EditResourceLimitsPageModel model;

        public EditResourceLimitsWizard(EditResourceLimitsPageModel model, String title) {
            this.model = model;
            setWindowTitle(title);
        }

        @Override
        public void addPages() {
            addPage(new EditResourceLimitsPage(model, this));
        }

        @Override
        public boolean performFinish() {
            model.dispose();
            new Job(NLS.bind(OpenShiftUIMessages.EditResourceLimitsJobTitle, model.getUpdatedReplicationController().getName())) {
                
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        IReplicationController rc = model.getUpdatedReplicationController();
                        Connection connection = ConnectionsRegistryUtil.getConnectionFor(rc);
                        connection.updateResource(rc);
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        String message = NLS.bind(OpenShiftUIMessages.EditResourceLimitsJobErrorMessage, model.getUpdatedReplicationController().getName());
                        OpenShiftUIActivator.getDefault().getLogger().logError(message,e);
                        return new Status(Status.ERROR, OpenShiftUIActivator.PLUGIN_ID, message, e);
                    }
                }
            }.schedule();
            return true;
        }
    }



}

