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
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

/**
 * @author Red Hat Developers
 *
 */
public class SandboxModel extends ObservableUIPojo {
  public static final String PROPERTY_ID_TOKEN = "IDToken";
  
  public static final String PROPERTY_PHONE_NUMBER = "phoneNumber";
  
  public static final String PROPERTY_COUNTRY_CODE = "countryCode";
  
  public static final String PROPERTY_VERIFICATION_CODE = "verificationCode";
  
  public static final String PROPERTY_CLUSTER_URL = "clusterURL";
  
  public static final String PROPERTY_CLUSTER_TOKEN = "clusterToken";
  
  private String IDToken;
  
  private String phoneNumber;
  
  private String countryCode;
  
  private String verificationCode;
  
  private String clusterURL;
  
  private String clusterToken;
  
  /**
   * @return the IDToken
   */
  public String getIDToken() {
    return IDToken;
  }

  /**
   * @param IDToken the IDToken to set
   */
  public void setIDToken(String IDToken) {
    firePropertyChange(PROPERTY_ID_TOKEN, this.IDToken, this.IDToken = IDToken);
  }

  /**
   * @return the phoneNumber
   */
  public String getPhoneNumber() {
    return phoneNumber;
  }

  /**
   * @param phoneNumber the phoneNumber to set
   */
  public void setPhoneNumber(String phoneNumber) {
    firePropertyChange(PROPERTY_PHONE_NUMBER, this.phoneNumber, this.phoneNumber = phoneNumber);
  }

  /**
   * @return the countryCode
   */
  public String getCountryCode() {
    return countryCode;
  }

  /**
   * @param countryCode the countryCode to set
   */
  public void setCountryCode(String countryCode) {
    firePropertyChange(PROPERTY_COUNTRY_CODE, this.countryCode, this.countryCode = countryCode);
  }

  /**
   * @return the verificationCode
   */
  public String getVerificationCode() {
    return verificationCode;
  }

  /**
   * @param verificationCode the verificationCode to set
   */
  public void setVerificationCode(String verificationCode) {
    firePropertyChange(PROPERTY_VERIFICATION_CODE, this.verificationCode, this.verificationCode = verificationCode);
  }

  /**
   * @return the clusterURL
   */
  public String getClusterURL() {
    return clusterURL;
  }

  /**
   * @param clusterURL the clusterURL to set
   */
  public void setClusterURL(String clusterURL) {
    firePropertyChange(PROPERTY_CLUSTER_URL, this.clusterURL, this.clusterURL = clusterURL);
  }

  /**
   * @return the clusterToken
   */
  public String getClusterToken() {
    return clusterToken;
  }

  /**
   * @param clusterToken the clusterToken to set
   */
  public void setClusterToken(String clusterToken) {
    firePropertyChange(PROPERTY_CLUSTER_TOKEN, this.clusterToken, this.clusterToken = clusterToken);
  }
}
