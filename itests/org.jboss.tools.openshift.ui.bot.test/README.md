# OpenShift tooling integrations tests
This plugin contains OpenShift 3 tooling integrations tests. For execution of a specific tests see guidelines below.

## OpenShift 3 integration tests
Running OpenShift 3 integrations tests requires OpenShift 3 instance.

#### Running OS tests from IDE
There are three suites for OpenShift 3 tests (full suite (OpenShift3BotTests), smoke suite (OpenShift3SmokeBotTests)) and stable suite (OpenShift3StableTests). To run OpenShift tests from IDE, perform following steps:
1. Install RedDeer to your IDE (RedDeer is on TP or use one of [update sites](http://jboss-reddeer.github.io/reddeer/#installation)
2. Select desired suite and in its context menu select _Run As_ - _Run Configurations..._
3. In Run Configurations shell double click on RedDeer Test and a new RedDeer test run configuration for your suite is created
4. Select tab Argument and fill in following properties **with credentials** to VM arguments:
`-Dopenshift.server= -Dopenshift.username= -Dopenshift.password= -Dsecurestorage.password= -Dusage_reporting_enabled=false`
for OpenShift 3 tests add also property `-Dopenshift.authmethod=basic`.   
5. Confirm changes and run test suite.

#### Running OS tests from command line
For execution of OpenShift tests from command line you will need maven. At first build _org.jboss.tools.openshift.reddeer_ plugin with maven. Then run in _org.jboss.tools.openshift.ui.bot.test_ plugin following command **with filled credentials and test scope** (in case of running with basic authentication. For OAuth authentication see step 4. in previous section). Test scope can be one of following values and represents test suite to run: 2|2Smoke|3|3Smoke.
`mvn clean verify -Dtest.installBase=/path/to/ide/to/run/against -Dopenshift.username= -Dopenshift.password= -Dsecurestorage.password= Dopenshift.authmethod=basic -Dopenshift.server= -Dscope= -DskipTests=false`

You can also run a single test class. To utilize all advantages of RedDeer suite, be sure your test class is annotated with RunWith(RedDeerSuite.class) annotation. If you want to run a single test class instead of a test suite, replace argument -Dscope by argument -Dtest. 