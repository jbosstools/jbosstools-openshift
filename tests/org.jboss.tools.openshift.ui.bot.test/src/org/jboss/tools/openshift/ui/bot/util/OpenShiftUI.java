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
	public static class Console {

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
}
