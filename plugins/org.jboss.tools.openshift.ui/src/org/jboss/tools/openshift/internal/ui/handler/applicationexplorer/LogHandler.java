/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.handler.applicationexplorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentFeature;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.core.UsageStats;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;

/**
 * @author Red Hat Developer
 */
public abstract class LogHandler extends OdoJobHandler {

	private boolean follow;
	private int choice;

	public LogHandler(boolean follow) {
		this.follow = follow;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement componentElement = UIUtils.getFirstElement(selection, ComponentElement.class);
		Component component = componentElement.getWrapped();
		NamespaceElement namespaceElement = componentElement.getParent();
		Odo odo = componentElement.getRoot().getOdo();
		try {
			var possibleLogs = getPossibleLogs(odo, componentElement);
			if (possibleLogs.size() > 1) {
				Display.getDefault().syncExec(() -> setChoice(MessageDialog.open(MessageDialog.QUESTION,
						Display.getDefault().getActiveShell(), "Choose logs target",
						"Component is running in both dev and deploy mode, which container do you want to get logs from ?",
						SWT.NONE, "Dev", "Deploy")));
				if (choice == 0) {
					possibleLogs = Collections.singletonList(ComponentFeature.DEV);
				} else if (choice == 1) {
					possibleLogs = Collections.singletonList(ComponentFeature.DEPLOY);
				}
			}
			if (possibleLogs.size() == 1) {
				final var target = possibleLogs.get(0);
				CompletableFuture.runAsync(() -> {
					try {
						if (follow) {
							odo.follow(namespaceElement.getWrapped(), component.getPath(), component.getName(),
									target == ComponentFeature.DEPLOY);
						} else {
							odo.log(namespaceElement.getWrapped(), component.getPath(), component.getName(),
									target == ComponentFeature.DEPLOY);
						}
					} catch (IOException e) {
						OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
					}
				});
			} else {
				Display.getDefault().asyncExec(() -> MessageDialog.open(MessageDialog.WARNING,
						Display.getDefault().getActiveShell(), "Choose logs target",
						"No more containers to target, logs is already running.",
						SWT.NONE));
			}
			UsageStats.getInstance().odoCommand(follow ? "follow log" : "show log", true);
			return Status.OK_STATUS;
		} catch (IOException e) {
			UsageStats.getInstance().odoCommand(follow ? "follow log" : "show log", false);
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private void setChoice(int choice) {
		this.choice = choice;
	}

	private List<ComponentFeature> getPossibleLogs(Odo odo, ComponentElement componentElement) throws IOException {
		var result = new ArrayList<ComponentFeature>();
		Component component = componentElement.getWrapped();
		if ((component.getLiveFeatures().isDev() || component.getLiveFeatures().isDebug())
				&& !odo.isLogRunning(component.getPath(), component.getName(), false)) {
			result.add(ComponentFeature.DEV);
		}
		if (component.getLiveFeatures().isDeploy()
				&& !odo.isLogRunning(component.getPath(), component.getName(), true)) {
			result.add(ComponentFeature.DEPLOY);
		}
		return result;
	}
}
