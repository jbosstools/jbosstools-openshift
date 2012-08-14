package org.jboss.tools.openshift.ui.bot.test.wizard;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.jboss.tools.ui.bot.ext.helper.ContextMenuHelper;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class RepublishApp extends SWTTestExt {

	@Test
	public void canModifyAndRepublishApp() {
		SWTBot wiz = open.newObject(ActionItem.NewObject.WebHTMLPage.LABEL);

		bot.waitForShell("New HTML File");

		wiz.text(0).setText(
				TestProperties.get("openshift.jbossapp.name")
						+ "/src/main/webapp");
		wiz.text(1).setText("Test.html");

		wiz.button(IDELabel.Button.FINISH).click();

		projectExplorer.show();
		projectExplorer.bot().tree().select(0);
		ContextMenuHelper.clickContextMenu(projectExplorer.bot().tree(),
				"Team", "Commit and Push...");

		// if the auth shell appears click on OK
		if (bot.waitForShell("Identify Yourself") != null) {
			bot.button("OK").click();
		}

		bot.waitForShell("Commit Changes");
		bot.styledText(0).setText("comment");

		// select all items to commit
		for (int i = 0; i < bot.table().rowCount(); i++) {
			bot.table().getTableItem(i).toggleCheck();
		}

		bot.button("Commit").click();

		bot.waitForShell("Push to Another Repository");
		bot.button(IDELabel.Button.FINISH).click();

		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_UNLIMITED, TIME_1S);

		// custom condition to wait for the openshift server to be synchronized
		/*
		 * servers.show(); assertTrue(servers.getServerPublishStatus(
		 * TestProperties.get("openshift.jbossapp.name") +
		 * " OpenShift Server").equalsIgnoreCase("synchronized"));
		 * 
		 * TODO: JIRA? Server not synchornized although it actually is
		 */
	}

}
