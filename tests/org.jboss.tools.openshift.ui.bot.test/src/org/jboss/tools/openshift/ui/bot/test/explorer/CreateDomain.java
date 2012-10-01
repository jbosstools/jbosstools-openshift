package org.jboss.tools.openshift.ui.bot.test.explorer;

import java.io.File;
import java.util.Date;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Before;
import org.junit.Test;

/**
 * Domain creation consists of creating the SSH key pair, storing user password
 * in the secure storage and creating the domain itself.
 * 
 * @author sbunciak
 * 
 */
public class CreateDomain extends SWTTestExt {

	private boolean keyAvailable = false;

	@Before
	public void prepareSSHPrefs() {
		// clear dir from libra stuff so wizard can create new
		File sshDir = new File(System.getProperty("user.home") + "/.ssh");

		if (sshDir.exists() && sshDir.isDirectory()
				&& sshDir.listFiles().length > 0) {
			for (File file : sshDir.listFiles()) {
				if (file.getName().contains("id_rsa"))
					//keyAvailable = true;
					file.delete();
				if (file.getName().contains("known_hosts"))
					file.delete();
			}
		}

	}

	@Test
	public void canCreateDomain() throws InterruptedException {
		// open OpenShift Explorer
		SWTBotView openshiftExplorer = open
				.viewOpen(OpenShiftUI.Explorer.iView);

		openshiftExplorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_CREATE_EDIT_DOMAIN)
				.click();

		bot.waitForShell(OpenShiftUI.Shell.CREATE_DOMAIN);

		SWTBotText domainText = bot.text(0);

		assertTrue("Domain should not be set at this stage!", domainText
				.getText().equals(""));

		domainText.setText(TestProperties.get("openshift.domain"));
		log.info("*** OpenShift SWTBot Tests: Domain name set. ***");

		SWTBotButton finishBtn = bot.button(IDELabel.Button.FINISH);
		
		
		bot.link(0).click();
		
		if (keyAvailable) {
// TODO: add them to the list 
			bot.waitForShell(OpenShiftUI.Shell.SSH_WIZARD);
			assertTrue("SSH key should be set!", bot.table().columnCount() > 0);

		} else {
			
			bot.waitForShell(OpenShiftUI.Shell.SSH_WIZARD);
			bot.buttonInGroup("New...", "SSH Public Keys").click();
			bot.waitForShell(OpenShiftUI.Shell.NEW_SSH);
			
			bot.textInGroup("New SSH Key", 0).setText("jbtkey" + new Date());
			bot.textInGroup("New SSH Key", 2).setText("id_rsa");
			bot.button(IDELabel.Button.FINISH).click();
			
			bot.waitWhile(new NonSystemJobRunsCondition(), TIME_20S, TIME_1S); 
			bot.waitForShell(OpenShiftUI.Shell.SSH_WIZARD);
			
			bot.button(IDELabel.Button.OK).click();
			bot.waitForShell(OpenShiftUI.Shell.CREATE_DOMAIN);

			log.info("*** OpenShift SWTBot Tests: SSH Keys created. ***");
		}

		bot.waitUntil(Conditions.widgetIsEnabled(finishBtn));
		finishBtn.click();

		// wait while the domain is being created
		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_60S);

		log.info("*** OpenShift SWTBot Tests: Domain created. ***");
	}

}
