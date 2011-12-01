package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftTestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.config.Annotations.Require;
import org.jboss.tools.ui.bot.ext.gen.ActionItem;
import org.junit.Test;

@Require(clearWorkspace = true)
public class CredentialsValidation extends SWTTestExt {

    @Test
    public void testUserCredentialsValidation() throws InterruptedException {
        // try to create new OpenShift Express Application
        SWTBot wiz = open.newObject(ActionItem.NewObject.create("OpenShift",
                "OpenShift Express Application"));

        SWTBotButton validateButton = wiz.button("Validate");

        assertFalse("Validation button shouldn't be enabled at this step.",
                validateButton.isEnabled());

        // set wrong user credentials
        wiz.text(0).setText(
                OpenShiftTestProperties.getProperty("openshift.user.name"));
        wiz.text(1).setText(
                OpenShiftTestProperties.getProperty("openshift.user.wrongpwd"));

        assertTrue(
                "Validation button should be enabled to check the user credentials.",
                validateButton.isEnabled());

        SWTBotButton nextButton = wiz.button("Next >");
        // try to move forward
        nextButton.click();

        // wait for credentials validation
        bot.waitUntilWidgetAppears(Conditions
                .waitForWidget(WidgetMatcherFactory
                        .withText(" The given credentials are not valid")));

        assertFalse("Next > button shouldn't be enabled to move forward.",
                nextButton.isEnabled());

        // test on changed credentials
        // set correct user credentials
        wiz.text(0).setText(
                OpenShiftTestProperties.getProperty("openshift.user.name"));
        wiz.text(1).setText(
                OpenShiftTestProperties.getProperty("openshift.user.pwd"));

        assertTrue(
                "Validation button should be enabled again, user credentials has been changed.",
                validateButton.isEnabled());

        // validate credentials
        validateButton.click();

        bot.waitUntil(Conditions.widgetIsEnabled(nextButton));

        // buttons assertion
        assertFalse(
                "Validation button shouldn't be enabled until user credentials hasn't been changed.",
                validateButton.isEnabled());
        assertFalse(
                "Finish button shouldn't be enabled at this step, can't finish wizard.",
                wiz.button("Finish").isEnabled());

        // move forward
        nextButton.click();
    }

}
