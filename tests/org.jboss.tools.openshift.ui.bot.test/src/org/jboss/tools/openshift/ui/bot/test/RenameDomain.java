package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.SWTBotTestCase;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.junit.Test;

public class RenameDomain extends SWTBotTestCase {

    @Test
    public void domainRename() {

        SWTBotText domainText = bot.textInGroup("Domain", 0);

        bot.waitUntil(Conditions.widgetIsEnabled(domainText));

        assertTrue(
                "Domain should be set correctly at this stage!",
                domainText.getText().equals(
                        TestProperties.getProperty("openshift.domain")));

        domainText.setText(TestProperties.getProperty("openshift.domain.new"));

        bot.button("Rename").click();

        bot.waitUntil(Conditions.widgetIsEnabled(domainText));
    }

}
