# CDK tooling integration tests
This plugin contains CDK tooling integration tests. For execution of a specific tests see guidelines below.

## Requirements
To be able to run CDK integration tests, couple of pre-requisities must be fulfilled.
1. You need to have an account on https://developers.redhat.com
2. Have downloaded and configured Container Development Kit (2.x and 3.x). Instructions to download/setup CDK 2.x/3.x can be found here: https://developers.redhat.com/products/cdk/overview/. Even though, there is always supported latest CDK version (3.2 version), integration tests for CDK tooling are still testing proper UI for CDK 2.x and CDK 3.1 adapters. Please, follow the requirements below, you need to have:
- downloaded CDK 2.x vagrant file
- downloaded and configured CDK 3.1 binary, `minishift setup-cdk` must be called before test is run in IDE
- TODO: downloaded and configured CDK 3.2 binary, and have called setup-cdk on it (which creates ~/.minishift folder, and prepares cdk to be able to start from IDE)
3. For successful registration of vm-guest rhel image your account must have signed Terms and Conditions (https://developers.redhat.com/terms-and-conditions/).
4. For ease of configuration and automation there is a possibility to use cdk configuration scripts (https://github.com/odockal/cdk-scripts)

## Running CDK integration tests

#### Using IDE
There are three suites for CDK integration tests (full suite (CDKAllTestsSuite), smoke suite (CDKSmokeTestsSuite)) and CDK 3.x only test suite (CDK3AllTestsSuite). To run CDK integration tests from IDE, perform following steps:
1. Install RedDeer to your IDE from update site: http://download.eclipse.org/reddeer/releases/latest (RedDeer is on DevStudio TP, RedDeer project repo URL: https://github.com/eclipse/reddeer)
2. Select desired java suite (e.g. CDKAllTestsSuite, CDKSmokeTestsSuite, CDK3AllTestsSuite) and in its context menu select _Run As_ - _Run Configurations..._
3. In Run Configurations dialog double click on RedDeer Test and a new RedDeer test run configuration for your suite will be created
4. Select tab Argument and fill in following properties **with credentials** to VM arguments:
```
-Dminishift.hypervisor=kvm|virtualbox|hyperv|xhyve 
-Dminishift.path=/path/to/your/minishift/binary 
-Ddevelopers.username=yourusername 
-Ddevelopers.password=password 
-Dusage_reporting_enabled=false 
``` 
5. If you do not want to test CDK 2.x, you can choose to run CDK3AllTestsSuite and then you can omit <br />
`-Dvagrantfile.path=/path/to/your/vagrantfile/folder/`

6. Confirm changes and run test suite.

#### Using maven
For execution of CDK integration tests from command line you will need maven. At first, you need to build openshift plugins with maven (`mvn clean install`). Secondly, build _org.jboss.tools.cdk.reddeer_ plugin and _org.jboss.tools.openshift.reddeer_. Alternatively, you can build both reddeer plugins by building pom.xml in test-framework folder. Then run in _org.jboss.tools.cdk.ui.bot.test_ plugin following command **with filled credentials** with basic authentication. Default test suite is CDKAllTestsSuite. User can switch to different suite class by using profiles:
* `-Psmoke-tests` (CDKSmokeTestsSuite) 
* `-Pall-tests` (CDKAllTestsSuite)
* `-Pcdk3-all-tests` (CDK3AllTestsSuite)

To run tests, type this command:
```
mvn clean verify -Dtest.installBase=/path/to/ide/to/run/against
-Dminishift.hypervisor=kvm|virtualbox|hyperv|xhyve
-Dminishift.path=/path/to/your/minishift/binary
-Ddevelopers.username=yourusername
-Ddevelopers.password=password
-DskipTests=false -Dusage_reporting_enabled=false
-Dvagrantfile.path=/path/to/your/vagrantfile
```

You can also run a single test class. To utilize all advantages of RedDeer suite, be sure your test class is annotated with RunWith(RedDeerSuite.class) annotation. If you want to run a single test class instead of a test suite, use parameter <br />`-Dtest`, ie.
`-Dtest=org.jboss.tools.ui.bot.test.server.adapter.CDK3ServerWizardTest` 
or in IDE, write down required class in _Test class_ row of the form that can be found in _Run Configurations_ under _Test_ tab.
