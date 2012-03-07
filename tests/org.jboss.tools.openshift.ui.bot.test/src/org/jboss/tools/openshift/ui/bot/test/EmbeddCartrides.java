package org.jboss.tools.openshift.ui.bot.test;

import java.util.StringTokenizer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class EmbeddCartrides extends SWTTestExt {

	@Test
	public void canEmbeddCartriges() {

		SWTBotView openshiftConsole = open.viewOpen(OpenShiftUI.Console.iView);

		SWTBotTreeItem account = openshiftConsole.bot().tree()
				.expandNode(TestProperties.getProperty("openshift.user.name"))
				.doubleClick();

		account.getNode(0).contextMenu("Edit Embeddable Cartridges").click();

		StringTokenizer tokenizer = new StringTokenizer(
				TestProperties.getProperty("openshift.jbossapp.cartridges"),
				";");

		bot.waitForShell("");
		
		SWTBotTable cartridgeTable = bot.tableInGroup("Embeddable Cartridges");

		selectCartridges(tokenizer, cartridgeTable);

		bot.button(IDELabel.Button.FINISH).click();

		bot.waitForShell("Embedded Cartridges");
		bot.button(IDELabel.Button.OK).click();
	}

	private void selectCartridges(StringTokenizer tokenizer,
			SWTBotTable cartridgeTable) {
		
		while (tokenizer.hasMoreTokens()) {

			String cartridge = tokenizer.nextToken();
			System.out.println(cartridge);
			if (cartridge.equals("jenkins")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.JENKINS).toggleCheck();
				bot.waitForShell("New Jenkins application");
				bot.text(0).setText("jenkins");
				bot.button(IDELabel.Button.OK).click();
				//bot.waitUntil(condition, timeout)
				bot.waitUntil(new NonSystemJobRunsCondition(), 200);
			}
			if (cartridge.equals("mysql")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.MYSQL).toggleCheck();
			}
			if (cartridge.equals("phpmyadmin")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.PHPMYADMIN).toggleCheck();
			}
			if (cartridge.equals("cron")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.CRON).toggleCheck();
			}
			if (cartridge.equals("postgresql")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.POSTGRESQL).toggleCheck();
			}
			if (cartridge.equals("mongodb")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.MONGODB).toggleCheck();
			}
			if (cartridge.equals("rockmongo")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.ROCKMONGO).toggleCheck();
			}
			if (cartridge.equals("metrics")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.METRICS).toggleCheck();
			}
		}
	}

}
