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
import java.util.function.Consumer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
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
 * @author Red Hat Developers
 *
 */
public abstract class FeatureHandler extends OdoJobHandler {

	protected final ComponentFeature feature;
	

	public FeatureHandler(ComponentFeature feature) {
		this.feature = feature;
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		ComponentElement componentElement = UIUtils.getFirstElement(selection, ComponentElement.class);
		Component component = componentElement.getWrapped();
		NamespaceElement namespaceElement = componentElement.getParent();
		executeInJob("Adding/removing " + feature.getLabel(), monitor -> {
			try {
				process(componentElement.getRoot().getOdo(), namespaceElement.getWrapped(), component, res -> {
					if (component.getLiveFeatures().is(feature)) {
						component.getLiveFeatures().removeFeature(feature);
					} else {
						component.getLiveFeatures().addFeature(feature);
					}
					componentElement.refresh();
				});
				UsageStats.getInstance().odoCommand(feature.getLabel(), true);
			} catch (IOException e) {
				UsageStats.getInstance().odoCommand(feature.getLabel(), false);
			}
		});
		return Status.OK_STATUS;
	}

	protected void process(Odo odo, String project, Component component, Consumer<Boolean> callback)
			throws IOException {
		if (odo.isStarted(project, component.getPath(), component.getName(), feature)) {
			odo.stop(project, component.getPath(), component.getName(), feature, callback);
		} else {
			odo.start(project, component.getPath(), component.getName(), feature, callback);
		}
	}

}
