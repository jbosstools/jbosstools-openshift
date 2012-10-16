package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

/**
 * Domain creation consists of creating the SSH key pair, storing user password
 * in the secure storage and creating the domain itself.
 * 
 * @author sbunciak
 * 
 */
public class CreateDomain extends SWTTestExt {

	@Test
	public void canCreateDomain() throws InterruptedException {
		// open OpenShift Explorer
		SWTBotView openshiftExplorer = open
				.viewOpen(OpenShiftUI.Explorer.iView);

		openshiftExplorer
				.bot()
				.tree()
				.getTreeItem(
						TestProperties.get("openshift.user.name") + " "
								+ TestProperties.get("openshift.server.prod"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_CREATE_EDIT_DOMAIN)
				.click();

		bot.waitForShell(OpenShiftUI.Shell.CREATE_DOMAIN);

		SWTBotText domainText = bot.text(0);

		assertTrue("Domain should not be set at this stage!", domainText
				.getText().equals(""));

		domainText.setText(TestProperties.get("openshift.domain"));
		log.info("*** OpenShift SWTBot Tests: Domain name set. ***");

		SWTBotButton finishBtn = bot.button(IDELabel.Button.FINISH);

		bot.waitUntil(Conditions.widgetIsEnabled(finishBtn));
		finishBtn.click();

		// wait while the domain is being created
		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_60S * 3,
				TIME_1S);
		bot.waitWhile(new NonSystemJobRunsCondition(), TIME_20S, TIME_1S);

		log.info("*** OpenShift SWTBot Tests: Domain created. ***");
	}

}
