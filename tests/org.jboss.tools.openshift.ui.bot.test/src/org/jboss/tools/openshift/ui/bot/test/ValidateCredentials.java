package org.jboss.tools.openshift.ui.bot.test;

import java.io.File;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.config.Annotations.Require;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Before;
import org.junit.Test;

@Require(clearWorkspace = true)
public class ValidateCredentials extends SWTTestExt {

	@Before
	public void prepareSSHPrefs() {
		// clear dir from libra stuff
		File sshDir = new File(System.getProperty("user.home") + "/.ssh");
		if (sshDir.exists() && sshDir.isDirectory()
				&& sshDir.listFiles().length > 0) {
			for (File file : sshDir.listFiles()) {
				if (file.getName().contains("libra"))
					file.delete();
			}
		}

	}

	@Test
	public void canValidateCredentials() throws InterruptedException {
		// create new OpenShift Express Application
		SWTBot wiz = open.newObject(OpenShiftUI.NewApplication.iNewObject);

		storePasswordThenForward();

		// set wrong user credentials
		wiz.text(0).setText(TestProperties.getProperty("openshift.user.name"));
		wiz.text(1).setText(
				TestProperties.getProperty("openshift.user.wrongpwd"));

		SWTBotButton nextButton = wiz.button(IDELabel.Button.NEXT);
		// try to move forward
		nextButton.click();

		// wait for credentials validation
		bot.waitUntil(new NonSystemJobRunsCondition());

		assertFalse("Next > button shouldn't be enabled to move forward.",
				nextButton.isEnabled());

		// set correct user credentials and save it to secure storage
		wiz.text(0).setText(TestProperties.getProperty("openshift.user.name"));
		wiz.text(1).setText(TestProperties.getProperty("openshift.user.pwd"));
		wiz.checkBox(0).select();

		// move forward
		nextButton.click();

		storePasswordThenForward();

		bot.waitForShell("", 100);
		
		log.info("OpenShift SWTBot Tests: Credentials validated.");
	}

	/*
	 * give the secure storage password (will use the same as user's ssh
	 * passphrase)
	 */
	private void storePasswordThenForward() {
		if (bot.waitForShell(IDELabel.Shell.SECURE_STORAGE) != null) {
			bot.text(0).setText(TestProperties.getPassphrase());
			bot.button(IDELabel.Button.OK).click();
		}
	}

}
