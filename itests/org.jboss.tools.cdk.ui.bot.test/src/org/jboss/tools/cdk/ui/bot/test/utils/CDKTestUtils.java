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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.exception.WaitTimeoutExpiredException;
import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithMnemonicTextMatcher;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.Server;
import org.eclipse.reddeer.eclipse.wst.server.ui.cnf.ServersView2;
import org.eclipse.reddeer.jface.exception.JFaceLayerException;
import org.eclipse.reddeer.jface.handler.TreeViewerHandler;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.clabel.DefaultCLabel;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.cdk.reddeer.core.label.CDKLabel;
import org.jboss.tools.cdk.reddeer.server.exception.CDKException;
import org.jboss.tools.cdk.reddeer.server.ui.CDKServersView;
import org.jboss.tools.cdk.reddeer.server.ui.wizard.NewCDKServerWizard;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;

/**
 * Utilities for CDK tests
 * @author odockal
 *
 */
public class CDKTestUtils {

	private static Logger log = Logger.getLogger(CDKTestUtils.class);
	
	public static String getSystemProperty(String systemProperty) {
		String property = System.getProperty(systemProperty);
		if (!(property == null || property.equals("") || property.startsWith("${"))) {  
			return property;
		}
		return null;
	}
	
	public static void checkParameterNotNull(Map<String, String> dict) {
		for (String key : dict.keySet()) {
			String value = dict.get(key);
			if (value == null) {
				throw new RedDeerException("Given key " + key + " value is null");  
			}
			log.info("Given key " + key + " value is " + value);  
		}	
	}
	
	public static List<Server> getAllServers() {
		log.info("Collecting all server adapters"); 
		ServersView2 view = new ServersView2();
		view.open();
		return view.getServers();
	}
	
	public static NewCDKServerWizard openNewServerWizardDialog() {
		log.info("Adding new Container Development Environment server adapter"); 
		// call new server dialog from servers view
		CDKServersView view = new CDKServersView();
		view.open();
		NewCDKServerWizard dialog = view.newCDKServer();
		new WaitWhile(new JobIsRunning(), TimePeriod.MEDIUM, false);
		return dialog;
	}
	
	
	public static void setOCToPreferences(String ocPath) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		OpenShift3PreferencePage page = new OpenShift3PreferencePage(dialog);
		
		dialog.open();
		dialog.select(page);
		
		page.setOCLocation(ocPath);
		try {
			new WaitUntil(new ControlIsEnabled(new PushButton(CDKLabel.Buttons.APPLY)), TimePeriod.DEFAULT); 
		} catch (WaitTimeoutExpiredException exc) {
			fail("WaitTimeoutExpiredException occured while processing oc binary on path " + ocPath); 
		}
		page.apply();
		dialog.cancel();
	}
	
	public static String findFileOnPath(String path, String fileToFind) {
		log.info("Searching for " + fileToFind + " on " + path);  
		File startPath = new File(path);
		if (startPath.exists()) {
			try {
				return Files.find(startPath.toPath(), 10, 
						(p, attrs) -> fileFitsCondition(p, attrs, p.getFileName().toString().equalsIgnoreCase(fileToFind)
						&& attrs.isRegularFile()), FileVisitOption.FOLLOW_LINKS).findAny().get().toString();
			} catch (NoSuchElementException noExc) {
				throw new CDKException("There was not found any oc file in given path: " + path); 
			} catch (IOException e) {
				throw new CDKException("Could not find oc on given path: " + path); 
			}
		} else {
			throw new CDKException("Given path " + path + " does not exist");  
		}
	}
	
	private static boolean fileFitsCondition(Path p, BasicFileAttributes attrs, boolean condition) {
		log.debug("Actual path: \"" + p.getFileName().toString() + "\" and is regular file: " + attrs.isRegularFile());  
		return condition;
	}
	
	// removes access redhat com credentials used for first cdk run
	public static void removeAccessRedHatCredentials(String domain, String username) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		dialog.select("JBoss Tools", "Credentials");  
        try {
	        new WaitUntil(new WidgetIsFound(
	        		org.eclipse.swt.custom.CLabel.class, 
	        		new WithMnemonicTextMatcher("Credentials")), 
	        		TimePeriod.MEDIUM);
	        new DefaultCLabel("Credentials"); 
	        DefaultTree tree = new DefaultTree(1);
	        TreeItem item = TreeViewerHandler.getInstance().getTreeItem(tree, new String[]{domain, username});
	        item.select();
	        new PushButton(new WithTextMatcher(CDKLabel.Buttons.REMOVE_USER)).click(); 
	        new WaitUntil(new JobIsRunning(), TimePeriod.MEDIUM, false);
        } catch (WaitTimeoutExpiredException exc) {
        	log.error("JBoss Tools - Credentials preferences page has timed out"); 
        	exc.printStackTrace();
        } catch (JFaceLayerException exc) {
        	log.error("JBoss Tools - Credentials does not contain required username to be deleted"); 
        	exc.printStackTrace();
        } finally {
        	dialog.ok();
		}
	}

}
