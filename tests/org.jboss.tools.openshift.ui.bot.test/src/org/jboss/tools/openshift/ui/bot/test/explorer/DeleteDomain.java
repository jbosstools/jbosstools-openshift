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

		explorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_DELETE_DOMAIN).click();

		
		bot.wait(TIME_5S);
		
		bot.waitUntil(new NonSystemJobRunsCondition());
		
		
	}
}
