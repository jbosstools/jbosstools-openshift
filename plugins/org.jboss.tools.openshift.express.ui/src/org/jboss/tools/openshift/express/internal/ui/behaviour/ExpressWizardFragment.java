/*******************************************************************************
 * Copyright (c) 2012 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.behaviour;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.common.ui.WizardUtils;
import org.jboss.tools.openshift.express.internal.core.behaviour.ExpressServerUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.CredentialsWizardPageModel;

import com.openshift.express.client.IApplication;
import com.openshift.express.client.IUser;
import com.openshift.express.client.OpenShiftException;

public class ExpressWizardFragment extends WizardFragment {
	private IWizardHandle handle;
	private IApplication fapplication;
	private IUser fuser;
	private String[] appListNames;
	private String error;
	
	private Button verifyButton;
	public ExpressWizardFragment() {
		// TODO Auto-generated constructor stub
	}
	
	public boolean hasComposite() {
		return true;
	}
	
	private String user, pass, app, remote;
	private Text userText, passText, remoteText;
	private Combo appNameCombo;
	public Composite createComposite(Composite parent, IWizardHandle handle) {
		this.handle = handle;
		handle.setTitle("Create an Openshift Server");
		handle.setDescription("Create an Openshift Server adapter by typing in your credentials and choosing an application.");
		Composite composite = createWidgets(parent);
		addListeners();
		setComplete(false);
		handle.update();
		return composite;
	}
	
	private Composite createWidgets(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		Label userLabel = new Label(composite, SWT.NONE);
		userText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(userText);
		Label passLabel = new Label(composite, SWT.NONE);
		passText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(passText);
		Label appNameLabel = new Label(composite, SWT.NONE);
		appNameCombo = new Combo(composite, SWT.SINGLE);
		Label remoteLabel = new Label(composite, SWT.NONE);
		remoteText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridDataFactory.fillDefaults().hint(150, SWT.DEFAULT).applyTo(remoteText);
		verifyButton = new Button(composite, SWT.PUSH);
		
		// Text
		userLabel.setText("Username: ");
		passLabel.setText("Password: ");
		appNameLabel.setText("Application Name: ");
		remoteLabel.setText("Remote: ");
		verifyButton.setText("Verify...");
		return composite;
	}
	
	private void addListeners() {
		// add listeners
		ModifyListener l = new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				widgetsUpdated();
			}
		};
		userText.addModifyListener(l);
		passText.addModifyListener(l);
		appNameCombo.addModifyListener(l);
		remoteText.addModifyListener(l);
		appNameCombo.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e) {
				widgetsUpdated();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetsUpdated();
			}
		});
		verifyButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				verifyPressed();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				verifyPressed();
			}
			
		});
	}
	
	public void enter() {
		// do nothing
	}

	private void widgetsUpdated() {
		this.user = userText.getText();
		this.pass = passText.getText();
		this.app = appNameCombo.getText();
		this.remote = remoteText.getText();
		this.verifyButton.setEnabled(true);
		setComplete(false);
		handle.update();
	}
	
	private void verifyPressed() {
		final CredentialsWizardPageModel model = new CredentialsWizardPageModel(null);
		this.fapplication = null;
		this.fuser = null;
		this.appListNames = null;
		model.setPassword(pass);
		model.setRhLogin(user);
		verifyButton.setEnabled(false);
		setComplete(false);
		handle.update();
		final Runnable runnable = getVerifyingCredentialsJob(model);
		Job j = new Job("Verify Pressed") {
			protected IStatus run(IProgressMonitor monitor) {
				runnable.run();
				return Status.OK_STATUS;
			}
		};
		IWizardContainer container = ((WizardPage)handle).getWizard().getContainer();
		try {
			WizardUtils.runInWizardSynchronous(j, null, container);
			postLongRunningValidate();
		} catch(Exception e) {
			
		}
	}

	private void postLongRunningValidate() {
		if( appListNames == null ) {
			appListNames = new String[0];
		}
		int index = Arrays.asList(appListNames).indexOf(app);
		appNameCombo.setItems(appListNames);
		if( index != -1 )
			appNameCombo.select(index);
		handle.setMessage(error, IMessageProvider.ERROR);
		verifyButton.setEnabled(true);
		setComplete(ExpressWizardFragment.this.fapplication != null );
		handle.update();
	}
	
	private Runnable getVerifyingCredentialsJob(final CredentialsWizardPageModel model) {
		return new Runnable() {
			public void run() {
				final IStatus s = model.validateCredentials();
				String error = null;
				String[] appNames = new String[]{};
				if( !s.isOK() ) {
					error = "Credentials Failed";
				} else {
					// now check the app name and cartridge
					try {
						final List<IApplication> allApps = model.getUser().getApplications();
						appNames = getAppNamesAsStrings(allApps);
						int index = Arrays.asList(appNames).indexOf(app);
						IApplication application = index == -1 ? null : allApps.get(index);
						appListNames = appNames;
						if( application == null ) {
							error = "Application " + app + " not found";
						} else {
							// Fill with new data
							try {
								ExpressWizardFragment.this.fapplication = application;
								ExpressWizardFragment.this.fuser = model.getUser();
								
								// update the values 
								IServerWorkingCopy wc = (IServerWorkingCopy)getTaskModel().getObject(TaskModel.TASK_SERVER);
								ExpressServerUtils.fillServerWithOpenShiftDetails(wc, application, model.getUser(),
										ExpressServerUtils.EXPRESS_SOURCE_MODE, remote);
							} catch(CoreException ce) {
								// TODO FIX HANDLE
							}
						}
					} catch(OpenShiftException ose) {
						error = "Application \"" + app + "\" not found: " + ose.getMessage();
					}
					ExpressWizardFragment.this.error = error;
				}
			}
		};
	}
	
	private String[] getAppNamesAsStrings(List<IApplication> allApps) {
		String[] appNames = new String[allApps.size()];
		for( int i = 0; i < allApps.size(); i++ ) {
			appNames[i] = allApps.get(i).getName();
		}
		return appNames;
	}
	
	public void performFinish(IProgressMonitor monitor) throws CoreException {
		// do nothing
	}

}
