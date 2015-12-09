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
package org.jboss.tools.openshift.internal.ui.property;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.WorkspaceOperationRunner;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.jboss.tools.common.jobs.ChainedJob;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.job.RefreshResourcesJob;

import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IResource;

public class OpenShiftResourceDocumentProvider extends AbstractDocumentProvider {

	private WorkspaceOperationRunner operationRunner;

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		OpenShiftResourceInput input = getInput(element);
		Document document = null;
		if (input != null) {
			IResource resource = input.getResource();
			document = new Document(resource.toJson());
		}
		return document;
	}

	@Override
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		return null;
	}

	@Override
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite)
			throws CoreException {
		OpenShiftResourceInput input = getInput(element);
		if (input == null) {
			return;
		}

		IResource resource = input.getResource();
		IClient client = resource.accept(new CapabilityVisitor<IClientCapability, IClient>() {
			@Override
			public IClient visit(IClientCapability cap) {
				return cap.getClient();
			}
		}, null);
		
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		Connection connection = input.getConnection();
		String resourceName = input.getName();
		IResource newResource = connection.getResourceFactory().create(document.get());
		ChainedJob updateResourceJob = new ChainedJob("Update "+resourceName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					client.update(newResource);//should run in job as well
				} catch (OpenShiftException e) {
					String problem = e.getStatus()==null?e.getMessage():e.getStatus().getMessage();
					String projectName = resource.getProject() == null? "unknown":resource.getProject().getName();
					IStatus error =	OpenShiftUIActivator.statusFactory().errorStatus(
							NLS.bind("Could not update \"{0}\" for project \"{1}\" : {2}", new String[]{resourceName, projectName, problem}), e);
					return error;
				}
				return Status.OK_STATUS;
			}
		};
		
		final RefreshResourcesJob refreshResourceJob = new RefreshResourcesJob(()->Collections.singleton(newResource), false);
		final OpenShiftResourceDocumentProvider docProvider = this;
		refreshResourceJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				Collection<IResource> refreshed = refreshResourceJob.getRefreshedResources();
				if (!refreshed.isEmpty()) {
					Display.getDefault().asyncExec(() -> {
					  IResource updatedResource = refreshed.iterator().next();
					  input.setResource(updatedResource);
					  try {
						docProvider.resetDocument(input);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					});
				}
			}
		});
		updateResourceJob.setNextJob(refreshResourceJob);
		updateResourceJob.schedule();
		Shell shell = Display.getCurrent().getActiveShell();
		service.showInDialog(shell, updateResourceJob);
	}

	@Override
	@SuppressWarnings("restriction")
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		if (operationRunner == null) {
			operationRunner = new WorkspaceOperationRunner();
		}
		operationRunner.setProgressMonitor(monitor);
		return operationRunner;
	}

	private OpenShiftResourceInput getInput(Object o) {
		return o instanceof OpenShiftResourceInput ? (OpenShiftResourceInput) o : null;
	}

	@Override
	public boolean isReadOnly(Object element) {
		return getInput(element) == null;
	}

	@Override
	public boolean isModifiable(Object element) {
		return getInput(element) != null;
	}
}
