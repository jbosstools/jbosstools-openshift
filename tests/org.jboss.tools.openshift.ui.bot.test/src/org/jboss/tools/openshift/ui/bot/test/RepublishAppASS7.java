package org.jboss.tools.openshift.ui.bot.test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.jboss.tools.ui.bot.ext.helper.ContextMenuHelper;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class RepublishAppASS7 extends SWTTestExt {

	@Test
	public void canModifyAndRepublishApp() {
		SWTBot wiz = open.newObject(ActionItem.NewObject.WebHTMLPage.LABEL);
		wiz.text(0).setText(
				TestProperties.getProperty("openshift.jbossapp.name")
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
		//bot.toolbarButtonWithTooltip("Select All").click();

		bot.button("Commit").click();

		bot.waitForShell("Push to Another Repository");
		bot.button(IDELabel.Button.FINISH).click();

		bot.waitForShell("Push Results: origin", 200);
		bot.button(IDELabel.Button.OK).click();
		
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod("https://"
				+ TestProperties.getProperty("openshift.jbossapp.name") + "-"
				+ TestProperties.getProperty("openshift.domain")
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
