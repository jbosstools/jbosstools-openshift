/*******************************************************************************
 * Copyright (c) 2015-2016 Red Hat, Inc.
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
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.Deployment;
import org.jboss.tools.openshift.internal.ui.wizard.importapp.ImportApplicationWizard;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuildConfig;
import com.openshift.restclient.model.IProject;

/**
 * @author Andre Dietisheim
 */
public class ImportApplicationHandler extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		ISelection currentSelection = HandlerUtil.getActivePart(event).getSite().getWorkbenchWindow().getSelectionService().getSelection();
		IBuildConfig buildConfig = UIUtils.getFirstElement(currentSelection, IBuildConfig.class);
		Map<IProject, Collection<IBuildConfig>> projectsAndBuildConfigs = null;
		IProject project = null;
		Collection<IBuildConfig> buildConfigs = null;
		if (buildConfig == null) {
			Deployment deployment = UIUtils.getFirstElement(currentSelection, Deployment.class);
			if (deployment == null || deployment.getService() == null) {
				project = UIUtils.getFirstElement(currentSelection, IProject.class);
			} else {
				project = deployment.getService().getProject();
			}
			if (project != null) {
				buildConfigs = project.getResources(ResourceKind.BUILD_CONFIG);
			}
		} else {
			project = buildConfig.getProject();
			buildConfigs = Collections.singleton(buildConfig);
		}
		if (project != null) {
			projectsAndBuildConfigs = Collections.singletonMap(project, buildConfigs);
		}
		
		WizardUtils.openWizardDialog(
				new ImportApplicationWizard(projectsAndBuildConfigs),
				HandlerUtil.getActiveShell(event));
		return Status.OK_STATUS;
	}
}
