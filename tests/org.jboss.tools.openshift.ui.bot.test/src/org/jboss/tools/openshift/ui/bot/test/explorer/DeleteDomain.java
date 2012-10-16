package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.junit.Test;

public class DeleteDomain extends SWTTestExt {

	@Test
	public void canDestroyDomain() throws InterruptedException {

		SWTBotView explorer = open.viewOpen(OpenShiftUI.Explorer.iView);

		// refresh first
		explorer.bot().tree()
				.getTreeItem(
						TestProperties.get("openshift.user.name") + " "
								+ TestProperties.get("openshift.server.prod"))
				.contextMenu(OpenShiftUI.Labels.REFRESH).click();

		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_60S * 3, TIME_1S);
		
		// delete
		explorer.bot().tree()
				.getTreeItem(
						TestProperties.get("openshift.user.name") + " "
								+ TestProperties.get("openshift.server.prod"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_DELETE_DOMAIN).click();

		bot.checkBox().select();
		bot.button("OK").click();

		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_60S * 4, TIME_1S);		
	}
}
