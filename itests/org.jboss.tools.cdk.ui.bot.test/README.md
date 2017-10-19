# CDK tooling integration tests
This plugin contains CDK tooling integration tests. For execution of a specific tests see guidelines below.

## Requirements
To be able to run CDK integration tests, couple of pre-requisities must be fulfilled.
1. You need to have an account on https://developers.redhat.com
2. Have downloaded and configured Container Development Kit (2.x and 3.x). Instructions to download/setup CDK 2.x/3.x can be found here: https://developers.redhat.com/products/cdk/overview/. Even though, there is always supported latest CDK version (3.2 version), integration tests for CDK tooling are still testing proper UI for CDK 2.x and CDK 3.1 adapters. Please, follow the requirements below, you need to have:
- downloaded CDK 2.x vagrant file
- downloaded CDK 3.1 binary
- downloaded and configured CDK 3.2 binary, and have called setup-cdk on it (which creates ~/.minishift folder, and prepares cdk to be able to start from IDE)
3. For successful registration of vm-guest rhel image your account must have signed Terms and Conditions (https://developers.redhat.com/terms-and-conditions/).
4. For ease of configuration and automation there is a possibility to use cdk configuration scripts (https://github.com/odockal/cdk-scripts), please, see below...

## Running CDK integration tests
Integration tests are verifying UI objects of CDK 2.x, CDK 3.1 and CDK 3.2+. By default, testing of server adapter happens only with latest supported version of CDK 3.2+. This includes start/stop/restart operations. This is because of any of CDK versions cannot be run together, they are mutually exclusive.

#### Using IDE
There are three suites for CDK integration tests (full suite (CDKAllTestsSuite), smoke suite (CDKSmokeTestsSuite)), CDK 3.1 only test suite (CDK3AllTestsSuite) and CDK 3.2+ tests only (CDK32AllTestsSuite). To run CDK integration tests from IDE, perform following steps:
1. Install RedDeer to your IDE from update site: http://download.eclipse.org/reddeer/releases/latest (RedDeer is on DevStudio TP, RedDeer project repo URL: https://github.com/eclipse/reddeer)
2. Select desired java suite (e.g. CDKAllTestsSuite, CDKSmokeTestsSuite, CDK3AllTestsSuite, CDK32AllTestsSuite) and in its context menu select _Run As_ - _Run Configurations..._
3. In Run Configurations dialog double click on RedDeer Test and a new RedDeer test run configuration for your suite will be created
4. Select tab Argument and fill in following properties **with credentials** to VM arguments:
```
-Dminishift.hypervisor=kvm|virtualbox|hyperv|xhyve
-Dminishift=/path/to/your/cdk-3.1/minishift
-Dminishift.profile=/path/to/your/cdk-3.2/minishift 
-Ddevelopers.username=yourusername 
-Ddevelopers.password=password
-Dvagrantfile=/path/to/vagrantfile
-Dusage_reporting_enabled=false 
``` 
5. All above Mentioned arguments must be used when CDKSmokeTestsSuite or CDKAllTestsSuitesuite suite is used, if you do not want to test CDK 2.x, you can choose to run CDK3AllTestsSuite or CDK32AllTestsSuite and then you can omit <br />
`-Dvagrantfile=/path/to/your/vagrantfile/folder/`

**NOTE:** In case that you run tests for, ie. CDK 3.2+ (CDKAllTestsSuite) and now you want to test CDK 3.1 (CDK3AllTestsSuite), you need to clean up old CDK (3.2+) configuration. To clean up old configuration, follow https://docs.openshift.org/latest/minishift/getting-started/uninstalling.html.

6. Confirm changes and run test suite. Please, note that this configuration also counts for running tests Using maven option

#### Using maven
For execution of CDK integration tests from command line you will need maven. At first, you need to build openshift plugins with maven (`mvn clean install`). Secondly, build _org.jboss.tools.cdk.reddeer_ plugin and _org.jboss.tools.openshift.reddeer_. Alternatively, you can build both reddeer plugins by building pom.xml in test-framework folder. Then run in _org.jboss.tools.cdk.ui.bot.test_ plugin following command **with filled credentials** with basic authentication. Default test suite is CDKAllTestsSuite (one do not need to use -Pall-tests). User can switch to different suite class by using profiles:
* `-Psmoke-tests` (CDKSmokeTestsSuite)
* `-Pall-tests` (CDKAllTestsSuite)
* `-Pcdk3-all-tests` (CDK3AllTestsSuite)
* `-Pcdk32-all-tests` (CDK32AllTestsSuite)

**NOTE:** Integration tests requires to use `-PITests` profile, so other settings can be applied.

To run tests, type this command:
```
mvn clean verify -Dtest.installBase=/path/to/ide/to/run/against -PITests
-Dminishift.hypervisor=kvm|virtualbox|hyperv|xhyve
-Dminishift=/path/to/your/cdk-3.1/minishift
-Dminishift.profile=/path/to/your/cdk-3.2/minishift 
-Dvagrantfile=/path/to/your/vagrantfile
-Ddevelopers.username=yourusername
-Ddevelopers.password=password
-DskipTests=false -Dusage_reporting_enabled=false

```

You can also run a single test class. To utilize all advantages of RedDeer suite, be sure your test class is annotated with RunWith(RedDeerSuite.class) annotation. If you want to run a single test class instead of a test suite, use parameter <br />`-Dtest`, ie.
`-Dtest=org.jboss.tools.ui.bot.test.server.adapter.CDK3ServerWizardTest` 
or in IDE, write down required class in _Test class_ row of the form that can be found in _Run Configurations_ under _Test_ tab.

#### Using cdk-scripts to download CDK binaries
In case that you want to run test suites that run tests for all CDK versions, you can ease the process of downloading and configuring CDK-3.1.1 and CDK-3.2. Base scenario:
1. git clone https://github.com/odockal/cdk-scripts
2. cd cdk-scripts/scripts
3. ./cdk3-install.sh -u http://download.server.cdk.org/cdk-3.1.1/minishift -p /home/user/cdk-3.1.1
4. ./cdk3-install.sh -u http://download.server.cdk.org/cdk-3.2/minishift -p /home/user/cdk-3.2.0 --setup "--default-vm-driver kvm"

and one is done with setting up CDK 3.x+ versions for integration tests. You still need to downlaod vagrantfile from CDK 2.x by yourself
