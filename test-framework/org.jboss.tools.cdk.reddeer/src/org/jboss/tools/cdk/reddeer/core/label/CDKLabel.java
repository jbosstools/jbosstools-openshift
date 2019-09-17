/******************************************************************************* 
 * Copyright (c) 2018 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.cdk.reddeer.core.label;

import org.jboss.tools.cdk.reddeer.utils.CDKUtils;

/**
 * Encapsulating class for statically accessed subclasses with labels
 * @author odockal
 *
 */
public class CDKLabel {
	
	public static class Job {
		public static final String MINISHIFT_VALIDATION_JOB = "Validate minishift location";
		public static final String REFRESHING_SERVER_ADAPTER_LIST = "Refreshing server adapter list";
	}
	
	public static class ServerContextMenu {
		public static final String START = "Start";
		public static final String RESTART = "Restart";
		public static final String STOP = "Stop";
		public static final String [] NEW_SERVER = {"New", "Server"};
		public static final String SETUP_CDK = "Setup CDK";
	}
	
	public static class Shell {
		public static final String UNTRUSTED_SSL_DIALOG = "Untrusted SSL Certificate"; 
		public static final String MULTIPLE_PROBLEMS_DIALOG = "Multiple problems have occurred"; 
		public static final String PROBLEM_DIALOG = "Problem Occurred"; 
		public static final String LAUNCH_CONFIG_DIALOG = "Edit Configuration";
		public static final String ADD_CREDENTIALS_DIALOG = "Add a Credential";
		public static final String SECURE_STORAGE_DIALOG = "Secure Storage Password";
		public static final String NEW_SERVER_WIZARD = "New Server";
		public static final String WARNING_FOLDER_EXISTS = "Warning: Folder already exists!";
		public static final String WARNING_CDK_NOT_INICIALIZED = "Warning: CDK has not been properly initialized!";
		public static final String DOWNLOAD_RUNTIMES = "Download Runtimes";
	}
	
	public static class Labels {
		public static final String PASSWORD = "Password: ";
		public static final String USERNAME = "Username: ";
		public static final String DOMAIN = "Domain: ";
		public static final String MINISHIFT_PROFILE = "Minishift Profile:";
		public static final String MINISHIFT_BINARY = "Minishift Binary: ";
		public static final String HYPERVISOR = "Hypervisor:";
		public static final String MINISHIFT_HOME = "Minishift Home:";	
		public static final String SERVER_NAME = "Server name:";	
		public static final String HOST_NAME = "Host name:";
		public static final String VAGRANTFILE_LOCATION = "Vagrantfile Location: ";
		public static final String LOCATION = "Location:";
		public static final String ARGUMENTS = "Arguments:";
		public static final String NAME = "Name:";
		public static final String ADD_CREDENTIAL = "Add a Credential";
		public static final String FOLDER = "Folder: ";
		public static final String SECURE_STORAGE = "Secure Storage";
		public static final String CRC_PULL_SECRET_FILE = "CRC Pull Secret File: ";
		public static final String CRC_BINARY = "CRC Binary: ";
		public static final String CRC_PULL_SECRET_FILE_EDITOR = "Pull Secret File:";
		public static final String CRC_BINARY_EDITOR = "crc binary file:";
	}
	
	public static class Links {
		public static final String DOWNLOAD_AND_INSTALL_RUNTIME = "Download and install runtime...";
	}
	
	public static class Sections {
		public static final String CDK_DETAILS = "CDK Details";
		public static final String CRC_DETAILS = "CRC Details";
		public static final String GENERAL = "General Information";
		public static final String CREDENTIALS = "Credentials";
	}
	
	public static class Buttons {
		public static final String ADD = "Add...";
		public static final String EDIT = "Edit...";
		public static final String YES = "Yes";
		public static final String NO = "No";
		public static final String PASS_CREDENTIALS_TO_ENV = "Pass credentials to environment";
		public static final String BROWSE = "Browse...";
		public static final String SKIP_REGISTRATION_STARTING = "Skip Registration when starting";
		public static final String SKIP_UNREGISTRATION_STOPPING = "Skip Unregistration when stopping";
		public static final String OK = "OK";
		public static final String CANCEL = "Cancel";
		public static final String APPLY = "Apply";
		public static final String REVERT = "Revert";
		public static final String ALWAYS_PROMPT_FOR_PASSWORD = "Always prompt for password";
		public static final String SHOW_PASSWORD = "Show password";
		public static final String REMOVE_USER = "Remove User";
	}
	
	public static class Server {
		// Server Adapter Wizard strings - Server artifacts
		public static final String SERVER_HOST = "localhost"; 
		public static final String SERVER_TYPE_GROUP = "Red Hat JBoss Middleware"; 
		public static final String CDK_SERVER_NAME = "Red Hat Container Development Kit 2.x"; 
		public static final String CDK3_SERVER_NAME = "Red Hat Container Development Kit 3"; 
		public static final String CDK32_SERVER_NAME = "Red Hat Container Development Kit 3.2+";
		public static final String CRC_SERVER_NAME = "Red Hat CodeReady Containers 1.0 (Tech Preview)"; 
		public static final String MINISHIFT_SERVER_NAME = "Minishift 1.7+"; 
	}
	
	public static class Messages {
		// page description messages
		
		public static final String NO_USER = "Red Hat Access credentials"; 
		public static final String DOES_NOT_EXIST = "does not exist";
		public static final String NOT_FILE = "is not a file"; 
		public static final String CANNOT_RUN_PROGRAM = "Cannot run program"; 
		public static final String NOT_EXECUTABLE = CDKUtils.IS_WINDOWS ? CANNOT_RUN_PROGRAM : "is not executable"; 
		public static final String CHECK_MINISHIFT_VERSION = "Unknown error while checking minishift version"; 
		public static final String NOT_COMPATIBLE = "is not compatible with this server adapter";
		public static final String SERVER_ADAPTER_REPRESENTING = "server adapter representing";
		public static final String SELECT_VALID_IMAGE = "Please select a valid Image";
		public static final String SELECT_VALID_SECRET_FILE = "Please select a valid Pull Secret file.";
	}
	
	public static class Others {
		public static final String CREDENTIALS_DOMAIN = "access.redhat.com";
		public static final String FILE_ARCH = "-amd64";
	}

}
