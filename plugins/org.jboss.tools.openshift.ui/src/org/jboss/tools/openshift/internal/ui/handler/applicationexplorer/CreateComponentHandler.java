/*******************************************************************************
 * Copyright (c) 2020-2022 Red Hat, Inc.
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.common.ui.notification.LabelNotification;
import org.jboss.tools.openshift.core.odo.Component;
import org.jboss.tools.openshift.core.odo.ComponentFeature;
import org.jboss.tools.openshift.core.odo.DevfileComponentType;
import org.jboss.tools.openshift.core.odo.Odo;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.models.AbstractOpenshiftUIElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.ApplicationExplorerUIModel;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistryComponentTypeElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.DevfileRegistryComponentTypeStarterElement;
import org.jboss.tools.openshift.internal.ui.models.applicationexplorer.NamespaceElement;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateComponentModel;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.CreateComponentWizard;

/**
 * @author Red Hat Developers
 */
public class CreateComponentHandler extends OdoJobHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		NamespaceElement project = UIUtils.getFirstElement(selection, NamespaceElement.class);
		DevfileRegistryComponentTypeElement componentType = null;
		DevfileRegistryComponentTypeStarterElement starter = null;
		if (project == null) {
			componentType = UIUtils.getFirstElement(selection, DevfileRegistryComponentTypeElement.class);
			if (componentType == null) {
				starter = UIUtils.getFirstElement(selection, DevfileRegistryComponentTypeStarterElement.class);
				if (starter == null) {
					return OpenShiftUIActivator.statusFactory()
							.cancelStatus("No project, component type nor starter selected"); //$NON-NLS-1$
				}
				componentType = starter.getParent();
			}
		}
		final Shell parent = HandlerUtil.getActiveShell(event);
		try {
			openDialog(componentType, project, parent, starter);
			return Status.OK_STATUS;
		} catch (IOException e) {
			return OpenShiftUIActivator.statusFactory().errorStatus(e);
		}
	}

	private static IProject getOpenedProject() {
		for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
				if (p.isOpen() && !p.hasNature("org.eclipse.rse.ui.remoteSystemsTempNature")) {
					return p;
				}
			} catch (CoreException e) {
				OpenShiftUIActivator.log(IStatus.INFO, e.getLocalizedMessage());
			}
		}
		return null;
	}

	private static void openDialog(DevfileRegistryComponentTypeElement componentType, NamespaceElement project,
			final Shell parent, DevfileRegistryComponentTypeStarterElement starter) throws IOException {
		Odo odo = project != null ? project.getParent().getOdo() : componentType.getRoot().getOdo();
		String projectName = project != null ? project.getWrapped() : odo.getNamespace();
		IProject eclipseProject = getOpenedProject();
		final CreateComponentModel model = new CreateComponentModel(odo, odo.getComponentTypes(), projectName,
				eclipseProject);
		if (componentType != null) {
			model.setSelectedComponentType(componentType.getWrapped());
		}
		if (starter != null) {
			model.setSelectedComponentStarter(starter.getWrapped());
		}
		final IWizard createComponentWizard = new CreateComponentWizard(model);
		if (WizardUtils.openWizardDialog(createComponentWizard, parent) == Window.OK) {
			AbstractOpenshiftUIElement<?, ?, ApplicationExplorerUIModel> element = componentType != null
					? componentType.getRoot()
					: project;
			executeInJob("Creating component", monitor -> execute(parent, model, element));
		}
	}

	/**
	 * @param shell
	 * @param model
	 * @return
	 */
	private static void execute(Shell shell, CreateComponentModel model,
			AbstractOpenshiftUIElement<?, ?, ApplicationExplorerUIModel> element) {
		LabelNotification notification = LabelNotification.openNotification(shell,
				"Creating component " + model.getComponentName());
		try {
			model.getOdo().createComponent(model.getProjectName(), model.getSelectedComponentType().getName(),
					((DevfileComponentType) model.getSelectedComponentType()).getDevfileRegistry().getName(),
					model.getComponentName(), model.getEclipseProject().getLocation().toOSString(),
					model.isEclipseProjectHasDevfile() ? CreateComponentModel.DEVFILE_NAME : null,
					model.getSelectedComponentStarter() == null ? null : model.getSelectedComponentStarter().getName());
			LabelNotification.openNotification(notification, shell,
					"Component " + model.getComponentName() + " created");
			element.getRoot().addContext(model.getEclipseProject());
//			if (model.isDevModeAfterCreate()) {
//				model.getOdo().start(model.getProjectName(), model.getEclipseProject().getLocation().toOSString(), model.getComponentName(), ComponentFeature.DEV, res -> {
//					Component component = (Component)element.getWrapped();
//					if (component.getLiveFeatures().is(ComponentFeature.DEV)) {
//						component.getLiveFeatures().removeFeature(ComponentFeature.DEV);
//					} else {
//						component.getLiveFeatures().addFeature(ComponentFeature.DEV);
//					}
//					element.refresh();
//				});
//			}
			
		} catch (IOException e) {
			shell.getDisplay().asyncExec(() -> {
				notification.close();
				MessageDialog.openError(shell, "Create component",
						"Error creating component " + model.getComponentName() + ": \n" + e.getLocalizedMessage());
			});
		}
	}

	/**
	 * @param shell
	 * @param parent
	 */
	public static void openDialog(Shell shell, AbstractOpenshiftUIElement<?, ?, ApplicationExplorerUIModel> parent) {
		try {
			if (parent instanceof NamespaceElement) {
				openDialog(null, (NamespaceElement) parent, shell, null);
			}
		} catch (IOException e) {
			MessageDialog.openError(shell, "Create component", "Error creating component: \n" + e.getLocalizedMessage());
		}

	}
}
