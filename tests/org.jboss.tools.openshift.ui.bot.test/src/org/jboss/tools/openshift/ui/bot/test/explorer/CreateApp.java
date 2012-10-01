package org.jboss.tools.openshift.ui.bot.test.explorer;

import java.io.File;
import java.io.IOException;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Before;
import org.junit.Test;

public class CreateApp extends SWTTestExt {

	@Before
	public void cleanUpProject() {
		File gitDir = new File(System.getProperty("user.home") + "/git");

		boolean exists = gitDir.exists() ?  true : gitDir.mkdir(); 
		
		if (exists && gitDir.isDirectory()
				&& gitDir.listFiles().length > 0) {
			for (File file : gitDir.listFiles()) {
				if (file.getName().contains(
						TestProperties.get("openshift.jbossapp.name")))
					try {
						delete(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
	}	

	@Test
	public void canCreateImportAppFromExplorer() {
		SWTBotView openshiftConsole = open.viewOpen(OpenShiftUI.Explorer.iView);

		openshiftConsole.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_NEW_APP).click();

		bot.waitForShell(OpenShiftUI.Shell.NEW_APP);

		// fill app info
		SWTBotText appNameText = bot.textInGroup("New application", 0);
		bot.waitUntil(Conditions.widgetIsEnabled(appNameText));

		assertTrue("App name should be empty!", appNameText.getText()
				.equals(""));

		appNameText.setText(TestProperties.get("openshift.jbossapp.name"));

		log.info("*** OpenShift SWTBot Tests: Application name set. ***");

		SWTBotCombo appTypeCombo = bot.comboBoxInGroup("New application");
		bot.waitUntil(Conditions.widgetIsEnabled(appNameText));
		appTypeCombo.setSelection(OpenShiftUI.AppType.JBOSS);

		log.info("*** OpenShift SWTBot Tests: Application type selected. ***");

		bot.button(IDELabel.Button.NEXT).click();

		bot.button(IDELabel.Button.NEXT).click();

		bot.waitUntil(Conditions.widgetIsEnabled(bot
				.button(IDELabel.Button.FINISH)));
		bot.button(IDELabel.Button.FINISH).click();

		log.info("*** OpenShift SWTBot Tests: Application creation started. ***");

		// only for the 1st time - with known_hosts deleting it will appear every time
		// add to known_hosts
		bot.waitForShell("Question", TIME_60S * 3);
		bot.button(IDELabel.Button.YES).click();
		// create known_hosts since it does not exists any more
		bot.waitForShell("Question", TIME_60S * 3);
		bot.button(IDELabel.Button.YES).click();
		
/*
		bot.waitForShell("Information", TIME_60S * 3);
		bot.text(0).setText(TestProperties.get("openshift.user.pwd"));
		bot.button(IDELabel.Button.OK).click();
*/
		log.info("*** OpenShift SWTBot Tests: SSH passphrase given. ***");

		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_60S, TIME_1S);

		log.info("*** OpenShift SWTBot Tests: New Application wizard closed. ***");

		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_60S, TIME_1S);

		servers.serverExists(TestProperties.get("openshift.jbossapp.name")
				+ " OpenShift Server");

		log.info("*** OpenShift SWTBot Tests: OpenShift Server Adapter created. ***");
	}
	
	private void delete(File file) throws IOException {

		if (file.isDirectory()) {
			// directory is empty, then delete it
			if (file.list().length == 0) {
				file.delete();
				log.debug("Directory is deleted : "
						+ file.getAbsolutePath());
			} else {
				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);
					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
					log.debug("Directory is deleted : "
							+ file.getAbsolutePath());
				}
			}
		} else {
			// if file, then delete it
			file.delete();
			log.debug("File is deleted : " + file.getAbsolutePath());
		}
	}

}
