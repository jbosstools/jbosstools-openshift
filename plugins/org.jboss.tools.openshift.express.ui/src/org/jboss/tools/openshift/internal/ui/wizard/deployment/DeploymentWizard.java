package org.jboss.tools.openshift.internal.ui.wizard.deployment;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.openshift.core.Connection;
import org.jboss.tools.openshift.egit.core.EGitUtils;
import org.jboss.tools.openshift.express.internal.ui.job.FireConnectionsChangedJob;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.IOpenShiftApplicationWizardModel;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.ProjectAndServerAdapterSettingsWizardPage;
import org.jboss.tools.openshift.express.internal.ui.wizard.application.template.IApplicationTemplate;
import org.jboss.tools.openshift.internal.ui.jobs.CreateDeploymentJob;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;
import com.openshift.client.cartridge.ICartridge;
import com.openshift.client.cartridge.IStandaloneCartridge;

/**
 * DeploymentWizard is the pages needed to deploy an application to OpenShift.  The ultimate
 * outcome of producing a deployment in OpenShift includes all the supporting OpenShift
 * resources (e.g. Deployment, DeploymentConfig, BuildConfig, etc)
 *
 */
public class DeploymentWizard extends Wizard implements IImportWizard, INewWizard {

	private DeploymentWizardContext context;
	
	public DeploymentWizard(DeploymentWizardContext context){
		setWindowTitle("New OpenShift Deployment");
		this.context = context;
	}
	
	
	@Override
	public void addPages() {
		addPage(new DeploymentWizardPage(this, context));
		addPage(new DeploymentSettingsWizardPage(this, context));
	}



	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		
	}

	@Override
	public boolean performFinish() {
		try {
			CreateDeploymentJob job;
			if(context.includeBuildConfig()){
				List<URIish> remotes = EGitUtils.getDefaultRemoteURIs(context.getProject());
				if(remotes.isEmpty()){
					MessageDialog.openError(getShell(), "Error", "No GitHub remotes exist for the project");
					return true;
				}
				job = new CreateDeploymentJob(remotes.get(0).toString(), context);
			}else{
				job = new CreateDeploymentJob(null, context);
			}
			job.schedule();
			
			//TODO - FIX TO REMOVE CASTING
			new FireConnectionsChangedJob((Connection)context.getClient()).schedule();
		} catch (CoreException e) {
			throw new RuntimeException("",e);
		}
		return true;
	}

}
