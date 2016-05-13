/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.tools.openshift.internal.ui.wizard.connection.OAuthDetailView.TokenExtractor;
import org.junit.Test;

/**
 * Testing the {@link OAuthDetailView} class
 * 
 * @author Jeff Maury
 *
 */
public class OAuthDetailViewTest {

    @Test
    public void testThatSimplePageDoesNotMatch() {
        assertFalse(new TokenExtractor("<html></html>").isTokenPage());
    }
    
    @Test
    public void testThatPageWithoutTokenDoesNotMatch() {
        assertFalse(new TokenExtractor("<h2>Your API token is</h2><a href=\"request\">Request another token</a>").isTokenPage());
    }
    
    @Test
    public void testThatPageWithTokenDoesMatch() {
        TokenExtractor extractor = new TokenExtractor("" +
                "<style>" +
                "    body     { font-family: sans-serif; font-size: 14px; margin: 2em 2%; background-color: #F9F9F9; }" +
                "    h2       { font-size: 1.4em;}" +
                "    h3       { font-size: 1em; margin: 1.5em 0 0; }" +
                "    code,pre { font-family: Menlo, Monaco, Consolas, monospace; }" +
                "    code     { font-weight: 300; font-size: 1.5em; margin-bottom: 1em; display: inline-block;  color: #646464;  }" +
                "    pre      { padding-left: 1em; border-radius: 5px; color: #003d6e; background-color: #EAEDF0; padding: 1.5em 0 1.5em 4.5em; white-space: normal; text-indent: -2em; }" +
                "    a        { color: #00f; text-decoration: none; }" +
                "    a:hover  { text-decoration: underline; }" +
                "    @media (min-width: 768px) {" +
                "        .nowrap { white-space: nowrap; }" +
                "    }" +
                "</style>" +
                "  <h2>Your API token is</h2>" +
                "  <code>mytoken</code>" +

                "  <h2>Log in with this token</h2>" +
                "  <pre>oc login <span class=\"nowrap\">--token=mytoken</span> <span class=\"nowrap\">--server=https://api.engint.openshift.com</span></pre>" +
                "  <h3>Use this token directly against the API</h3>" +
                "  <pre>curl <span class= \"nowrap\">-H \"Authorization: Bearer mytoken\"</span> <span class=\"nowrap\">\"https://api.engint.openshift.com/oapi/v1/users/~\"</span></pre>" +
                "<br><br>" +
                "<a href=\"request\">Request another token</a>");
        assertTrue(extractor.isTokenPage());
        assertEquals("mytoken", extractor.getToken());
    }
    
    @Test
    public void testThatPageWithTokenAndCRLFDoesMatch() {
        TokenExtractor extractor = new TokenExtractor("\n" +
                "<style>\n" +
                "    body     { font-family: sans-serif; font-size: 14px; margin: 2em 2%; background-color: #F9F9F9; }\n" +
                "    h2       { font-size: 1.4em;}\n" +
                "    h3       { font-size: 1em; margin: 1.5em 0 0; }\n" +
                "    code,pre { font-family: Menlo, Monaco, Consolas, monospace; }\n" +
                "    code     { font-weight: 300; font-size: 1.5em; margin-bottom: 1em; display: inline-block;  color: #646464;  }\n" +
                "    pre      { padding-left: 1em; border-radius: 5px; color: #003d6e; background-color: #EAEDF0; padding: 1.5em 0 1.5em 4.5em; white-space: normal; text-indent: -2em; }\n" +
                "    a        { color: #00f; text-decoration: none; }\n" +
                "    a:hover  { text-decoration: underline; }\n" +
                "    @media (min-width: 768px) {\n" +
                "        .nowrap { white-space: nowrap; }\n" +
                "    }\n" +
                "</style>\n" +
                "  <h2>Your API token is</h2>\n" +
                "  <code>mytoken</code>\n" +

                "  <h2>Log in with this token</h2>\n" +
                "  <pre>oc login <span class=\"nowrap\">--token=mytoken</span> <span class=\"nowrap\">--server=https://api.engint.openshift.com</span></pre>\n" +
                "  <h3>Use this token directly against the API</h3>\n" +
                "  <pre>curl <span class= \"nowrap\">-H \"Authorization: Bearer mytoken\"</span> <span class=\"nowrap\">\"https://api.engint.openshift.com/oapi/v1/users/~\"</span></pre>\n" +
                "<br><br>\n" +
                "<a href=\"request\">Request another token</a>");
        assertTrue(extractor.isTokenPage());
        assertEquals("mytoken", extractor.getToken());
    }
}
