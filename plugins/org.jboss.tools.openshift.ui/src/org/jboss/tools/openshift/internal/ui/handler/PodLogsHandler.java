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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.job.PodLogsJob;
import org.jboss.tools.openshift.internal.ui.models.IResourceUIModel;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IResource;

/**
 * @author jeff.cantrill
 */
public class PodLogsHandler extends AbstractOpenShiftCliHandler {
	private static final String [] STATES = new String [] {"Running", "Succeeded", "Failed"};

	@Override
	protected void handleEvent(ExecutionEvent event){
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IPod pod = UIUtils.getFirstElement(selection, IPod.class);
		if(pod == null) {
			pod = getPodFromBuild(selection);
			if(pod == null) {
				MessageDialog.openError(HandlerUtil.getActiveShell(event), "No pod selected", "Unable to determine the build pod in order to retrieve its log.");
				return;
			}
		}
		showLogs(pod, event);
	}
	
	private void showLogs(IPod pod, ExecutionEvent event) {
		if(pod == null){
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "No pod selected", "No pod was selected to retrieve a log.");
			return;
		}
		if(!ArrayUtils.contains(STATES, pod.getStatus())) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "Logs Unavailable", NLS.bind("The log is unavailable while the pod is in {0} state.", pod.getStatus()));
			return;
		}
		Collection<IContainer> containers = pod.getContainers();
		if(containers.isEmpty()) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), "Logs Unavailable", "There are no containers from which to retrieve logs");
			return;
		}
		String containerName = null;
		if(containers.size() > 1) {
			List<String> names = containers.stream().map(c->c.getName()).collect(Collectors.toList());
			Collections.sort(names);
			ElementListSelectionDialog dialog = new ElementListSelectionDialog(HandlerUtil.getActiveShell(event), new LabelProvider());
			dialog.setElements(names.toArray());
			dialog.setTitle("Pod Containers");
			dialog.setMessage("Select a pod container");
			dialog.setMultipleSelection(false);
			int result = dialog.open();
			if(Window.CANCEL == result) {
				return;
			}
			containerName = (String) dialog.getFirstResult();
		}else if (containers.size() == 1){
			containerName = containers.iterator().next().getName();
		}
		new PodLogsJob(pod, containerName).schedule();
	}

	private IPod getPodFromBuild(ISelection selection) {
		IBuild build = UIUtils.getFirstElement(selection, IBuild.class);
		if(build == null) {
			IResourceUIModel model = UIUtils.getFirstElement(selection, IResourceUIModel.class);
			if(model != null) {
				IResource resource = model.getResource();
				if(resource != null && ResourceKind.BUILD.equals(resource.getKind())) {
					build = (IBuild) resource;
				}
			}
		}
		if(build != null) {
			final String buildName = build.getName();
			Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(build);
			List<IPod> pods = connection.getResources(ResourceKind.POD, build.getNamespace());
			for (IPod pod : pods) {
				if(buildName.equals(pod.getAnnotation(OpenShiftAPIAnnotations.BUILD_NAME))) {
					return pod;
				}
			}
		}
		return null;
	}

}
