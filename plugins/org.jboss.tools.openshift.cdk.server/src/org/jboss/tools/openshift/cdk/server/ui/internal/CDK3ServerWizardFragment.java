package org.jboss.tools.openshift.cdk.server.ui.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
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
import org.jboss.tools.openshift.cdk.server.core.internal.MinishiftBinaryUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDK3Server;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.CDKServer;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CDKLaunchUtility;
import org.jboss.tools.openshift.cdk.server.core.internal.adapter.controllers.CommandTimeoutException;

public class CDK3ServerWizardFragment extends CDKServerWizardFragment {

	private Combo hypervisorCombo;
	private String selectedHypervisor;

	private Job longValidation;
	private Properties minishiftVersionProps = null;
	public static String ERROR_KEY = "properties.load.error";
	public static String VERSION_KEY = "Minishift version"; 

	
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
	protected Composite createComposite(Composite parent, IWizardHandle handle,
			String title, String desc, String homeLabel) {
		// boilerplate
		Composite main = setupComposite(parent, handle, title, desc);
		createCredentialWidgets(main);
		createHypervisorWidgets(main);
		createLocationWidgets(main, homeLabel);
		
		
		validateAndPack(main);
		return main;
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
				if( ind != -1 )
					selectedHypervisor = hypervisorCombo.getItem(ind);
				validate();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		
		
		if( hypervisorCombo.getItems().length > 0 ) {
			hypervisorCombo.select(0);
		}
		int ind = hypervisorCombo.getSelectionIndex();
		if( ind != -1 )
			selectedHypervisor = hypervisorCombo.getItem(ind);
	}
	

	@Override
	protected String findError() {
		if( credentials.getDomain() == null || credentials.getUser() == null) {
			return "The Container Development Environment Server Adapter requires Red Hat Access credentials.";
		}
		if( selectedHypervisor == null ) {
			return "You must choose a hypervisor.";
		}

		
		String retString = null;
		if( homeDir == null || !(new File(homeDir)).exists()) {
			retString = "The selected file does not exist.";
		} else if( !(new File(homeDir).canExecute())) {
			retString = "The selected file is not executable.";
		} else if( minishiftVersionProps != null && minishiftVersionProps.getProperty(VERSION_KEY) == null ) {
			if( minishiftVersionProps.getProperty(ERROR_KEY) != null ) {
				retString = minishiftVersionProps.getProperty(ERROR_KEY);
			} else {
				retString = "Unknown error while checking minishift version";
			}
		}
		toggleDecorator(homeText, retString);
		return retString;
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
		if( longValidation != null ) {
			longValidation.cancel(); 
		} 
		File f = new File(homeDir);
		if( !f.exists() || !f.canExecute()) {
			validate();
			return;
		}
		handle.setMessage("Checking minishift version...", IMessageProvider.INFORMATION);
		setComplete(false);
		handle.update();

		longValidation = new Job("Validate minishift location") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Properties ret = new Properties();
				try {
					String[] lines = CDKLaunchUtility.call(homeDir, new String[] {"version"}, 
						new File(homeDir).getParentFile(),
						new HashMap<String,String>(), 5000, false);
					for( int i = 0; i < lines.length; i++ ) {
						String[] split = lines[i].split(":");
						if( split.length == 2 )
							ret.put(split[0], split[1]);
					}
				} catch(IOException | CommandTimeoutException e )  {
					ret.put(ERROR_KEY, e.getMessage());
				}
				minishiftVersionProps = ret;
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
		if( homeDir != null ) {
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
		if( s instanceof IServerWorkingCopy ) {
			IServerWorkingCopy swc = (IServerWorkingCopy) s;
			swc.setAttribute(CDK3Server.MINISHIFT_FILE, homeDir);
			swc.setAttribute(CDKServer.PROP_USERNAME, selectedUser);
			swc.setAttribute(CDK3Server.PROP_HYPERVISOR, selectedHypervisor);
		}
	}
	
	
}
