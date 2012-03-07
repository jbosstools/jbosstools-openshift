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
		
		SWTBotCombo appTypeCombo = bot.comboBoxInGroup("New application");
		bot.waitUntil(Conditions.widgetIsEnabled(appNameText));
		appTypeCombo.setSelection(OpenShiftUI.AppType.JBOSS);
		
		bot.button(IDELabel.Button.NEXT).click();

		bot.waitUntil(Conditions.widgetIsEnabled(bot
				.button(IDELabel.Button.FINISH)));
		bot.button(IDELabel.Button.FINISH).click();

		bot.waitForShell("Information", 500);
		bot.text(0).setText(TestProperties.getPassphrase());
		bot.button(IDELabel.Button.OK).click();

		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_20S);
		
		assertNotNull("OpenShift Server runtime is not in the Servers View!",
				servers.findServerByName(servers.show().bot().tree(),
						TestProperties.getProperty("openshift.jbossapp.name")));
	}

}
