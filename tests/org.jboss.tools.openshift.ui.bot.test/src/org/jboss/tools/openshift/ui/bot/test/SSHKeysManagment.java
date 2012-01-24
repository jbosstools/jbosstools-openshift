package org.jboss.tools.openshift.ui.bot.test;

import org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.junit.Test;

public class SSHKeysManagment extends SWTTestExt {

    @Test
    public void addPrivateKey() {
        
        bot.waitUntilWidgetAppears(Conditions.waitForWidget(WidgetMatcherFactory
                        .withLabel("SSH Public Key")));

        bot.link(0).click("SSH2 Preferences");
        //TODO: wait
                
        SWTBotText privateKeysText = bot.text(2); 
        
        privateKeysText.setText(privateKeysText.getText() + ",libra_id_rsa");
        
        bot.button("OK").click();
        
        // TODO: repeat untilClosedActiveShell - due to HTTP 500
        
        bot.button("Finish").click();
    }
    
}
