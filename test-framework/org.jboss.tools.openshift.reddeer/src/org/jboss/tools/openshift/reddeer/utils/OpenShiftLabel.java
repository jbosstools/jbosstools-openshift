/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

/**
 * Various labels for OpenShift tools plugin.
 * 
 * @author mlabuda@redhat.com
 *
 */
public class OpenShiftLabel {

	/**
	 * Cartridges for application creation. Basic and downloadable.
	 */
	public static class Cartridge {
		// Basic cartridges
		public static final String DIY = "Do-It-Yourself 0.1";
		public static final String JENKINS = "Jenkins Server";
		public static final String JBOSS_AS = "JBoss Application Server 7";
		public static final String JBOSS_EAP = "JBoss Enterprise Application Platform 6";
		public static final String JBOSS_EWS = "Tomcat 7 (JBoss EWS 2.0)";
		public static final String PERL = "Perl 5.10";
		public static final String PHP = "PHP 5.3";
		public static final String PYTHON = "Python 2.7";
		public static final String RUBY_1_9 = "Ruby 1.9";
		
		// Quickstarts
		public static final String DJANGO = "Django (Quickstart)";
		
		// Downloadable cartridge
		public static final String DOWNLOADABLE_CARTRIDGE = "Code Anything";
	}

	/**
	 * Embeddable cartridges.
	 */
	public static class EmbeddableCartridge {
		public static final String CRON = "Cron 1.4 (cron-1.4)";
		public static final String JENKINS = "Jenkins Client (jenkins-client-1)";
		public static final String MY_SQL = "MySQL 5.5 (mysql-5.5)";
		public static final String MONGO_DB = "MongoDB 2.4 (mongodb-2.4)";
		public static final String PHP_MYADMIN = "phpMyAdmin 4.0 (phpmyadmin-4)";
		public static final String POSTGRE_SQL = "PostgreSQL 9.2 (postgresql-9.2)";
		public static final String ROCK_MONGO = "RockMongo 1.1 (rockmongo-1.1)";
		
		public static final String DOWNLOADABLE_CARTRIDGE = "Code Anything (Downloadable Cartridge)";
	}
	
	/**
	 * OpenShift explorer context menu labels.
	 */
	public static class ContextMenu {
		// General
		public static final String DELETE = "Delete";
		public static final String EDIT = "Edit...";
		public static final String PROPERTIES = "Properties";
		public static final String REFRESH = "Refresh";
		public static final String[] SHOW_IN_WEB_CONSOLE = {"Show In", "Web Console"};
		public static final String[] SHOW_IN_WEB_BROWSER = {"Show In", "Web Browser"};
		
		// Connection related
		public static final String EDIT_CONNECTION = "Edit Connection...";
		public static final String MANAGE_SSH_KEYS = "Manage SSH Keys...";
		public static final String MANAGE_DOMAINS = "Manage Domains...";
		public static final String[] NEW_DOMAIN = {"New", "Domain..."};
		public static final String[] NEW_CONNECTION = {"New", "Connection..."};
		public static final String[] NEW_OS_PROJECT = {"New", "Project..."};
		public static final String REMOVE_CONNECTION = "Remove Connection(s)...";
		public static final String DELETE_CONNECTION = "Delete";
		
		// Domain related
		public static final String DELETE_DOMAIN = "Delete";
		public static final String EDIT_DOMAIN = "Edit Domain...";
		
		// Project related
		public static final String MANAGE_OS_PROJECTS = "Manage Projects...";
		public static final String DELETE_OS_PROJECT = "Delete";
		public static final String DEPLOY_DOCKER_IMAGE = "Deploy Docker Image...";
		public static final String DEPLOY_TO_OPENSHIFT = "Deploy to OpenShift...";
		
		// Resource related
		public static final String BUILD_LOG = "Build Log...";
		public static final String CLONE_BUILD = "Clone Build";
		public static final String DELETE_RESOURCE = "Delete";
		public static final String POD_LOG = "Pod Log...";
		public static final String[] NEW_RESOURCE = {"New", "Resource..."};
		public static final String START_BUILD = "Start Build";
		public static final String[] SCALE_UP = {"Scale", "Up"};
		public static final String[] SCALE_DOWN = {"Scale", "Down"};
		public static final String[] SCALE_TO = {"Scale", "To..."};
		
