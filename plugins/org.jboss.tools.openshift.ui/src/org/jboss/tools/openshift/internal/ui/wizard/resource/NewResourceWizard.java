/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.wizard.resource;

import static org.jboss.tools.common.ui.WizardUtils.runInWizard;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.jboss.tools.common.ui.JobUtils;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.utils.VariablesHelper;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.OpenShiftUIUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.ResourceSummaryDialog;
import org.jboss.tools.openshift.internal.ui.job.CreateResourceJob;

import com.openshift.restclient.model.IProject;

/**
 * The new resource wizard that allows you to create a resource given an
 * OpenShift resource JSON file
 * 
 * @author Jeff Maury
 */
public class NewResourceWizard extends Wizard implements IWorkbenchWizard {

	private NewResourceWizardModel model;

	public NewResourceWizard(NewResourceWizardModel model) {
		setWindowTitle("New OpenShift resource");
		setNeedsProgressMonitor(true);
		this.model = model;
	}
	
	public NewResourceWizard() {
        this(new NewResourceWizardModel());
    }

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
        IConnection connection = UIUtils.getFirstElement(selection, IConnection.class);
        if (connection == null) {
            IProject project = UIUtils.getFirstElement(selection, IProject.class);
            if (project != null) {
                model.setConnection(ConnectionsRegistryUtil.getConnectionFor(project));
                model.setProject(project);                   
            }
        } else {
            model.setConnection(connection);
        }
	}

	@Override
	public void addPages() {
		addPage(new ResourcePayloadPage(this, model));
	}

	@Override
	public boolean performFinish() {
        boolean success = false;
        try (InputStream is = (IResourcePayloadPageModel.URL_VALIDATOR.isValid(model.getSource()))?new URL(model.getSource()).openStream()
                                                                                                  :new FileInputStream(VariablesHelper.replaceVariables(model.getSource()))) {
            final CreateResourceJob createJob = new CreateResourceJob(model.getProject(), is);

            createJob.addJobChangeListener(new JobChangeAdapter() {

                @Override
                public void done(IJobChangeEvent event) {
                    IStatus status = event.getResult();
                    if (JobUtils.isOk(status) || JobUtils.isWarning(status)) {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                new ResourceSummaryDialog(getShell(),
                                        createJob.getResource(), "Create Resource Summary",
                                        "Results of creating the resource(s)").open();
                            }
                        });
                        OpenShiftUIUtils.showOpenShiftExplorerView();
                    }
                }
            });

            IStatus status = runInWizard(createJob, createJob.getDelegatingProgressMonitor(), getContainer());
            success = isSuccess(status);
        } catch (InvocationTargetException | InterruptedException | IOException e) {
            OpenShiftUIActivator.getDefault().getLogger().logError(e);
            success = false;
        }
        return success;
    }

	private boolean isSuccess(IStatus status) {
		return JobUtils.isOk(status) 
				|| JobUtils.isWarning(status);
	}
}
