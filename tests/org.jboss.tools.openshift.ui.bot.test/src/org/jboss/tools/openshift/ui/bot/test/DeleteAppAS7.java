package org.jboss.tools.openshift.ui.bot.test;

import java.io.File;
import java.io.IOException;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class DeleteAppAS7 extends SWTTestExt {
    @Test
    public void deleteApplication() {
        SWTBot wiz = open.newObject(ActionItem.NewObject.create("OpenShift",
                "OpenShift Express Application"));
        wiz.text(0).setText(TestProperties.getProperty("openshift.user.name"));
        wiz.text(1).setText(TestProperties.getProperty("openshift.user.pwd"));
        wiz.button(IDELabel.Button.NEXT).click();
        
        SWTBotText domainText = bot.textInGroup("Domain", 0);
        bot.waitUntil(Conditions.widgetIsEnabled(domainText));
        
        SWTBotTable appTable = bot.tableInGroup("Available Applications");

        appTable.select(TestProperties.getProperty("openshift.jbossapp.name"));

        bot.buttonInGroup(IDELabel.Button.DELETE, "Available Applications")
                .click();

        bot.waitUntilWidgetAppears(Conditions
                .waitForWidget(WidgetMatcherFactory
                        .withLabel("You're up to delete all data within an application. "
                                + "The data may not be recovered. Are you sure that you want to delete application "+TestProperties.getProperty("openshift.jbossapp.name")+"?")));

        bot.button(IDELabel.Button.YES).click();

        bot.waitUntil(Conditions.widgetIsEnabled(appTable));

        assertFalse(appTable.containsItem(TestProperties
                .getProperty("openshift.jbossapp.name")));

        deleteJenkins();
        
        deleteLocalGitRepo();
    }

    /*
     * Since the JBoss tooling doesn't remove app repos, we need to delete it
     * manually
     */
    private void deleteLocalGitRepo() {

        String userHome = System.getProperty("user.home");
        String defaultGitLocation = userHome + "\\git\\"
                + TestProperties.getProperty("openshift.jbossapp.name");

        log.info("Removing " + defaultGitLocation);

        File gitDir = new File(defaultGitLocation);
        if (gitDir.exists() && gitDir.isDirectory()) {
            try {
                doDelete(gitDir);
            } catch (IOException e) {
                log.error("Exception when trying to delete git repo!", e);
            }
        }

    }

    private void doDelete(File path) throws IOException {
        if (path.isDirectory()) {
            for (File child : path.listFiles()) {
                doDelete(child);
            }
        }
        if (!path.delete()) {
            throw new IOException("Could not delete " + path);
        }
    }

    private void deleteJenkins() {
        SWTBotTable appTable = bot.tableInGroup("Available Applications");

        if (appTable.containsItem("jenkins")) {
            appTable.select("jenkins");

            bot.buttonInGroup(IDELabel.Button.DELETE, "Available Applications")
                    .click();

            bot.waitUntilWidgetAppears(Conditions
                    .waitForWidget(WidgetMatcherFactory
                            .withLabel("You're up to delete all data within an application. "
                                    + "The data may not be recovered. Are you sure that you want to delete application jbossapp?")));

            bot.button(IDELabel.Button.YES).click();

            bot.waitUntil(Conditions.widgetIsEnabled(appTable));

            assertFalse(appTable.containsItem("jenkins"));
        }
    }
}
