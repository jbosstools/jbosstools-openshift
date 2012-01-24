package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class ImportRepublishAppASS7 extends SWTTestExt {

    private void finishWizardAndImportApp() {

        bot.tableInGroup("Available Applications").select(
                TestProperties.getProperty("openshift.jbossapp.name"));

        bot.button(IDELabel.Button.NEXT).click();

        SWTBotButton finishButton = bot.button(IDELabel.Button.FINISH);

        bot.waitUntil(Conditions.widgetIsEnabled(finishButton));

        finishButton.click();

        // TODO: wait for new shell
        bot.text(0).setText(TestProperties
                .getProperty("openshift.ssh.passphrase"));
        
        bot.waitUntil(Conditions.shellCloses(bot.activeShell()));

        assertTrue("App is not in the Package Explorer!",
                packageExplorer.existsResource(TestProperties
                        .getProperty("openshift.jbossapp.name")));
    }

    @Test
    public void adapterIsCreated() {
        finishWizardAndImportApp();

        assertNotNull("App runtime not in the Servers View!",
                servers.findServerByName(servers.show().bot().tree(),
                        TestProperties.getProperty("openshift.jbossapp.name")));
    }

    private void modifyApp() {
        SWTBot wiz = open.newObject(ActionItem.NewObject.WebHTMLPage.LABEL);
        wiz.text(1).setText("Test.html");
        wiz.button(IDELabel.Button.FINISH);
    }

    @Test
    public void republishApp() {
        modifyApp();

        servers.findServerByName(servers.show().bot().tree(),
                TestProperties.getProperty("openshift.jbossapp.name"))
                .select("Jon, Doe").contextMenu("Publish").click();

    }

}
