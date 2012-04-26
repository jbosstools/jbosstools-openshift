package org.jboss.tools.openshift.ui.bot.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * 
 * @author sbunciak
 *
 * Utility class to destroy domain on OpenShift 
 */
public class DomainDestroyer {

    /**
     * 
     * Destroys registered domain on OpenShift
     * 
     * @param domain
     * @param login
     * @param password
     * @return HTTP Response code or 0 if some Exception was caught
     */
    public static int destroyDomain(String domain, String login, String password) {

        int resp_code = 0;
        String input = "{\"namespace\": \"" + domain + "\", \"rhlogin\": \""
                + login + "\", \"delete\": true }";

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(
                "https://openshift.redhat.com/broker/domain");

        method.addParameter("json_data", input);
        method.addParameter("password", password);

        try {
            resp_code = client.executeMethod(method);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            method.releaseConnection();
        }
        return resp_code;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    	int resp_code = DomainDestroyer.destroyDomain(
				TestProperties.getProperty("openshift.domain.new"),
				TestProperties.getProperty("openshift.user.name"),
				TestProperties.getProperty("openshift.user.pwd"));
    	
        if (resp_code == 200) {
        	System.out.println("Domain destroyed.");
        } else {
        	System.out.println("Domain was not destroyed. Response code: "+resp_code);
        }
    }
}
