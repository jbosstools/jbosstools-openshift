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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.OpenShiftAPIAnnotations;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionsRegistryUtil;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.job.StartBuildJob;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IResource;

/**
 * 
 * @author jeff.cantrill
 *
 */
public class StartBuildHandler extends AbstractHandler{
	
	private static final String SOURCE = "org.jboss.tools.openshift.ui.command.startbuild.source";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException { 
		ISelection selection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IResource buildAble = getBuildableResource(selection, event.getParameter(SOURCE));
		if(buildAble == null) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Trigger Build", "A build or build config must be selected in order to trigger a build.");
			return null;
		}
		new StartBuildJob(buildAble).schedule();
		return null;
	}
	
	public IResource getBuildableResource(ISelection selection, String source) {
		IBuild selectedBuild= UIUtils.getFirstElement(selection, IBuild.class);
		if (selectedBuild != null) {
			// TODO: return the related build config
		}
		return UIUtils.getFirstElement(selection, IBuild.class);
	}
	
	private IResource loadBuildConfig(IResource build) {
		Connection connection = ConnectionsRegistryUtil.safeGetConnectionFor(build);
		String buildConfigName = build.getLabels().get(OpenShiftAPIAnnotations.BUILD_CONFIG_NAME);
		if(!buildConfigName.isEmpty()) {
			IResource proto = connection.getResourceFactory().stub(ResourceKind.BUILD_CONFIG, buildConfigName, build.getNamespace());
			return connection.getResource(proto);
		}
		OpenShiftUIActivator.getDefault().getLogger().logInfo("Unable to determine associated BuildConfig from Build: " + build.toJson());
		return null;
	}
}