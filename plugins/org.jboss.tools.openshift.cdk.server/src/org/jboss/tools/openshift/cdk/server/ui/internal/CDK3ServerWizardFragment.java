package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.jboss.tools.as.runtimes.integration.ui.composites.DownloadRuntimeHyperlinkComposite;
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader;
import org.jboss.tools.openshift.cdk.server.core.internal.detection.MinishiftVersionLoader.MinishiftVersions;

public class CDK3ServerWizardFragment extends CDKServerWizardFragment {

	private Combo hypervisorCombo;
	private String selectedHypervisor;

	private Job longValidation;
	private MinishiftVersions minishiftVersionProps = null;

	public CDK3ServerWizardFragment() {
		super(); // 0-arg constructor for extension pt creation
	}

	@Override
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		String title = "Red Hat Container Development Environment";
		String desc = "A server adapter representing Red Hat Container Development Kit Version 3.";
		String label = "Minishift Binary: ";
		return createComposite(parent, handle, title, desc, label);
	}

	@Override
	protected Composite createComposite(Composite parent, IWizardHandle handle, String title, String desc,
			String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createCredentialWidgets(main);
		createHypervisorWidgets(main);
		createDownloadWidgets(main, handle);
		createLocationWidgets(main, homeLabel);

		validateAndPack(main);
		return main;
	}

	
	protected void createDownloadWidgets(Composite main, IWizardHandle handle) {
		DownloadRuntimeHyperlinkComposite hyperlink = new DownloadRuntimeHyperlinkComposite(main, 
				SWT.NONE, handle, getTaskModel()) {
			
			@Override
			protected void postDownloadRuntimeUpdateWizard(String newHomeDir) {
				handleDownloadedFile(newHomeDir);
				getWizardHandle().update();
			}
		};
		GridDataFactory.fillDefaults().span(3, 1).align(SWT.END, SWT.END).applyTo(hyperlink);
	}
	
	protected void handleDownloadedFile(String newHome) {
		if( !homeText.isDisposed()) {
			homeText.setText(newHome);
		}
	}
	
	protected void createHypervisorWidgets(Composite main) {
		// Point to file / folder to run
		Label l = new Label(main, SWT.NONE);
		l.setText("Hypervisor:");
		GridData comboData = new GridData();
		comboData.grabExcessHorizontalSpace = true;
		comboData.horizontalAlignment = SWT.FILL;
		comboData.horizontalSpan = 2;
		hypervisorCombo = new Combo(main, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		hypervisorCombo.setLayoutData(comboData);
		hypervisorCombo.setItems(CDK3Server.getHypervisors());

		hypervisorCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ind = hypervisorCombo.getSelectionIndex();
				if (ind != -1)
					selectedHypervisor = hypervisorCombo.getItem(ind);
				validate();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		if (hypervisorCombo.getItems().length > 0) {
			hypervisorCombo.select(0);
		}
		int ind = hypervisorCombo.getSelectionIndex();
		if (ind != -1)
			selectedHypervisor = hypervisorCombo.getItem(ind);
	}

	@Override
	protected String findError() {
		// Validate credentials
		if (shouldCreateCredentialWidgets()) {
			if (credentials.getDomain() == null || credentials.getUser() == null) {
				return "The Container Development Environment Server Adapter requires Red Hat Access credentials.";
			}
		}

		// Validate hypervisor
		if (selectedHypervisor == null) {
			return "You must choose a hypervisor.";
		}

		// Validate home directory
		String retString = validateHomeDirectory();
		if (retString != null)
			return retString;

		// Validate versions
		return validateMinishiftVersion();
	}

	protected String validateMinishiftVersion() {
		String ret = null;
		if (minishiftVersionProps == null) {
			ret = "Unknown error when checking minishift version: " + homeDir;
		} else if (!minishiftVersionProps.isValid()) {
			ret = minishiftVersionProps.getError();
			if (ret == null) {
				ret = "Unknown error while checking minishift version";
			}
		} else {
			String versionCompatError = isVersionCompatible(minishiftVersionProps);
			if (versionCompatError != null)
				ret = versionCompatError;
		}
		toggleHomeDecorator(ret);
		return ret;
	}

	protected String validateHomeDirectory() {
		String retString = null;
		if (homeDir == null || !(new File(homeDir)).exists()) {
			retString = "The selected file does not exist.";
		} else if (!(new File(homeDir).canExecute())) {
			retString = "The selected file is not executable.";
		}
		toggleHomeDecorator(retString);
		return retString;
	}

	protected String isVersionCompatible(MinishiftVersions versions) {
		String cdkVers = versions.getCDKVersion();
		if (cdkVers == null) {
			return "Cannot determine CDK version.";
		}
		if (CDK3Server.matchesCDK3(cdkVers)) {
			return null;
		}
		return "CDK version " + cdkVers + " is not compatible with this server adapter.";
	}

	@Override
	protected void browseHomeDirClicked() {
		browseHomeDirClicked(false);
	}

	@Override
	protected SelectionListener createBrowseListener() {
		return new BrowseListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				minishiftVersionProps = null;
				super.widgetSelected(e);
				kickValidationJob();
			}
		};
	}

	@Override
	protected ModifyListener createHomeModifyListener() {
		return new HomeModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				minishiftVersionProps = null;
				super.modifyText(e);
				kickValidationJob();
			}
		};
	}

	private synchronized void kickValidationJob() {
		if (longValidation != null) {
			longValidation.cancel();
		}
		File f = new File(homeDir);
		if (!f.exists() || !f.canExecute()) {
			validate();
			return;
		}
		handle.setMessage("Checking minishift version...", IMessageProvider.INFORMATION);
		setComplete(false);
		handle.update();

		longValidation = new Job("Validate minishift location") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				minishiftVersionProps = MinishiftVersionLoader.getVersionProperties(homeDir);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						validate();
					}
				});
				return Status.OK_STATUS;
			}
		};
		longValidation.setSystem(true);
		longValidation.schedule(750);
	}

	@Override
	protected void fillTextField() {
		if (homeDir != null) {
			homeText.setText(homeDir);
		} else {
			homeDir = MinishiftBinaryUtility.getMinishiftLocation();
			if (homeDir != null) {
				homeText.setText(homeDir);
			}
		}
	}

	@Override
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		exit();
		IServer s = getServerFromTaskModel();
		if (s instanceof IServerWorkingCopy) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CDK3Server.MINISHIFT_FILE, homeDir);
			swc.setAttribute(CDKServer.PROP_USERNAME, selectedUser);
			swc.setAttribute(CDK3Server.PROP_HYPERVISOR, selectedHypervisor);
		}
	}

}
