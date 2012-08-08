package org.jboss.tools.openshift.ui.bot.test.explorer;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.config.Annotations.Require;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

@Require(clearWorkspace = true)
public class Connection extends SWTTestExt {

	@Test
	public void canCreateConnectionToOpenShiftAccount() {
		// open OpenShift Explorer
		open.viewOpen(OpenShiftUI.Explorer.iView);

		bot.toolbarButtonWithTooltip(OpenShiftUI.Labels.CONNECT_TO_OPENSHIFT)
				.click();

		// open credentials dialog
		bot.waitForShell(OpenShiftUI.Shell.CREDENTIALS);

		// set wrong user credentials
		bot.text(0).setText(TestProperties.get("openshift.user.name"));
		bot.text(1).setText(TestProperties.get("openshift.user.wrongpwd"));
		bot.checkBox(0).deselect();

		SWTBotButton finishButton = bot.button(IDELabel.Button.FINISH);
		// try to move forward
		finishButton.click();

		// wait for credentials validation
		bot.waitUntil(new NonSystemJobRunsCondition());

		assertFalse("Finish button shouldn't be enabled.",
				finishButton.isEnabled());

		// set correct user credentials and save it to secure storage
		bot.text(0).setText(TestProperties.get("openshift.user.name"));
		bot.text(1).setText(TestProperties.get("openshift.user.pwd"));

		// create connection to OpenShift account
		finishButton.click();

		bot.waitUntil(Conditions.shellCloses(bot.activeShell()));

		log.info("*** OpenShift SWTBot Tests: Credentials validated. ***");
		log.info("*** OpenShift SWTBot Tests: Connection to OpenShift established. ***");
	}
}
