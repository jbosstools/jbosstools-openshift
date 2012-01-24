package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class CreateAppAS7 extends SWTTestExt {

    @Test
    public void createApplication() {
        bot.button(IDELabel.Menu.NEW).click();

        SWTBotText appNameText = bot.text(0);

        assertTrue("App name should be empty!", appNameText.getText()
                .equals(""));

        appNameText.setText(TestProperties
                .getProperty("openshift.jbossapp.name"));

        bot.comboBox(0).setSelection("jbossas-7.0");

        bot.button(IDELabel.Button.FINISH).click();

        bot.waitUntil(Conditions.shellCloses(bot.activeShell()));

        assertTrue(bot.tableInGroup("Available Applications").containsItem(
                TestProperties.getProperty("openshift.jbossapp.name")));
    }

}
