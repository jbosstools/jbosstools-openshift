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
package org.jboss.tools.openshift.cdk.server.test.internal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jboss.jdf.stacks.model.Stacks;
import org.jboss.tools.stacks.core.model.StacksManager;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for stack runtimes available from remote or local yaml file
 * @author ondrej dockal
 *
 */
public class CDKStackRuntimeTest {

	private static final String MINISHIFT_YAML_URL = "https://raw.githubusercontent.com/jboss-developer/jboss-stacks/1.0.0.Final/minishift.yaml";
	private static final String [] BUNDLED_YAML_PATH = {"resources", "minishift.yaml"};
	private static final String CREDENTIALS_USERNAME_KEY = "developers.username";
	private static final String CREDENTIALS_PASSWORD_KEY = "developers.password";
	private static boolean useCredentials = false;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static StacksManager stackManager;

	private static List<Object> results = new ArrayList<Object>();
    private static Log log = LogFactory.getLog(CDKStackRuntimeTest.class);
	
    static {
    	USERNAME = System.getProperty(CREDENTIALS_USERNAME_KEY);
    	PASSWORD = System.getProperty(CREDENTIALS_PASSWORD_KEY);
    	if (USERNAME == null || USERNAME.isEmpty() || PASSWORD == null) {
    		useCredentials = false;
    	} else {
    		useCredentials = true;
    	}
    }
    
    @BeforeClass
    public static void setupStackManager() {
    	stackManager = new StacksManager();
    }
    
    @After
    public void showResults() {
    	if (!results.isEmpty()) {
	    	results.clear();
    	}
    }
    
	@Test
	public void testLoadingLocalYaml() {
		loadStacksFromYaml(loadURLFromFile(BUNDLED_YAML_PATH));
	}
	
	@Test
	public void testLoadingRemoteYaml() {
		loadStacksFromYaml(loadURLFromString(MINISHIFT_YAML_URL));
	}
	
	@Test
	public void testMinishiftRemoteYamlURLAvailability() {
		testJBossStackRuntimeURLsAvailability(
				loadURLFromString(MINISHIFT_YAML_URL), "minishift", 302,
				x -> getHttpHeadRequestStatusCode(x));
	}
	
	@Test
	public void testCDKRemoteYamlURLAvailability() {
		if (useCredentials) {
			testJBossStackRuntimeURLsAvailability(
					loadURLFromString(MINISHIFT_YAML_URL), "cdk", 302,
					x -> getAuthorizedHttpHeadRequestStatusCode(x));			
		} else {
			testJBossStackRuntimeURLsAvailability(
					loadURLFromString(MINISHIFT_YAML_URL), "cdk", 401,
					x -> getHttpHeadRequestStatusCode(x));
		}
	}
	
	@Test
	public void testMinishiftBundledYamlURLAvailability() {
		testJBossStackRuntimeURLsAvailability(
				loadURLFromFile(BUNDLED_YAML_PATH), "minishift", 302,
				x -> getHttpHeadRequestStatusCode(x));
	}
	
	@Test
	public void testCDKBundledYamlURLAvailability() {
		if (useCredentials) {
			testJBossStackRuntimeURLsAvailability(
					loadURLFromFile(BUNDLED_YAML_PATH), "cdk", 302,
					x -> getAuthorizedHttpHeadRequestStatusCode(x));			
		} else {
			testJBossStackRuntimeURLsAvailability(
					loadURLFromFile(BUNDLED_YAML_PATH), "cdk", 401,
					x -> getHttpHeadRequestStatusCode(x));
		}
	}
	
    public static int getHttpHeadRequestStatusCode(String url) {
    	int response = 0;
    	try {
			HttpURLConnection con = getHttpConnection(url, "HEAD", null);
			response = con.getResponseCode();
			con.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Sending Http head request to " + url + " ended up with " + e.getMessage());
		}
    	return response;
    }
    
    public static int getAuthorizedHttpHeadRequestStatusCode(String url) {
    	int response = 0;
    	try {
			HttpURLConnection con = getHttpConnection(url, "HEAD", encodeCredentials(USERNAME, PASSWORD));
			response = con.getResponseCode();
			con.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Sending authorized Http head request to " + url + " ended up with " + e.getMessage());
		}
    	return response;
    }
    
