package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class CreateAppAS7 extends SWTTestExt {

	@Test
	public void canCreateApplication() {

		SWTBotText appNameText = bot.textInGroup("New application", 0);
		bot.waitUntil(Conditions.widgetIsEnabled(appNameText));
		
		assertTrue("App name should be empty!", appNameText.getText()
				.equals(""));

		appNameText.setText(TestProperties
				.getProperty("openshift.jbossapp.name"));
		
		log.info("OpenShift SWTBot Tests: Application name set.");
		
		SWTBotCombo appTypeCombo = bot.comboBoxInGroup("New application");
		bot.waitUntil(Conditions.widgetIsEnabled(appNameText));
		appTypeCombo.setSelection(OpenShiftUI.AppType.JBOSS);
		
		log.info("OpenShift SWTBot Tests: Application type selected.");
		
		bot.button(IDELabel.Button.NEXT).click();

		bot.waitUntil(Conditions.widgetIsEnabled(bot
				.button(IDELabel.Button.FINISH)));
		bot.button(IDELabel.Button.FINISH).click();

		log.info("OpenShift SWTBot Tests: Application creation started.");
		
		bot.waitForShell("Information", 500);
		bot.text(0).setText(TestProperties.getPassphrase());
		bot.button(IDELabel.Button.OK).click();

		log.info("OpenShift SWTBot Tests: SSH passphrase given.");
		
		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_20S);
		
		log.info("OpenShift SWTBot Tests: 'New Application wizard' was closed.");
		
		assertNotNull("OpenShift Server runtime is not in the Servers View!",
				servers.findServerByName(servers.show().bot().tree(),
						TestProperties.getProperty("openshift.jbossapp.name")));
	}

}
