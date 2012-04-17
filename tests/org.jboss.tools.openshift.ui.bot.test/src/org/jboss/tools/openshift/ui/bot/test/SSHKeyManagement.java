package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.hamcrest.Matcher;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class SSHKeyManagement extends SWTTestExt {
	
	@Test
	public void canCreateSSHKey() {

		bot.waitForShell("", 100);

		@SuppressWarnings("unchecked")
		Matcher<Widget> matcher = WidgetMatcherFactory.allOf(
				WidgetMatcherFactory.widgetOfType(Shell.class),
				WidgetMatcherFactory.withText(""));

		bot.waitUntilWidgetAppears(Conditions.waitForWidget(matcher));

		log.info("OpenShift SWTBot Tests: SSH Keys creation.");
		
		bot.link(0).click("SSH2 Preferences");
		bot.waitForShell(IDELabel.Shell.PREFERENCES);

		log.info("OpenShift SWTBot Tests: SSH Preferences opened.");
		
		//SWTBotText sshDirText = bot.text(1);
		//sshDirText.setText(System.getProperty("user.home") + "/.ssh2");

		bot.button(IDELabel.Button.OK).click();
		bot.waitUntilWidgetAppears(Conditions.waitForWidget(matcher));

		bot.button(IDELabel.Shell.NEW).click();
		bot.waitForShell("New ssh key");
		bot.text(0).setText(TestProperties.getPassphrase());
		bot.button(IDELabel.Button.OK).click();

		log.info("OpenShift SWTBot Tests: SSH Keys created.");
		
		bot.waitUntilWidgetAppears(Conditions.waitForWidget(matcher));
	}

}