		// Application related
		public static final String APPLICATION_DETAILS = "Details...";
		public static final String APPLICATION_PROPERTIES = "Properties";
		public static final String DELETE_APPLICATION = "Delete";
		public static final String DELETE_APPLICATION_VIA_ADAPTER = "Delete Application...";
		public static final String EDIT_ENV_VARS = "Edit User Environment Variables...";
		public static final String EMBED_CARTRIDGE = "Edit Embedded Cartridges...";
		public static final String IMPORT_APPLICATION = "Import Application...";
		public static final String[] NEW_OS2_APPLICATION = {"New", "Application..."};
		public static final String[] NEW_OS3_APPLICATION = {"New", "Application..."};
		public static final String[] NEW_SERVER = {"New", "Server"};	
		public static final String[] NEW_SERVER_ADAPTER = {"New", "Server Adapter..."};
		public static final String NEW_ADAPTER_FROM_EXPLORER = "Server Adapter...";
		public static final String PORT_FORWARD = "Port Forwarding...";		
		public static final String RESTART_APPLICATION = "Restart Application";	
		public static final String[] RESTORE_SNAPSHOT = {"Snapshot", "Restore/Deploy..."};
		public static final String[] SAVE_SNAPSHOT = {"Snapshot", "Save..."}; 
		public static final String SHOW_ENV_VARS = "List All Environment Variables";
		public static final String[] SHOW_IN_BROWSER = {"Show In", "Web Browser"};
		public static final String TAIL_FILES = "Tail Files...";
		public static final String[] DEPLOY_PROJECT = {"Configure", "Deploy to OpenShift"};
		
		// Server adapter related
		public static final String PUBLISH = "Publish";
		
		// Workspace project related
		public static final String[] CONFIGURE_MARKERS = {"OpenShift", "Configure Markers..."};
		public static final String[] GIT_ADD = {"Team", "Add to Index"};
		public static final String[] GIT_COMMIT = {"Team", "Commit..."};
	}
		
	/**
	 * Shell title labels.
	 */
	public static class Shell {
		public static final String ACCEPT_HOST_KEY = "Question";
		public static final String ADAPTER = "New Server";
		public static final String ADD_CARTRIDGES = "Add Embedded Cartridges";
		public static final String ADD_CARTRIDGE_DIALOG = "Add Cartridges";
		public static final String APPLICATION_DETAILS = "Application Details";
		public static final String APPLICATION_PORT_FORWARDING = "Application Port Forwarding";
		public static final String APPLICATION_SERVER_REMOVE = "Application and Server removal";
		public static final String COMMIT = "Commit Changes";
		public static final String DELETE_APP = "Application removal";
		public static final String DELETE_OS_PROJECT = "Delete OpenShift Project";
		public static final String DELETE_ADAPTER = "Delete Server";
		public static final String DEPLOY_IMAGE_TO_OPENSHIFT = "Deploy Image to OpenShift";
		public static final String EDIT_CARTRIDGES = "Edit Embedded Cartridges";
		public static final String EDIT_ENV_VAR = "Edit Environment variable";
		public static final String EDIT_CONNECTION = "Edit OpenShift Connection";
		public static final String EMBEDDED_CARTRIDGE = "Embedded Cartridges";
		public static final String ENVIRONMENT_VARIABLE = "Environment Variable";
		public static final String ENV_VARS = "Create Environment Variable(s)";
		public static final String IMPORT = "Import";
		public static final String IMPORT_APPLICATION_WIZARD = "Import OpenShift Application ";
		public static final String IMPORT_APPLICATION = "Import OpenShift Application";
		public static final String MANAGE_ENV_VARS = "Manage Application Environment Variable(s) for application ";
		public static final String MARKERS = "Configure OpenShift Markers for project ";
		public static final String NEW_APP_WIZARD = "New OpenShift Application";
		public static final String NEW_CONNECTION = "New OpenShift Connection";
		public static final String NEW_RESOURCE = "New OpenShift resource";
		public static final String PORTS_FORWARDING = "Application port forwarding";
		public static final String SAVE_SNAPSHOT = "Save Snapshot";
		public static final String SCALE_DEPLOYMENTS = "Scale Deployments";
		public static final String SELECT_EXISTING_APPLICATION = "Select Existing Application";
		public static final String SELECT_EXISTING_PROJECT = "Select Existing Project";
		public static final String SELECT_OPENSHIFT_TEMPLATE = "Select an OpenShift template";
		public static final String PREFERENCES = "Preferences";
		public static final String REMOVE_CONNECTION = "Remove connection";
		public static final String REMOVE_ENV_VAR = "Remove Environment Variable";
		public static final String REMOVE_PORT = "Remove port";
		public static final String REFRESH_ENV_VARS = "Refresh Environment Variables";
		public static final String RESET_ENV_VAR = "Reset Environment Variable";
		public static final String RESET_PORTS = "Reset ports";
		public static final String RESTART_APPLICATION = "Restart Application";
		public static final String RESTORE_SNAPSHOT = "Restore/Deploy Snapshot";
		public static final String SECURE_STORAGE = "Secure Storage";
		public static final String SECURE_STORAGE_PASSWORD = "Secure Storage Password";
		public static final String PASSWORD_HINT_NEEDED = "Secure Storage - Password Hint Needed";
		public static final String STOP_ALL_DEPLOYMENTS = "Stop all deployments?";
		public static final String TAIL_FILES = "Tail Files";
		public static final String UNTRUSTED_SSL_CERTIFICATE = "Untrusted SSL Certificate";
		
