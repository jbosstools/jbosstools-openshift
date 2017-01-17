/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.Git;
import org.jboss.reddeer.swt.impl.button.CheckBox;
import org.jboss.reddeer.swt.impl.button.PushButton;
import org.jboss.reddeer.swt.impl.button.RadioButton;
import org.jboss.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement;

public class TestUtils {

	public static void setUpOcBinary() {
		setOCBinaryPath(true);
	}
	
	public static void cleanUpOCBinary() {
		setOCBinaryPath(false);
	}
	
	private static void setOCBinaryPath(boolean setUp) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		OpenShift3PreferencePage page = new OpenShift3PreferencePage();
		dialog.open();
		dialog.select(page);
		if (setUp) {
			page.setOCLocation(OpenShiftCommandLineToolsRequirement.getOCLocation());
		} else {
			page.clearOCLocation();
		}
		page.apply();
		dialog.ok();
	}
	
	public static void setVisualEditorToUseHTML5() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		dialog.select("JBoss Tools", "Web", "Editors", "Visual Page Editor");
		
		RadioButton button = new RadioButton("HTML5 (use WebKit)");
		if (button.isEnabled() && !button.isSelected()) {
			button.click();
		}
		
		CheckBox checkBox = new CheckBox("Do not show Browser Engine dialog");
		if (checkBox.isEnabled() && !checkBox.isChecked()) {
			checkBox.click();
		}
		
		new PushButton("Apply").click();
		dialog.ok();
	}
	
	public static void cleanupGitFolder(String appname) {
		File gitDir = new File(System.getProperty("user.home") + File.separatorChar + "git");
		
		boolean exists = gitDir.exists() ? true : gitDir.mkdir();

		if (exists && gitDir.isDirectory() && gitDir.listFiles().length > 0) {
			for (File file : gitDir.listFiles()) {
				if (file.getName().contains(appname)) {
					if (file.isDirectory()) {
						closeGitRepository(file);
					}
					try {
						TestUtils.delete(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void closeGitRepository(File repoDir) {
		try {
			Git git = Git.open(repoDir);
			git.getRepository().close();
			git.close();
		} catch (IOException ex) {
			// DO NOTHING
		}
	}
	
	public static void delete(File file) throws IOException {
		if (file.isDirectory() && file.list().length > 0) {
			String files[] = file.list();
			for (String tmpFile : files) {
				File fileToDelete = new File(file, tmpFile);
				delete(fileToDelete);
			}
		}
		
		file.delete();
	}
	
	public static String getValueOrDefault(String value, String defaultValue) {
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		return value;
	}

	/**
	 * Finds out whether a URL returns HTTP OK or not.
	 * 
	 * @param URL URL to find out whether is accessible 
	 * @return true if URL is accesible with HTTP OK exit code (200), false otherwise
	 */
	public static boolean isURLAccessible(String URL) {
		try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
            connection.setRequestMethod("HEAD");
            return (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
	}
}
