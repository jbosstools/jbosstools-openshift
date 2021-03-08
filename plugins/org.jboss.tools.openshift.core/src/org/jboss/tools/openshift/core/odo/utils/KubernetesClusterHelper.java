/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo.utils;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Red Hat Developers
 *
 */
public class KubernetesClusterHelper {

  /**
   * 
   */
  private static final String REQUEST_TOKEN_PATH = "request";

  /**
   * 
   */
  private static final String TOKEN_ENDPOINT_KEY = "token_endpoint";

  /**
   * 
   */
  private static final String WELL_KNOWN_OAUTH_AUTHORIZATION_SERVER = ".well-known/oauth-authorization-server";

  private static final X509TrustManager ACCEPT_ALL_CERTIFICATES = new X509TrustManager() {

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }
  };
  
  private static String append(String baseURL, String suffix) {
    if (baseURL.charAt(baseURL.length() - 1) == '/') {
      return baseURL + suffix;
    } else {
      return baseURL + '/' + suffix;
    }
  }

  public static Map<String, String> getOAuthAuthorizationServerInfo(String clusterURL) throws IOException {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, new TrustManager[] { ACCEPT_ALL_CERTIFICATES }, null);
      SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      OkHttpClient newClient = new OkHttpClient.Builder().sslSocketFactory(sslSocketFactory, ACCEPT_ALL_CERTIFICATES).build();
      Request request = new Request.Builder().url(new URL(append(clusterURL, WELL_KNOWN_OAUTH_AUTHORIZATION_SERVER)))
          .build();
      try (Response response = newClient.newCall(request).execute()) {
        String responseBody = response.body().string();
        return Serialization.unmarshal(responseBody, Map.class);
      }
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      throw new IOException();
    }
  }

  public static String getTokenEndpoint(String clusterURL) throws IOException {
    Map<String, String> config = getOAuthAuthorizationServerInfo(clusterURL);
    return config != null ? config.get(TOKEN_ENDPOINT_KEY) : null;
  }

  public static String getTokenRequest(String clusterURL) throws IOException {
    String tokenEndpoint = getTokenEndpoint(clusterURL);
    if (tokenEndpoint != null) {
      return append(tokenEndpoint, REQUEST_TOKEN_PATH);
    } else {
      return null;
    }
  }
}