		// Domain related
		public static final String CREATE_DOMAIN = "Create Domain";
		public static final String DELETE_DOMAIN = "Domain deletion";
		public static final String EDIT_DOMAIN = "Edit domain";
		public static final String MANAGE_DOMAINS = "Domains";
		
		// SSH Key related
		public static final String ADD_SSH_KEY = "Add SSH Key";
		public static final String MANAGE_SSH_KEYS = "Manage SSH Keys";
		public static final String NEW_SSH_KEY = "New SSH Key";
		public static final String NO_SSH_KEY = "No SSH Keys";
		public static final String REMOVE_SSH_KEY = "Remove SSH Key";
		
		// Project related
		public static final String MANAGE_OS_PROJECTS = "OpenShift Projects";
		public static final String CREATE_OS_PROJECT = "Create OpenShift Project";
		
		// Server adapter related
		public static final String PUBLISH_CHANGES = "Publish Changes";
		
		// Application related
		public static final String APPLICATION_SUMMARY = "Create Application Summary";
		public static final String EDIT_TEMPLATE_PARAMETER = "Edit Template Parameter";
		public static final String REMOVE_LABEL = "Remove Label";
		public static final String RESOURCE_LABEL = "Resource Label";
		public static final String TEMPLATE_DETAILS = "Template Details";
		public static final String WEBHOOK_TRIGGERS = "Webhooks triggers";
		
		// Resources related
		public static final String CREATE_RESOURCE_SUMMARY = "Create Resource Summary";
		public static final String DELETE_RESOURCE = "Delete OpenShift Resource";
		
		// Others
		public static final String BINARY_LOCATION_UNKNOWN = "Unknown executable location";
		public static final String CHEATSHEET = "Found cheatsheet";
		public static final String LOADING_CONNECTION_DETAILS = "Loading OpenShift 2 connection details";
		public static final String SERVICE_PORTS = "Service Ports";
		public static final String SERVER_ADAPTER_SETTINGS = "OpenShift Server Adapter Settings";
	}
	
	/**
	 * Button labels.
	 */
	public static class Button {
		public static final String APPLY = "Apply";
		public static final String ADD = "Add...";
		public static final String ADD_SSH_KEY = "Add Existing...";
		public static final String ADVANCED_OPEN = " Advanced >> ";
		public static final String ADVANCED_CLOSE = " << Advanced ";
		public static final String BROWSE = "Browse...";
		public static final String CLOSE = "Close";
		public static final String COMMIT= "Commit";
		public static final String COMMIT_PUBLISH = "Commit and Publish";
		public static final String COMMIT_PUSH = "Commit and Push";
		public static final String CREATE_DOMAIN = "New...";
		public static final String CREATE_SSH_KEY = "New...";
		public static final String DEFINED_RESOURCES = "Defined Resources...";
		public static final String DESELECT_ALL = "Deselect all";
		public static final String EDIT = "Edit...";
		public static final String EDIT_DOMAIN = "Edit...";
		public static final String ENV_VAR = "Environment Variables... ";
		public static final String NEW = "New...";
		public static final String REFRESH = "Refresh...";
		public static final String REMOVE = "Remove...";
		public static final String REMOVE_BASIC = "Remove";
		public static final String RESET = "Reset";
		public static final String RESET_ALL = "Reset All";
		public static final String SELECT_ALL = "Select all";
		public static final String START_ALL = "Start All";
		public static final String STOP_ALL = "Stop All";
		public static final String BROWSE_WORKSPACE = "Browse Workspace...";
		public static final String WORKSPACE = "Workspace...";
	}
	
