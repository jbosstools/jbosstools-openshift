# OpenShift tooling integrations tests
This plugin contains OpenShift tooling integrations tests. For execution of a specific tests see guidelines below.

## OpenShift integration tests
Running OpenShift integrations tests requires OpenShift local cluster in CDK (https://developers.redhat.com/products/cdk/overview/). Online instances, like https://www.openshift.com, can be used too, but they are not tested. 
Note: tested with CDK 3.2.0.alpha.

#### Running OS tests from IDE
There are three suites for OpenShift tests (full suite (OpenShift3BotTests), smoke suite (OpenShift3SmokeBotTests)) and stable suite (OpenShift3StableTests). To run OpenShift tests from IDE, perform following steps:
1. Install RedDeer to your IDE from update site: http://download.eclipse.org/reddeer/releases/latest (RedDeer is on DevStudio TP, RedDeer project repo URL: https://github.com/eclipse/reddeer)
2. Select desired suite (e.g. OpenShift3BotTests, OpenShift3SmokeBotTests, OpenShift3StableBotTests) and in its context menu select _Run As_ - _Run Configurations..._
3. In Run Configurations shell double click on RedDeer Test and a new RedDeer test run configuration for your suite is created
4. Select tab Argument and fill in following properties **with credentials** to VM arguments:
`-Dopenshift.server= -Dopenshift.username= -Dopenshift.password= -Dsecurestorage.password= -Dusage_reporting_enabled=false -Dopenshift.authmethod=basic`
for OpenShift tests is used basic authentication method with username/password by default.   
5. Confirm changes and run test suite.

#### Running OS tests from command line
For execution of OpenShift tests from command line you need to have installed maven. At first you need to build jbosstools-openshift repo with maven and disabled tests, e.g. `mvn clean install -DskipTests=true -DskipITests=true`. Then run in _org.jboss.tools.openshift.ui.bot.test_ plugin following command **with filled credentials and OpenShift server** with basic authentication. By default it is used OpenShift3StableBotTests tests suite. User can switch to different suite class by using profiles: `-Psmoke` (OpenShift3SmokeBotTests) or `-Pfull` (OpenShift3BotTests).
`mvn clean verify -PITests -Dopenshift.username= -Dopenshift.password= -Dsecurestorage.password= -Dopenshift.server=`

Or you can run ITests from jbosstools-openshift root folder with profile ITests, e.g. `mvn clean verify -PITests -Dopenshift.username= -Dopenshift.password= -Dsecurestorage.password= -Dopenshift.server= `.

You can also run a single test class. To utilize all advantages of RedDeer suite, be sure your test class is annotated with RunWith(RedDeerSuite.class) annotation. If you want to run a single test class instead of a test suite, replace argument -Dscope by argument -Dtest. 