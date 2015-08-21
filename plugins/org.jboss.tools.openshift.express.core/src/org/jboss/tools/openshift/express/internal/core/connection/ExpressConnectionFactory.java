/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.core.connection;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.common.core.connection.IConnectionFactory;
import org.jboss.tools.openshift.express.core.ExpressCoreUIIntegration;
import org.jboss.tools.openshift.express.internal.core.ExpressCoreActivator;

import com.openshift.client.OpenShiftException;
import com.openshift.client.configuration.OpenShiftConfiguration;


/**
 * @author Andre Dietisheim
 */
public class ExpressConnectionFactory implements IConnectionFactory {

	public ExpressConnectionFactory() {
	}

	@Override
	public String getName() {
		return "OpenShift 2";
	}

	@Override
	public String getId() {
		return "org.jboss.tools.openshift.express.core.ConnectionFactory";
	}
	
	@Override
	public ExpressConnection create(String url) {
		return new ExpressConnection(url, ExpressCoreUIIntegration.getDefault().getSSLCertificateCallback());
	}


	@Override
	public String getDefaultHost() {
		try {
			return new OpenShiftConfiguration().getLibraServer();
		} catch (OpenShiftException e) {
			ExpressCoreActivator.pluginLog().logError("Could not load default host.", e);
			return null;
		} catch (IOException e) {
			ExpressCoreActivator.pluginLog().logError("Could not load default host.", e);
			return null;
		}
	}

	@Override
	public boolean hasDefaultHost() {
		return true;
	}

	@Override
	public <T extends IConnection> boolean canCreate(Class<T> clazz) {
		return ExpressConnection.class == clazz;
	}

	@Override
	public String getSignupUrl(String host) {
		if (StringUtils.isEmpty(host)
				|| !host.equals(getDefaultHost())) {
			return null;
		}
		return host + "/app/user/new/express";
	}
}
