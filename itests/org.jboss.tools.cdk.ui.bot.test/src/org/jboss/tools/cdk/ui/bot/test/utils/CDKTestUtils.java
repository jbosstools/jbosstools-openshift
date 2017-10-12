/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.ui.bot.test.utils;

import java.util.List;
import java.util.Map;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithMnemonicTextMatcher;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.exception.EclipseLayerException;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.eclipse.wst.server.ui.wizard.NewServerWizard;
import org.eclipse.reddeer.jface.exception.JFaceLayerException;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.clabel.DefaultCLabel;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;

/**
 * Utilities for CDK tests
 * @author odockal
 *
 */
public class CDKTestUtils {

	private static Logger log = Logger.getLogger(CDKTestUtils.class);
	
	public static String getSystemProperty(String systemProperty) {
		String property = System.getProperty(systemProperty);
		if (!(property == null || property.equals("") || property.startsWith("${"))) { //$NON-NLS-1$ //$NON-NLS-2$
			return property;
		}
		return null;
	}
	
	public static void checkParameterNotNull(Map<String, String> dict) {
		for (String key : dict.keySet()) {
			String value = dict.get(key);
			if (value == null) {
				throw new RedDeerException("Given key " + key + " value is null"); //$NON-NLS-1$
			}
			log.info("Given key " + key + " value is " + value);
		}	
	}
	
	public static void deleteCDEServer(String adapter) {
		log.info("Deleting Container Development Environment server adapter:" + adapter); //$NON-NLS-1$
		ServersView2 servers = new ServersView2();
		servers.open();
		try {
			servers.getServer(adapter).delete(true);
		} catch (EclipseLayerException exc) {
			log.error(exc.getMessage());
			exc.printStackTrace();
		}
	}
	
	public static List<Server> getAllServers() {
		log.info("Collecting all server adapters");
		ServersView2 view = new ServersView2();
		view.open();
		return view.getServers();
	}
	
	public static void deleteAllCDEServers(String name) {
		log.info("Deleting all server containing '" + name + "' string");
		for (Server server : getAllServers()) {
			String label = server.getLabel().getName();
			log.info("Working with " + label);
			if (label.contains(name)) {
				deleteCDEServer(label);
			}
		}
	}
	
	public static NewServerWizard openNewServerWizardDialog() {
		log.info("Adding new Container Development Environment server adapter"); //$NON-NLS-1$
		// call new server dialog from servers view
		ServersView2 view = new ServersView2();
		view.open();
		NewServerWizard dialog = view.newServer();
		new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
		return dialog;
	}
	
	// removes access redhat com credentials used for first cdk run
	public static void removeAccessRedHatCredentials(String domain, String username) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		dialog.select("JBoss Tools", "Credentials"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
	        new WaitUntil(new WidgetIsFound(
	        		org.eclipse.swt.custom.CLabel.class, 
	        		new WithMnemonicTextMatcher("Credentials")),
	        		TimePeriod.MEDIUM); //$NON-NLS-1$
	        new DefaultCLabel("Credentials"); //$NON-NLS-1$
	        DefaultTree tree = new DefaultTree(1);
	        TreeItem item = TreeViewerHandler.getInstance().getTreeItem(tree, new String[]{domain, username});
	        item.select();
	        new PushButton(new WithTextMatcher("Remove User")).click(); //$NON-NLS-1$
	        new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
        } catch (WaitTimeoutExpiredException exc) {
        	log.error("JBoss Tools - Credentials preferences page has timed out"); //$NON-NLS-1$
        	exc.printStackTrace();
        } catch (JFaceLayerException exc) {
        	log.error("JBoss Tools - Credentials does not contain required username to be deleted"); //$NON-NLS-1$
        	exc.printStackTrace();
        } finally {
        	dialog.ok();
		}
	}

}
