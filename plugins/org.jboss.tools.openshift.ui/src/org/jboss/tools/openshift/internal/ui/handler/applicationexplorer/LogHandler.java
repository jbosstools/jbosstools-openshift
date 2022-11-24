/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ComponentElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;

/**
 * @author Red Hat Developer
 */
public abstract class LogHandler extends OdoJobHandler {

	private boolean follow;
	
	
	public LogHandler(boolean follow) {
		this.follow  = follow;
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement componentElement = UIUtils.getFirstElement(selection, ComponentElement.class);
		Component component = componentElement.getWrapped();
		NamespaceElement namespaceElement = componentElement.getParent();
		Odo odo = componentElement.getRoot().getOdo();
		Optional<Boolean> deploy;
		try {
			deploy = isDeploy(odo, componentElement);
			if (deploy.isEmpty()) {
				// int choice = Messages.showDialog(componentNode.getRoot().getProject(),
				// "Component is running in both dev and deploy mode, which container do you
				// want to get logs from ?", getActionName(),new String[] {"Dev", "Deploy"}, 0,
				// null);
				// if (choice == 0) {
				deploy = Optional.of(Boolean.FALSE);
				// } else if (choice == 1) {
				// deploy = Optional.of(Boolean.TRUE);
				// }
			}
			if (deploy.isPresent()) {
				Optional<Boolean> finalDeploy = deploy;
				CompletableFuture.runAsync(() -> {
					try {
						if (follow) {
							odo.follow(namespaceElement.getWrapped(), component.getPath(), component.getName(),
									finalDeploy.get());
						} else {
							odo.log(namespaceElement.getWrapped(), component.getPath(), component.getName(),
									finalDeploy.get());
						}
					} catch (IOException e) {
					}
				});
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	private Optional<Boolean> isDeploy(Odo odo, ComponentElement componentElement) throws IOException {
		Optional<Boolean> result = Optional.empty();
		Component component = componentElement.getWrapped();
		if ((component.getLiveFeatures().isDev() || component.getLiveFeatures().isDebug())
				&& !component.getLiveFeatures().isDeploy()
				&& !odo.isLogRunning(component.getPath(), component.getName(), false)) {
			result = Optional.of(Boolean.FALSE);
		}
		if (!component.getLiveFeatures().isDev() && !component.getLiveFeatures().isDebug()
				&& component.getLiveFeatures().isDeploy()
				&& !odo.isLogRunning(component.getPath(), component.getName(), true)) {
			result = Optional.of(Boolean.TRUE);
		}
		if ((component.getLiveFeatures().isDev() || component.getLiveFeatures().isDebug())
				&& component.getLiveFeatures().isDeploy()) {
			if (odo.isLogRunning(component.getPath(), component.getName(), false)) {
				result = Optional.of(Boolean.TRUE);
			} else if (odo.isLogRunning(component.getPath(), component.getName(), true)) {
				result = Optional.of(Boolean.FALSE);
			}
		}
		return result;
	}
}
