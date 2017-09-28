/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.io.core;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.core.resources.IResource;
import org.jboss.tools.openshift.io.core.exception.OpenshiftIOConfigurationException;
import org.jboss.tools.openshift.io.core.exception.OpenshiftIOLoginException;
import org.jboss.tools.openshift.io.core.exception.OpenshiftIORefreshException;
import org.jboss.tools.openshift.io.core.model.ICluster;
import org.jboss.tools.openshift.io.core.model.IAccount;
import org.jboss.tools.openshift.io.core.model.IAccountModel;
import org.jboss.tools.openshift.io.internal.core.model.AccountModel;

public class AccountService {

	private static final AccountService INSTANCE = new AccountService();

	private final LoginProvider provider = LoginProvider.get();

	private HttpClient client = HttpClients.createDefault();

	private IAccountModel model;

	private AccountService() {
	}

	public static AccountService getDefault() {
		return INSTANCE;
	}

	public IAccountModel getModel() {
		if (null == model) {
			model = new AccountModel();
		}
		return model;
	}

	public AccountStatus getStatus(IAccount account) {
		if (account.getAccessToken() == null) {
			return AccountStatus.NEEDS_LOGIN;
		}
		long lastRefreshed = account.getLastRefreshedTime();
		long current = System.currentTimeMillis();
		if (current > account.getAccessTokenExpiryTime()) {
			if (current > account.getRefreshTokenExpiryTime()) {
				return AccountStatus.NEEDS_LOGIN;
			} else {
				return AccountStatus.NEEDS_REFRESH;
			}
		}
		if (((current - lastRefreshed) > OSIOCoreConstants.DURATION_24_HOURS)
				|| ((current - lastRefreshed) > ((account.getAccessTokenExpiryTime() - current) / 2))) {
			return AccountStatus.NEEDS_REFRESH;
		}
		return AccountStatus.VALID;
	}

	public String getToken(IResource t) {
		String token = null;

		ICluster cluster = getModel().getClusters().get(0);
		List<IAccount> identities = cluster.getAccounts();
		if (identities.isEmpty()) {
			token = performLogin(cluster, null);
		} else {
			IAccount account = identities.get(0);
			AccountStatus status = getStatus(account);
			switch (status) {
			case VALID:
				token = account.getAccessToken();
				break;
			case NEEDS_REFRESH:
				token = performRefresh(account);
				break;
			case NEEDS_LOGIN:
				token = performLogin(cluster, account);
				break;
			}

		}
		return token;
	}

	private String performLogin(ICluster cluster, IAccount account) {
		if (null != provider) {
			LoginResponse response = provider.login(cluster, account);
			if (null != response) {
				if (null == account) {
						String id = OSIOUtils.decodeEmailFromToken(response.getAccessToken());
						IAccount newAccount = cluster.createAccount(id);
						updateAccount(response, newAccount);
						cluster.addAccount(newAccount);
						return newAccount.getAccessToken();
				} else {
					updateAccount(response, account);
				}
				return account.getAccessToken();
			} else {
				throw new OpenshiftIOLoginException(cluster, account);
			}
		} else {
			throw new OpenshiftIOConfigurationException("No login provider found");
		}
	}

	void updateAccount(LoginResponse info, IAccount account) {
		account.setAccessToken(info.getAccessToken());
		account.setRefreshToken(info.getRefreshToken());
		account.setLastRefreshedTime(System.currentTimeMillis());
		account.setAccessTokenExpiryTime(OSIOUtils.decodeExpiryFromToken(info.getAccessToken()));
		account.setRefreshTokenExpiryTime(OSIOUtils.decodeExpiryFromToken(info.getRefreshToken()));
		account.save();
	}

	private String performRefresh(IAccount account) {
		HttpPost post = new HttpPost(account.getCluster().getEndpointURL() + OSIOCoreConstants.REFRESH_SUFFIX);
		try {
			HttpEntity entity = new StringEntity("{\"refresh_token\":\"" + account.getRefreshToken() + "\"}");
			post.setEntity(entity);
			try (CloseableHttpResponse httpResp = (CloseableHttpResponse) client.execute(post)) {
				int status = httpResp.getStatusLine().getStatusCode();
				if (HttpStatus.SC_OK == status) {
					RefreshResponse response = OSIOUtils
							.decodeRefreshResponse(EntityUtils.toString(httpResp.getEntity()));
					account.setAccessToken(response.getLoginResponse().getAccessToken());
					account.setRefreshToken(response.getLoginResponse().getRefreshToken());
					account.setAccessTokenExpiryTime(
							OSIOUtils.decodeExpiryFromToken(response.getLoginResponse().getAccessToken()));
					account.setRefreshTokenExpiryTime(
							OSIOUtils.decodeExpiryFromToken(response.getLoginResponse().getRefreshToken()));
					account.setLastRefreshedTime(System.currentTimeMillis());
					account.save();
					return account.getAccessToken();
				} else {
					throw new OpenshiftIORefreshException(account, status);
				}
			}
		} catch (ParseException | IOException e) {
			throw new OpenshiftIORefreshException(account, e);
		}
	}
}
