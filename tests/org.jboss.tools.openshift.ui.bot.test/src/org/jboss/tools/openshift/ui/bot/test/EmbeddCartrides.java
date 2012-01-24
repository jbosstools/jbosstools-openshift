package org.jboss.tools.openshift.ui.bot.test;

import java.util.StringTokenizer;

import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Test;

public class EmbeddCartrides extends SWTTestExt {

    @Test
    public void embeddCartriges() {

        //TODO: be sure to have app selected
        
        bot.buttonInGroup("Edit", "Available Applications").click();

        // TODO: Doesn't work
        bot.waitUntilWidgetAppears(Conditions
                .waitForWidget(WidgetMatcherFactory
                        .withText("Embeddable Cartridges")));

        StringTokenizer tokenizer = new StringTokenizer(
                TestProperties.getProperty("openshift.jbossapp.cartridges"),
                ";");

        SWTBotTable cartridgeTable = bot.tableInGroup("Embeddable Cartridges",
                0);

        while (tokenizer.hasMoreTokens()) {
            String cartridge = tokenizer.nextToken();

            cartridgeTable.getTableItem(cartridge).click();

            if (cartridge.contains("jenkins")) {
                bot.text(0).setText("jenkins");
                bot.button(IDELabel.Button.OK).click();
                bot.waitUntilWidgetAppears(Conditions
                        .waitForWidget(WidgetMatcherFactory
                                .withLabel(IDELabel.Button.OK)));
                bot.button(IDELabel.Button.OK).click();
            }

            bot.button(IDELabel.Button.FINISH).click();

            bot.waitUntilWidgetAppears(Conditions
                    .waitForWidget(WidgetMatcherFactory
                            .withLabel(IDELabel.Button.OK)));
            bot.button(IDELabel.Button.OK).click();
        }

    }

}
