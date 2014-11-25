package org.jboss.tools.openshift.internal.ui.command;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jboss.tools.openshift.express.internal.ui.utils.WizardUtils;
import org.jboss.tools.openshift.internal.ui.wizard.deployment.DeploymentWizard;
import org.jboss.tools.openshift.internal.ui.wizard.deployment.DeploymentWizardContext;

import com.openshift.kube.Client;
import com.openshift.kube.Project;
import com.openshift.kube.capability.ImageRegistryHosting;

public class NewDeploymentHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if(selection == null || selection.isEmpty() || !(selection instanceof StructuredSelection)) return null;
		Object element = ((StructuredSelection)selection).getFirstElement();
		if(!(element instanceof Project)) return null;
		
		Project p = (Project) element;
		if(!p.getClient().isCapableOf(ImageRegistryHosting.class))
			return null;
		
		ImageRegistryHosting hosting = p.getClient().getCapability(ImageRegistryHosting.class);
		DeploymentWizardContext context = new DeploymentWizardContext(p.getClient(), p, hosting.getRegistryUri());
		if (WizardUtils.openWizard(new DeploymentWizard(context), HandlerUtil.getActiveShell(event))) {
		}

		return null;
	}

}
