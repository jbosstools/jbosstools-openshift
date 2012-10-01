package org.jboss.tools.openshift.ui.bot.util;

import java.util.List;
import java.util.Vector;

import org.jboss.tools.ui.bot.ext.gen.INewObject;
import org.jboss.tools.ui.bot.ext.gen.IView;

/**
 * 
 * Wrapper class for OpenShift UI tooling.
 * 
 * @author sbunciak
 * 
 */
public class OpenShiftUI {

	/**
	 * 
	 * Class representing OpenShift Console View
	 *
	 */
	public static class Explorer {

		public static final IView iView = new IView() {
			@Override
			public String getName() {
				return "OpenShift Explorer";
			}

			@Override
			public List<String> getGroupPath() {
				List<String> l = new Vector<String>();
				l.add("JBoss Tools");
				return l;
			}
		};
	}

	/**
	 * 
	 * Class representing "navigation" to new OpenShift Application
	 * 
	 */
	public static class NewApplication {
		public static final INewObject iNewObject = new INewObject() {
			@Override
			public String getName() {
				return "OpenShift Application";
			}

			@Override
			public List<String> getGroupPath() {
				List<String> l = new Vector<String>();
				l.add("OpenShift");
				return l;
			}
		};
	}
	
	/**
	 * List of available application type labels
	 * 
	 * @author sbunciak
	 *
	 */
	public static class AppType {
		
		public static final String JBOSS = "jbossas-7";
		public static final String JENKINS = "jenkins-1.4";
		public static final String PERL = "perl-5.10";
		public static final String PHP = "php-5.3";
		public static final String PYTHON = "python-2.6";
		public static final String RAW = "raw-0.1";
		public static final String RUBY = "ruby-1.8";
	}
	
	/**
	 * List of available cartridge labels
	 * 
	 * @author sbunciak
	 *
	 */
	public static class Cartridge {
		
		public static final String MONGODB = "mongodb-2.0";
		public static final String JENKINS = "jenkins-1.4";
		public static final String CRON = "cron-1.4";
		public static final String MYSQL = "mysql-5.1";
		public static final String POSTGRESQL = "postgresql-8.4";
		public static final String PHPMYADMIN = "phpmyadmin-3.4";
		public static final String METRICS = "metrics-0.1";
		public static final String ROCKMONGO = "rockmongo-1.1";
	}
	
	public static class Labels {
		
		public static final String CONNECT_TO_OPENSHIFT = "Connect to OpenShift";
		public static final String EXPLORER_NEW_APP = "New OpenShift Application...";
		public static final String EXPLORER_CREATE_EDIT_DOMAIN = "Create or Edit Domain...";
		public static final String EXPLORER_DELETE_DOMAIN = "Delete Domain";
		public static final String EXPLORER_CREATE_SERVER = "Create a Server Adapter";
		public static final String EXPLORER_DELETE_APP = "Delete Application(s)";
		public static final String EDIT_CARTRIDGES = "Edit Embedded Cartridges...";
		public static final String REFRESH = "Refresh";
		
	}
	
	public static class Shell {
		
		public static final String NO_TITLE = "";
		public static final String NEW_APP = "New OpenShift Application";
		public static final String CREATE_DOMAIN = "Create Domain";
		public static final String EDIT_DOMAIN = "Edit Domain";
		public static final String CREDENTIALS = "";
		public static final String SSH_WIZARD = "";
		public static final String NEW_SSH = "";
		public static final String DELETE_APP = "Application deletion";
		
	}
}