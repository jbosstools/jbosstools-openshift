/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.core.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.openshift.core.LazyCredentialsPrompter;
import org.jboss.tools.openshift.core.OpenShiftCoreUIIntegration;

import com.openshift.client.OpenShiftException;

/**
 * Utility class to serialize and deserialize a connection. The connection
 * will be represented as a json string in the format of:
 * {
 *     "url" : "https://localhost:8443",
 *     "username" : "foo",
 *     "token" : "abaceaeadafaera"
 * }
 */
public class ConnectionSerializer {
	
	private static final String URL = "url";
	private static final String TOKEN = "token";
	private static final String USERNAME = "username";

	private ConnectionFactory factory = new ConnectionFactory();
	
	/**
	 * Serialize the connection
	 * @param conn
	 * @return the serialized connection
	 * @throws OpenShiftException
	 */
	public String serialize(Connection conn){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		serialize(conn, os);
		return os.toString();
	}
	
	/**
	 * Serialize the connection
	 * @param conn
	 * @param writer
	 * @throws OpenShiftException
	 */
	public void serialize(Connection conn, OutputStream os){
		if(StringUtils.isBlank(conn.getUsername()))
			throw new OpenShiftException("Unable to serialize a connection with no associated user");
		ModelNode node = new ModelNode();
		node.get(URL).set(conn.toString());
		node.get(USERNAME).set(conn.getUsername());
		if(StringUtils.isNotBlank(conn.getToken()))
			node.get(TOKEN).set(conn.getToken());
		try {
			OutputStreamWriter writer = new OutputStreamWriter(os);
			writer.write(node.toJSONString(true));
			writer.flush();
		} catch (IOException e) {
			throw new OpenShiftException(e, "There was an error serializing the connection %s", conn);
		}
	}

	/**
	 * Deserialize the connection from the given input
	 * stream
	 * @param is
	 * @return {@link Connection}
	 * @throws OpenShiftException
	 */
	public Connection deserialize(InputStream is) {
		try {
			ModelNode node = ModelNode.fromJSONStream(is);
			Connection conn = factory.create(
					node.get(URL).asString(), 
					new LazyCredentialsPrompter(OpenShiftCoreUIIntegration.getInstance().getCredentialPrompter()));
			conn.setUsername(node.get(USERNAME).asString());
			conn.setToken(StringUtils.defaultIfBlank(node.get(TOKEN).asString(), null));
			return conn;
		} catch (IOException e) {
			throw new OpenShiftException(e, "There was an error deserializing the connection");
		}
	}

	/**
	 * Deserialize the connection from the given string
	 * @param entry
	 * @return {@link Connection}
	 * @throws OpenShiftException
	 */
	public Connection deserialize(String entry) {
		ByteArrayInputStream is = new ByteArrayInputStream(entry.getBytes());
		return deserialize(is);
	}
}
