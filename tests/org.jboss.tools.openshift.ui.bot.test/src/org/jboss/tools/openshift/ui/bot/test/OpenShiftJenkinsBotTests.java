package org.jboss.tools.openshift.ui.bot.test;

import org.jboss.tools.openshift.ui.bot.test.explorer.Connection;
import org.jboss.tools.openshift.ui.bot.test.explorer.CreateDomain;
import org.jboss.tools.openshift.ui.bot.test.explorer.DeleteDomain;
import org.jboss.tools.openshift.ui.bot.test.explorer.RenameDomain;
import org.jboss.tools.ui.bot.ext.RequirementAwareSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
Connection.class,
CreateDomain.class,
RenameDomain.class,
DeleteDomain.class
})
@RunWith(RequirementAwareSuite.class)
public class OpenShiftJenkinsBotTests {

}