    /**
     * Example curl command:  
	 * curl --verbose -L -u user:pass -H "Content-Type: application/xml" -H "Accept: application/xml" -O https://www.jboss.org/download-manager/jdf/file/cdk-3.2.0-1-minishift-linux-amd64
     * @param url address of the resource
     * @param requestMethod method to be used in request
     * @param basicAuth authentication string if provided will be added into request
     * @return HttpURLConnection object representing http request
     * @throws IOException
     */
    public static HttpURLConnection getHttpConnection(String url, String requestMethod, String basicAuth) throws IOException {
    	log.info("Creating HTTP " + requestMethod + " request for " + url);
    	HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setInstanceFollowRedirects(false);
		if (basicAuth != null && !basicAuth.isEmpty()) {
			log.info("Setting Authorization property");
			con.setRequestProperty("Authorization", basicAuth);
		}
		con.setRequestProperty("Content-Type", "application/xml");
		con.setRequestProperty("Accept", "application/xml");
		con.setRequestMethod(requestMethod);
		con.setReadTimeout(30000);
		return con;
    }
	
	public static String encodeCredentials(String user, String pass) {
		String userPass = user + ":" + pass;
		return "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
	}
	
	private Stacks loadStacksFromYaml(URL url) {
		Stacks stacks = null;
		try {
			stacks = stackManager.getStacks(url.toString(), new NullProgressMonitor());
		} catch (Exception exc) {
			fail("Exception occured during getting stack from yaml file " + exc.getMessage());
		}
		return stacks;
	}
	
	private URL loadURLFromFile(String... path) {
		URL yamlUrl = null;
		try {
			yamlUrl = new File(getProjectResourceAbsolutePath(path)).toURI().toURL();
		} catch (FileNotFoundException fnf) {
			fail(BUNDLED_YAML_PATH + " file was not found within project with message: " + fnf.getMessage());
		} catch (MalformedURLException e) {
			fail("Given url: " + yamlUrl + " is malformed: " + e.getMessage());
		}
		return yamlUrl;
	}
	
	private URL loadURLFromString(String url) {
		URL yamlUrl = null;
		try {
			yamlUrl = new URL(MINISHIFT_YAML_URL);
		} catch (MalformedURLException e) {
			fail("Given url: " + yamlUrl + " is malformed: " + e.getMessage());
		}
		return yamlUrl;
	}
	
	private void testJBossStackRuntimeURLsAvailability(URL yamlUrl, String idFilter, 
			int expectedStatusCode, ProcessingHttpRequest request) {
		Stacks stacks = loadStacksFromYaml(yamlUrl);
    	for (org.jboss.jdf.stacks.model.Runtime runtime : stacks.getAvailableRuntimes()) {
    		if (runtime.getId().contains(idFilter)) {
	    		String runtimeName = runtime.getName();
	    		Properties allUrls = extractDownloadURLs(runtime);
	    		if (allUrls.size() != 3) {
	    			results.add("Verifying " + runtimeName
	    					+ ": Expects three urls, but got " + allUrls.size()
		    				+ ", available urls are: "
		    				+ joinList(allUrls.values(), "\r\n"));
	    		}
	    		for (Object key : allUrls.keySet()) {
	    			String url = allUrls.get(key).toString();
	    			int resp = request.process(url);
	    			if (resp != expectedStatusCode) {
	    				results.add("Verifying " + runtimeName 
	    						+ ": HTTP response status code for " + url + " does not match. "
	    						+ "Expected: " + expectedStatusCode + " but was: " + resp);
	    			}
	    		}
    		}
    	}
    	assertTrue(joinList(results, "\r\n"), results.isEmpty());
	}
    
    private Properties extractDownloadURLs(org.jboss.jdf.stacks.model.Runtime runtime) {
    	Properties properties = runtime.getLabels();
		String [] urls = properties.get("additionalDownloadURLs").toString().replaceAll("[{}]", " ").split(",");
		Properties props = new Properties();
		for (String item : urls) {
			String key = item.trim().split("=")[0];
			props.setProperty(key, item.trim().split("=")[1]);
		}
		return props;
    }
    
	/**
	 * Provide resource absolute path in project directory
	 * @param path - resource relative path
	 * @return resource absolute path
	 * @throws FileNotFoundException 
	 */
	public static String getProjectResourceAbsolutePath(String... path) throws FileNotFoundException {

		// Construct path
		StringBuilder builder = new StringBuilder();
		for (String fragment : path) {
			builder.append("/" + fragment); 
		}

		String filePath = ""; 
		filePath = System.getProperty("user.dir"); 
		File file = new File(filePath + builder.toString());
		if (!file.exists()) {
			throw new FileNotFoundException("Resource file does not exists within project path " 
					+ filePath + builder.toString());
		}
		
		return file.getAbsolutePath();
	}
	
	public static String joinList(Collection<Object> list, String delimiter) {
		return list.stream().map(x -> x.toString()).collect(Collectors.joining(delimiter));
	}
	
	@FunctionalInterface
	private interface ProcessingHttpRequest {
		int process(String url);
	}
}
