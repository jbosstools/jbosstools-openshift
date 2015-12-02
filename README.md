# The OpenShift Tools project

## Summary

OpenShift Tools provides wizards and views for creating and maintaining OpenShift applications.

## Install

_OpenShift Tools_ is part of [JBoss Tools](http://jboss.org/tools) from
which it can be [downloaded and installed](http://jboss.org/tools/download)
on its own or together with the full JBoss Tools distribution.

## Get the code

The easiest way to get started with the code is to [create your own fork](http://help.github.com/forking/),
and then clone your fork:

    $ git clone git@github.com:<you>/jbosstools-openshift.git
    $ cd jbosstools-openshift
    $ git remote add upstream git://github.com/jbosstools/jbosstools-openshift.git

At any time, you can pull changes from the upstream and merge them onto your master:

    $ git checkout master               # switches to the 'master' branch
    $ git pull upstream master          # fetches all 'upstream' changes and merges 'upstream/master' onto your 'master' branch
    $ git push origin                   # pushes all the updates to your fork, which should be in-sync with 'upstream'

The general idea is to keep your 'master' branch in-sync with the
'upstream/master'.

## Building OpenShift Tools

To build _OpenShift Tools_ requires specific versions of Java (1.6+) and
+Maven (3.1+). See this [link](https://github.com/jbosstools/jbosstools-devdoc/blob/master/building/readme.md) for more information on how to setup, run and configure build.

This command will run the build:

    $ mvn clean verify

If you just want to check if things compiles/builds you can run:

    $ mvn clean verify -DskipTest=true

But *do not* push changes without having the new and existing unit tests pass!

## Working with [openshift-restclient-java](https://github.com/openshift/openshift-restclient-java/)

When making changes to the [openshift-restclient-java](https://github.com/openshift/openshift-restclient-java/), it needs to be built and its jar copied to `org.jboss.tools.openshift.client/lib` in order to be consumed (workspace resolution doesn't work for projects embedded as jars within Eclipse plugins). The best way to achieve a quick turnaround it to have `openshift-restclient-java` opened as an Eclipse project and leverage its `jar.outputDir` property, so that Maven can generate the rest client jar
directly in the org.jboss.tools.openshift.client/lib folder, followed by a directory refresh.

- Right-click on the `openshift-rest-client-java` project
- Run As > Maven Build
- set goals=package
- add `jar.outpuDir` property, set the absolute path to org.jboss.tools.openshift.client/lib as value
- In the refresh tab, enable refresh resources upon completion
- click specify resources..., select org.jboss.tools.openshift.client/lib
- Run, profit!

Subsequent Maven runs will reuse this launch configuration automatically.

## Contribute fixes and features

_OpenShift Tools_ is open source, and we welcome anybody that wants to
participate and contribute!

If you want to fix a bug or make any changes, please log an issue in
the [JBoss Tools JIRA](https://issues.jboss.org/browse/JBIDE)
describing the bug or new feature and give it a component type of
`openshift`. Then we highly recommend making the changes on a
topic branch named with the JIRA issue number. For example, this
command creates a branch for the JBIDE-1234 issue:

	$ git checkout -b jbide-1234

After you're happy with your changes and a full build (with unit
tests) runs successfully, commit your changes on your topic branch
(with good comments). Then it's time to check for any recent changes
that were made in the official repository:

	$ git checkout master               # switches to the 'master' branch
	$ git pull upstream master          # fetches all 'upstream' changes and merges 'upstream/master' onto your 'master' branch
	$ git checkout jbide-1234           # switches to your topic branch
	$ git rebase master                 # reapplies your changes on top of the latest in master
	                                      (i.e., the latest from master will be the new base for your changes)

If the pull grabbed a lot of changes, you should rerun your build with
tests enabled to make sure your changes are still good.

You can then push your topic branch and its changes into your public fork repository:

	$ git push origin jbide-1234         # pushes your topic branch into your public fork of OpenShift Tools

And then [generate a pull-request](http://help.github.com/pull-requests/) where we can
review the proposed changes, comment on them, discuss them with you,
and if everything is good merge the changes right into the official
repository.
