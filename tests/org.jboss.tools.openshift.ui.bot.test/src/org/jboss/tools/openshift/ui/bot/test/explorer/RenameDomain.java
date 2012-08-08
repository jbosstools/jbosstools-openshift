package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class RenameDomain extends SWTTestExt {

	@Test
	public void canRenameDomain() {

		SWTBotView explorer = open.viewOpen(OpenShiftUI.Explorer.iView);

		explorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_CREATE_EDIT_DOMAIN)
				.click();

		bot.waitForShell("");

		SWTBotText domainText = bot.text(0);

		assertTrue(
				"Domain should be set correctly at this stage!",
				domainText.getText().equals(
						TestProperties.get("openshift.domain")));

		domainText.setText(TestProperties.get("openshift.domain.new"));

		bot.button(IDELabel.Button.FINISH).click();
		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_60S);

		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod("https://"
				+ TestProperties.get("openshift.jbossapp.name") + "-"
				+ TestProperties.get("openshift.domain.new")
				+ ".rhcloud.com");

		try {
			assertTrue(client.executeMethod(method) == 200);
		} catch (Exception e) {
			log.error("File has not been published to the server!", e);
		} finally {
			method.releaseConnection();
		}
	}

}
