package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.junit.Test;

public class CreateDomain extends SWTTestExt {

    @Test
    public void domainCreate() {

        // create domain only if explicit configured
        if (Boolean.parseBoolean(TestProperties.getProperty("createDomain"))) {

            SWTBotText domainText = bot.textInGroup("Domain", 0);

            bot.waitUntil(Conditions.widgetIsEnabled(domainText));

            assertTrue("Domain should not be set at this stage!", domainText
                    .getText().equals(""));

            domainText.setText(TestProperties.getProperty("openshift.domain"));

            bot.button("Create").click();
        }
    }

}
