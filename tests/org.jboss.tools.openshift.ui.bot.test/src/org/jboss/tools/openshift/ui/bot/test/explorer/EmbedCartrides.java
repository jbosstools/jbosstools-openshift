package org.jboss.tools.openshift.ui.bot.test.explorer;

import java.util.StringTokenizer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class EmbedCartrides extends SWTTestExt {
	SWTBotTreeItem account;

	@Test
	public void canEmbeddCartriges() {

		SWTBotView explorer = open.viewOpen(OpenShiftUI.Explorer.iView);

		account = explorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.doubleClick();

		// TODO: cannot find widget, not needed actually
		// bot.toolbarButtonWithTooltip("Collapse All").click();

		// custom condition to wait for app to show
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() {

				for (SWTBotTreeItem item : account.getItems()) {
					if (item.getText().contains(
							TestProperties.get("openshift.jbossapp.name"))) {
						return true;
					}
				}
				return false;
			}

			@Override
			public void init(SWTBot bot) {
				// keep empty
			}

			@Override
			public String getFailureMessage() {
				return "Application did not appear in user account after reasonable timeout.";
			}

		}, TIME_60S, TIME_1S);

		account.getNode(0).contextMenu(OpenShiftUI.Labels.EDIT_CARTRIDGES)
				.click();

		bot.waitForShell("");

		SWTBotTable cartridgeTable = bot.tableInGroup("Embeddable Cartridges");

		selectCartridges(cartridgeTable);

		bot.button(IDELabel.Button.FINISH).click();

		bot.waitForShell("Embedded Cartridges", TIME_60S + TIME_30S);
		bot.button(IDELabel.Button.OK).click();
	}

	/*
	 * Jenkins cartridge not supported yet
	 */
	private void selectCartridges(SWTBotTable cartridgeTable) {

		StringTokenizer tokenizer = new StringTokenizer(
				TestProperties.get("openshift.jbossapp.cartridges"), ";");

		while (tokenizer.hasMoreTokens()) {

			String cartridge = tokenizer.nextToken();

			if (cartridge.equals("mysql")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.MYSQL)
						.toggleCheck();
			}
			if (cartridge.equals("phpmyadmin")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.PHPMYADMIN)
						.toggleCheck();
			}
			if (cartridge.equals("cron")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.CRON)
						.toggleCheck();
			}
			if (cartridge.equals("postgresql")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.POSTGRESQL)
						.toggleCheck();
			}
			if (cartridge.equals("mongodb")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.MONGODB)
						.toggleCheck();
			}
			if (cartridge.equals("rockmongo")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.ROCKMONGO)
						.toggleCheck();
			}
			if (cartridge.equals("metrics")) {
				cartridgeTable.getTableItem(OpenShiftUI.Cartridge.METRICS)
						.toggleCheck();
			}
		}
	}

}
