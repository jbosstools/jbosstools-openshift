/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.domain;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.common.ui.databinding.ObservableUIPojo;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.utils.UrlUtils;
import org.jboss.tools.openshift.express.core.util.ExpressConnectionUtils;
import org.jboss.tools.openshift.express.internal.core.connection.ExpressConnection;
import org.jboss.tools.openshift.express.internal.ui.ExpressUIActivator;
import org.jboss.tools.openshift.express.internal.ui.utils.Logger;

import com.openshift.client.IDomain;
import com.openshift.client.IUser;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 * @author Xavier Coulon
 */
public class DomainWizardModel extends ObservableUIPojo {

	public static final String PROPERTY_DOMAIN_ID = "domainId";
	
	private static final String OPENSHIFT_ENTERPRISE_WEBUI_DOMAINPAGE = "{0}/console/domain/{1}";
	private static final String OPENSHIFT_ORIGIN_WEBUI_DOMAINPAGE = "{0}/app/console/domain/{1}";

	private String domainId;
	private final IDomain domain;
	private ExpressConnection connection;

	public DomainWizardModel(ExpressConnection connection) {
		this(null, connection);
	}

	public DomainWizardModel(IDomain domain) {
		this(domain, null);
	}

	protected DomainWizardModel(IDomain domain, ExpressConnection connection) {
		this.domain = domain;
		this.connection = connection;
		if (domain == null) {
			return;
		}
		setDomainId(domain.getId());
	}

	public String getDomainId() {
		return this.domainId;
	}

	public void setDomainId(String domainId) {
		firePropertyChange(PROPERTY_DOMAIN_ID,
				this.domainId, this.domainId = domainId);
	}

	public void renameDomain() throws OpenShiftException, SocketTimeoutException {
		if (domain == null) {
			Logger.warn("Attempting to rename missing user domain...");
			return;
		}
		domain.rename(domainId);
		fireConnectionChanged(domain, connection);
	}

	private void fireConnectionChanged(IDomain domain, ExpressConnection connection) {
		if (connection == null) {
			connection = ExpressConnectionUtils.getByResource(domain.getUser(), ConnectionsRegistrySingleton.getInstance());
		}
		ConnectionsRegistrySingleton.getInstance().fireConnectionChanged(connection);
	}

	public boolean isCurrentDomainId(String domainId) {
		try {
			if (domain == null) {
				return false;
			}
			return domain.getId().equals(domainId);
		} catch (Exception e) {
			ExpressUIActivator.log(e);
			return true;
		}
	}

	public IDomain getDomain() {
		return domain;
	}
	
	public ExpressConnection getConnection() {
		return connection;
	}

	public String getWebUIDomainPageUrl() {
		if (getDomain() == null
				|| getDomain().getUser() == null) {
			return null;
		}

		String domainWebUIUrl = getOriginWebUIDomainPageUrl();
		if (isUrlAvailable(domainWebUIUrl)) {
			return domainWebUIUrl;
		}
		domainWebUIUrl = getEnterpsiseWebUIDomainPageUrl();
		if (isUrlAvailable(domainWebUIUrl)) {
			return domainWebUIUrl;
		}
		return null;	
	}
	
	public String getOriginWebUIDomainPageUrl() {
		if (getDomain() == null) {
			return null;
		}
		String host = getDomain().getUser().getServer();
		return MessageFormat.format(OPENSHIFT_ORIGIN_WEBUI_DOMAINPAGE, host, domainId);
	}
	
	public String getEnterpsiseWebUIDomainPageUrl() {
		if (getDomain() == null) {
			return null;
		}
		String host = getDomain().getUser().getServer();
		return MessageFormat.format(OPENSHIFT_ENTERPRISE_WEBUI_DOMAINPAGE, host, domainId);
	}
	
	private boolean isUrlAvailable(String domainWebUIUrl) {
		try {
			URL url = new URL(domainWebUIUrl);
			try {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				if (connection instanceof HttpsURLConnection) {
					UrlUtils.setupPermissiveSSLHandlers((HttpsURLConnection) connection);
				}
				IUser user = getDomain().getUser();
				UrlUtils.addBasicAuthorization(user.getRhlogin(), user.getPassword(), connection);

				connection.connect();
				return connection.getResponseCode() == 200;
			} catch (KeyManagementException e) {
				ExpressUIActivator.log(NLS.bind(
						"Could not install permissive trust manager and hostname verifier for connection {0}", url), e);
				return false;
			} catch (NoSuchAlgorithmException e) {
				ExpressUIActivator.log(NLS.bind(
						"Could not install permissive trust manager and hostname verifier for connection {0}", url), e);
				return false;
			} catch (IOException e) {
				return false;
			}
		} catch (MalformedURLException e) {
			return false;
		}
	}
	
}
