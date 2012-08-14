package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class DeleteApp extends SWTTestExt {

	SWTBotTreeItem account;

	@Test
	public void canDeleteApplication() {

		SWTBotView explorer = open.viewOpen(OpenShiftUI.Explorer.iView);

		account = explorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.doubleClick();

		account.getNode(0).contextMenu(OpenShiftUI.Labels.EXPLORER_DELETE_APP)
				.click();

		bot.waitForShell(OpenShiftUI.Shell.DELETE_APP);

		bot.button(IDELabel.Button.OK).click();
		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_60S + TIME_30S, TIME_1S);
// TODO: Explorer is not updated??? Cannot find domain
		assertTrue("Application still present in the OpenShift Console view!",
				account.getItems().length == 0);

		projectExplorer.show();
		projectExplorer.bot().tree(0).contextMenu("Delete").click();

		bot.waitForShell("Delete Resources");
		bot.checkBox().select();
		bot.button(IDELabel.Button.OK).click();

		assertFalse("The project still exists!",
				projectExplorer.existsResource(TestProperties
						.get("openshift.jbossapp.name")));
	}
}
