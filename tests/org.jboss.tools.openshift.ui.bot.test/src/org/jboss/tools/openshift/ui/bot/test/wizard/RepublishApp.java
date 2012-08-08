package org.jboss.tools.openshift.ui.bot.test.wizard;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.Timing;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.jboss.tools.ui.bot.ext.helper.ContextMenuHelper;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class RepublishApp extends SWTTestExt {

	@Test
	public void canModifyAndRepublishApp() {
		SWTBot wiz = open.newObject(ActionItem.NewObject.WebHTMLPage.LABEL);
		wiz.text(0).setText(
				TestProperties.get("openshift.jbossapp.name")
						+ "/src/main/webapp");
		wiz.text(1).setText("Test.html");

		wiz.button(IDELabel.Button.FINISH).click();

		projectExplorer.show();
		projectExplorer.bot().tree().select(0);
		ContextMenuHelper.clickContextMenu(projectExplorer.bot().tree(),
				"Team", "Commit and Push...");

		bot.waitForShell("Commit Changes");
		bot.styledText(0).setText("comment");

		bot.table().getTableItem(0).toggleCheck();
		// bot.toolbarButtonWithTooltip("Select All").click();

		bot.button("Commit").click();

		bot.waitForShell("Push to Another Repository");
		bot.button(IDELabel.Button.FINISH).click();

		// custom condition to wait for the openshift server to be synchronized
		bot.waitUntil(new ICondition() {
			@Override
			public boolean test() {

				return servers.getServerPublishStatus(
						TestProperties.get("openshift.jbossapp.name")
								+ " OpenShift Server").equalsIgnoreCase(
						"synchronized");
			}

			@Override
			public void init(SWTBot bot) {
				// keep empty
			}

			@Override
			public String getFailureMessage() {
				return "OpenShift server is not synchronized after reasonable timeout.";
			}

		}, Timing.time100S(), TIME_1S);

		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod("https://"
				+ TestProperties.get("openshift.jbossapp.name") + "-"
				+ TestProperties.get("openshift.domain")
				+ ".rhcloud.com/Test.html");

		try {
			assertTrue(client.executeMethod(method) == 200);
		} catch (Exception e) {
			log.error("File has not been published to the server!", e);
		} finally {
			method.releaseConnection();
		}
	}

}