	public static class TextLabels {	
		// Connection related
		public static final String NEW_CONNECTION = "<New Connection>";
		public static final String CREATE_CONNECTION = "No connections are available. "
				+ "Create a new connection with the New Connection Wizard...";
		public static final String CONNECTION = "Connection:";
		public static final String PASSWORD = "Password:";
		public static final String SERVER = "Server:";
		public static final String USERNAME = "Username:";
		public static final String SELECT_LOCAL_TEMPLATE = "Select a local template file or a full URL:";
		public static final String SERVER_TYPE = "Server type:";
		public static final String STORE_PASSWORD = "Save password (could trigger secure storage login)";
		public static final String STORE_TOKEN= "Save token (could trigger secure storage login)";
		public static final String CHECK_SERVER_TYPE = "Check Server Type";
		public static final String PROTOCOL = "Protocol:";
		public static final String TOKEN = "Token";
		public static final String RETRIEVE_TOKEN = "Enter a token or retrieve a new one.";
		public static final String LINK_RETRIEVE = "retrieve";

		// Domain related
		public static final String DOMAIN_NAME = "Domain Name:";
	
		// Docker integration related
		public static final String IMAGE_NAME = "Image Name: ";
		public static final String RESOURCE_NAME = "Resource Name: ";
		
		// Project related
		public static final String PROJECT_NAME = "Project Name:";
		public static final String PROJECT_DISPLAYED_NAME = "Display Name:";
		public static final String PROJECT_DESCRIPTION = "Description:";
		public static final String PROJECT = "OpenShift project: ";
		
		
		// SSH Key related
		public static final String NAME = "Name:";
		public static final String PRIVATE_NAME = "Private Key File Name:";
		public static final String PUBLIC_NAME = "Public Key File Name:";
		public static final String PUB_KEY = "Public Key:";
		
		// Wizard
		public static final String CARTRIDGE_URL = "Cartridge URL:";
		public static final String SOURCE_CODE = "Source code:";
		
		// Embeddable Cartridge
		public static final String EMBEDDED_CARTRIDGE_URL = "Cartridge URL:";
		
		// Application related
		public static final String LOCAL_TEMPLATE = "Local template";
		public static final String DESTINATION = "Destination:";
		public static final String SERVER_TEMPLATE = "Server application source";
		public static final String TAIL_OPTIONS = "Tail options:";	
		
		// Webhook
		public static final String GENERIC_WEBHOOK = "Generic webhook:";
		public static final String GITHUB_WEBHOOK = "GitHub webhook:";
		
		// Labels
		public static final String LABEL = "Label:";
		public static final String OC_LOCATION = "'oc' executable location";
		public static final String VALUE = "Value:";
		public static final String REMOTE_REQUEST_TIMEOUT = "Remote requests timeout (in seconds):";
		public static final String FIND_FREE_PORTS = "Find free local ports for remote ports";
		public static final String RESOURCE_LOCATION = "Enter a file path (workspace or local) or a full URL.";
		public static final String BUILDER_RESOURCE_NAME = "Name: ";
		
		// Git related
		public static final String GIT_REPO_URL = "Git Repository URL:";
		public static final String GIT_REF = "Git Reference:";
		public static final String CONTEXT_DIR = "Context Directory:";
		
		// Ports
		public static final String SERVICE_PORT = "Service port:";
		public static final String POD_PORT = "Pod port:";
	}
	
	/**
	 * Magic pond.
	 */
	public static class Others {
		public static final String CONNECT_TOOL_ITEM = "Connection...";
		public static final String EAP_TEMPLATE = "eap64-basic-s2i (eap, javaee, java, jboss, xpaas) - openshift";
		public static final String EAP_BUILDER_IMAGE = "jboss-eap64-openshift:1.3 (builder, eap, javaee, java, jboss, xpaas) - openshift";
		public static final String NODEJS_TEMPLATE = "nodejs-example (quickstart, nodejs) - openshift";
		public static final String RED_HAT_CENTRAL = "Red Hat Central";
		public static final String MAVEN_MIRROR_URL = "MAVEN_MIRROR_URL";
		public static final String[] NEW_APP_MENU = {"File", "New", "OpenShift Application"};
		public static final String OPENSHIFT_APP = "OpenShift Application";
		public static final String OPENSHIFT_CENTRAL_SCRIPT = "$(\"#wizards\" ).find('a').filter(\""
				+ ":contains('OpenShift Application')\").click()";
		
		// Server adapter
		public static final String[] OS2_SERVER_ADAPTER = new String[] {"OpenShift", 
				"OpenShift 2 Server Adapter"};
		public static final String[] OS3_SERVER_ADAPTER = new String[] {"OpenShift", 
				"OpenShift 3 Server Adapter"};

	}
}
