package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class CreateApp extends SWTTestExt {

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

		bot.waitUntil(Conditions.widgetIsEnabled(bot
				.button(IDELabel.Button.FINISH)));
		bot.button(IDELabel.Button.FINISH).click();

		log.info("*** OpenShift SWTBot Tests: Application creation started. ***");

		bot.waitForShell("Information", 500);
		bot.text(0).setText(TestProperties.get("openshift.user.pwd"));
		bot.button(IDELabel.Button.OK).click();

		log.info("*** OpenShift SWTBot Tests: SSH passphrase given. ***");

		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_20S);

		log.info("*** OpenShift SWTBot Tests: New Application wizard closed. ***");

		servers.serverExists(TestProperties.get("openshift.jbossapp.name")
				+ " OpenShift Server");

		log.info("*** OpenShift SWTBot Tests: OpenShift Server Adapter created. ***");
	}

}
