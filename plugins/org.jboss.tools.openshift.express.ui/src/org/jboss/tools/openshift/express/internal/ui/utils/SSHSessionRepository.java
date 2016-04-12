/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.internal.credentials.EGitCredentialsProvider;
import org.eclipse.jgit.transport.CredentialsProviderUserInfo;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;
import org.eclipse.jsch.core.IJSchService;
import org.eclipse.jsch.ui.UserInfoPrompter;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.console.JschToEclipseLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.openshift.client.IApplication;
import com.openshift.client.OpenShiftSSHOperationException;

/**
 * Same as EclipseSshSessinFactory, but provides a mean to retrieve the pure Jsch Session, not a RemoteSession.
 * 
 * @author Xavier Coulon
 * 
 */
@SuppressWarnings("restriction")
public class SSHSessionRepository extends JschConfigSessionFactory {

	private static SSHSessionRepository INSTANCE = new SSHSessionRepository();

	/**
	 * Get the currently configured JVM-wide factory.
	 * <p>
	 * A factory is always available. By default the factory will read from the user's <code>$HOME/.ssh</code> and
	 * assume OpenSSH compatibility.
	 * 
	 * @return factory the current factory for this JVM.
	 */
	public static SSHSessionRepository getInstance() {
		return INSTANCE;
	}

	private final IJSchService provider;

	private OpenSshConfig config;

	private final Map<URIish, Session> cache = new HashMap<>();

	SSHSessionRepository() {
		final BundleContext context = ExpressUIActivator.getDefault().getBundle().getBundleContext();
		final ServiceReference<?> ssh = context.getServiceReference(IJSchService.class.getName());
		this.provider = (IJSchService) context.getService(ssh);
	}

	public Session getSession(final IApplication application) throws OpenShiftSSHOperationException {
		try {
			final URIish uri = getSshUri(application);
			final Session session = cache.get(uri);
			if (session == null 
					|| !session.isConnected()) {
				final FS fs = FS.DETECTED;
				if (config == null) {
					config = OpenSshConfig.get(fs);
				}
				String user = uri.getUser();
				String host = uri.getHost();
				int port = uri.getPort();
				JSch.setLogger(new JschToEclipseLogger());
				final OpenSshConfig.Host hc = config.lookup(host);
				try {
					cache.put(uri, createSession(hc, user, host, port, fs));
				} catch (JSchException e) {
					throw new OpenShiftSSHOperationException(e, "Could not create SSH session for application ''{0}''", application.getName());
				}
			}
			return cache.get(uri);
		} catch (URISyntaxException e1) {
			throw new OpenShiftSSHOperationException(e1, "Could not create SSH Session for application ''{0}''", application.getName());
		}
	}

	static URIish getSshUri(IApplication application) throws URISyntaxException {
		final URI sshURI = new URI(application.getSshUrl());
		final String host = sshURI.getHost();
		final String user = sshURI.getUserInfo();
		final URIish uri = new URIish().setHost(host).setPort(22).setUser(user);
		return uri;
	}
	
	@Override
	protected Session createSession(final OpenSshConfig.Host hc, final String user, final String host, final int port,
			FS fs) throws JSchException {
		final JSch jsch = getJSch(hc, FS.DETECTED);
		if (jsch == provider.getJSch()) {
			// If its the default JSch desired, let the provider
			// manage the session creation for us.
			//
			final Session session = provider.createSession(host, port, user);
			configure(hc, session);
			session.connect();
			return session;
		} else {
			// This host configuration is using a different IdentityFile,
			// one that is not available through the default JSch.
			//
			final Session session = jsch.getSession(user, host, port);
			configure(hc, session);
			session.connect(0);
			return session;
		}
	}

	@Override
	protected JSch createDefaultJSch(FS fs) throws JSchException {
		// Forcing a dummy session to be created will cause the known hosts
		// and configured private keys to be initialized. This is needed by
		// our parent class in case non-default JSch instances need to be made.
		//
		provider.createSession("127.0.0.1", 0, "eclipse"); //$NON-NLS-1$ //$NON-NLS-2$
		return provider.getJSch();
	}

	@Override
	protected void configure(final OpenSshConfig.Host hc, final Session session) {
		final EGitCredentialsProvider credentialsProvider = new EGitCredentialsProvider();
		if ((!hc.isBatchMode() || !credentialsProvider.isInteractive())) {
			session.setUserInfo(new CredentialsProviderUserInfo(session, credentialsProvider));
		} else {
			UserInfo userInfo = session.getUserInfo();

			if (!hc.isBatchMode() && userInfo == null)
				new UserInfoPrompter(session);
		}
	}

}