/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.openshift.cdk.server.ui.internal.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.Messages;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.internal.wizard.fragment.ModifyModulesWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.NewServerWizardFragment;
import org.eclipse.wst.server.ui.internal.wizard.fragment.TasksWizardFragment;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.tools.openshift.cdk.server.core.internal.CDKCoreActivator;
import org.jboss.tools.openshift.cdk.server.ui.internal.dialogs.ChooseServerDialog;

public class LaunchCDKServerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IServer s = findCDKServer();
		if( s != null ) {
			try {
				s.start("run", new NullProgressMonitor());
			} catch(CoreException ce) {
				CDKCoreActivator.getDefault().getLog().log(ce.getStatus());
			}
		}
		return null;
	}

	private void trimNotStopped(ArrayList<IServer> servers) {
		// Find ones that are not running
		Iterator<IServer> i = servers.iterator();
		while(i.hasNext()) {
			IServer next = i.next();
			if( next.getServerState() != IServer.STATE_STOPPED) {
				i.remove();
			}
		}
	}
	
	private IServer findCDKServer() {
		ArrayList<IServer> allCDK  = findCDKServers();
		if( allCDK.size() == 0 ) {
			// find a new one
			return showCreateNewServerDialog();
		} else {
			trimNotStopped(allCDK);
			// Now we have only stopped ones
			if( allCDK.size() == 0 ) {
				// There exist cdk servers, but all are already started or starting or stopping
				showAllRunningError();
			} else {
				if( allCDK.size() > 1 ) {
					// More than 1 possible cdk server to launch
					return showSelectServerDialog(allCDK);
				}
				return allCDK.get(0);
			}
		}
		return null;
	}
	
	private IServer showSelectServerDialog(ArrayList<IServer> valid) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ChooseServerDialog dialog = new ChooseServerDialog(shell, valid);
		int ret = dialog.open();
		if( ret == Window.OK) {
			return dialog.getServer();
		}
		return null;
	}
	
	private void showAllRunningError() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		String msg = "All available CDK servers are already running";
		MessageDialog.openError(shell, msg, msg);
	}
	
	private IServer showCreateNewServerDialog() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		IServer created = showNewServerWizard(shell, "org.jboss.tools.openshift.cdk.server.type");
		return created; 
	}
	
	private ArrayList<IServer> findCDKServers() {
		ArrayList<IServer> cdk = new ArrayList<>();
		IServer[] all = ServerCore.getServers();
		for( int i = 0; i < all.length; i++ ) {
			if( all[i].getServerType().getId().equals("org.jboss.tools.openshift.cdk.server.type")) {
				cdk.add(all[i]);
			}
		}
		return cdk;
	}
	
	
	

	/**
	 * Open the new server wizard.
	 * 
	 * @param shell a shell
	 * @param serverTypeId a server runtime type, or null for any type
	 * @return <code>true</code> if a server was created, or
	 *    <code>false</code> otherwise
	 */
	public static IServer showNewServerWizard(Shell shell, final String serverTypeId) {
		WizardFragment fragment = new WizardFragment() {
			@Override
			protected void createChildFragments(List<WizardFragment> list) {
				list.add(new NewServerWizardFragment(null, serverTypeId));
				
				list.add(WizardTaskUtil.TempSaveRuntimeFragment);
				list.add(WizardTaskUtil.TempSaveServerFragment);
				
				list.add(new ModifyModulesWizardFragment());
				list.add(new TasksWizardFragment());
				
				list.add(WizardTaskUtil.SaveRuntimeFragment);
				list.add(WizardTaskUtil.SaveServerFragment);
				list.add(WizardTaskUtil.SaveHostnameFragment);
			}
		};
		
		TaskWizard wizard = new TaskWizard(Messages.wizNewServerWizardTitle, fragment);
		wizard.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(shell, wizard);
		int ret = (dialog.open());
		if( ret ==  IDialogConstants.OK_ID) {
			IServer server = (IServer)wizard.getTaskModel().getObject(TaskModel.TASK_SERVER);
			return server;
		}
		return null;
	}
}
