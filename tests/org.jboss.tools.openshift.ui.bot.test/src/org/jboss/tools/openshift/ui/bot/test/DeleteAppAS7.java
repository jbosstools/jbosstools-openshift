package org.jboss.tools.openshift.ui.bot.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class DeleteAppAS7 extends SWTTestExt {
	@Test
	public void canDeleteApplication() {
		projectExplorer.show();
		projectExplorer.bot().tree(0).contextMenu("Delete").click();

		bot.waitForShell("Delete Resources");
		bot.checkBox().select();
		bot.button(IDELabel.Button.OK).click();

		assertFalse("The project still exists!",
				projectExplorer.existsResource(TestProperties
						.getProperty("openshift.jbossapp.name")));

		SWTBotView openshiftConsole = open.viewOpen(OpenShiftUI.Console.iView);

		SWTBot consoleBot = openshiftConsole.bot();

		SWTBotTreeItem account = consoleBot.tree()
				.expandNode(TestProperties.getProperty("openshift.user.name"))
				.doubleClick();

		account.getNode(0).contextMenu("Delete Application").click();

		bot.waitForShell("Application deletion");

		bot.button(IDELabel.Button.OK).click();
// TODO !!!
		bot.waitWhile(new ICondition() {

			private boolean deletionInvoked = false;

			@Override
			public boolean test() throws Exception {

				if (deletionInvoked && getJobs().size() == 0) {
					return false;
				} else {
					return true;
				}

			}

			@Override
			public void init(SWTBot bot) {
				// Keep empty
			}

			@Override
			public String getFailureMessage() {
				return "Deletion was not invoked in timeout.";
			}

			private List<Job> getJobs() {
				List<Job> jobs = new ArrayList<Job>();
				for (Job job : Job.getJobManager().find(null)) {
					if (Job.SLEEPING != job.getState()) {
						jobs.add(job);

						System.out.println("Job: " + job.getName());

						if (job.getName().contains("OpenShift")) {
							System.out
									.println("!!!!!!!!!!!FOUND ONE!!!!!!!!!!!!!");
							deletionInvoked = true;
						}

					}
				}
				return jobs;
			}
		}, TIME_60S, TIME_1S);

		/*
		 * TODO
		 * 
		 * // delete jenkins if present if (account.getNode("jenkins") != null)
		 * { account.getNode("jenkins").contextMenu("Delete Application");
		 * 
		 * bot.waitForShell("Application deletion");
		 * bot.button(IDELabel.Button.OK).click();
		 * 
		 * bot.waitUntil(new NonSystemJobRunsCondition()); }
		 */
		assertTrue("Application still present in the OpenShift Console view!",
				account.getItems().length == 0);

	}
}
