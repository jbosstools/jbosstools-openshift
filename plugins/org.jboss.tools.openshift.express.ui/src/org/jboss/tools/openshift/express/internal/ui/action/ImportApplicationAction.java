package org.jboss.tools.openshift.express.internal.ui.action;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.openshift.express.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.express.internal.ui.messages.OpenShiftExpressUIMessages;
import org.jboss.tools.openshift.express.internal.ui.wizard.OpenShiftExpressApplicationWizard;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;

public class ImportApplicationAction extends AbstractAction {

	public ImportApplicationAction() {
		super(OpenShiftExpressUIMessages.IMPORT_APPLICATION_ACTION);
		setImageDescriptor(OpenShiftUIActivator.getDefault().createImageDescriptor("go-into.gif"));
	}
	
	@Override
	public void run() {
		final ITreeSelection treeSelection = (ITreeSelection)selection;
		if (selection != null && selection instanceof ITreeSelection && treeSelection.getFirstElement() instanceof IApplication) {
			final IApplication application = (IApplication) treeSelection.getFirstElement();
			//final IUser user = OpenShiftUIActivator.getDefault().getUser();
			OpenShiftExpressApplicationWizard wizard = new OpenShiftExpressApplicationWizard();
			TreePath[] paths = treeSelection.getPaths();
			if( paths != null && paths.length == 1 ) {
				Object user = paths[0].getParentPath().getLastSegment();
				if( user instanceof IUser )
					wizard.setInitialUser((IUser)user);
			}
			wizard.setSelectedApplication(application);
			final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(application.getName());
			if(project.exists()) {
				wizard.setSelectedProject(project);
			}
			WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
			dialog.create();
			dialog.open();
			
		}
	}

	
}
