/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. Distributed under license by Red Hat, Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.openshift.test.core.connection;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionSerializer;
import org.junit.Before;
import org.junit.Test;

import com.openshift.client.OpenShiftException;

public class ConnectionSerializerTest {

	private ConnectionSerializer serializer;
	private final String CONN_STRING = "{\"url\" : \"https://localhost:8442\", \"username\" : \"foo\", \"token\" : \"abcdef\"}";
	private Connection conn;
	
	@Before
	public void setup() throws Exception{
		serializer = new ConnectionSerializer();
		conn = new Connection("https://localhost:8442", null);
		conn.setUsername("foo");
		conn.setToken("abcdef");
	}
	
	@Test
	public void testSerializeConnection() throws Exception{
		ByteArrayOutputStream os = new ByteArrayOutputStream(CONN_STRING.length());
		serializer.serialize(conn, os);
		assertEquals("Exp. the connection to be serialized", CONN_STRING, os.toString());
	}

	@Test(expected = OpenShiftException.class)
	public void testSerializeConnectionWithNullUser(){
		ByteArrayOutputStream os = new ByteArrayOutputStream(CONN_STRING.length());
		conn.setUsername(null);
		serializer.serialize(conn, os);
	}
	@Test(expected = OpenShiftException.class)
	public void testSerializeConnectionWithEmptyUser(){
		ByteArrayOutputStream os = new ByteArrayOutputStream(CONN_STRING.length());
		conn.setUsername("");
		serializer.serialize(conn, os);
	}

	@Test(expected = OpenShiftException.class)
	public void testSerializeConnectionWithBlankUser(){
		ByteArrayOutputStream os = new ByteArrayOutputStream(CONN_STRING.length());
		conn.setUsername(" ");
		serializer.serialize(conn, os);
	}

	@Test
	public void testDeserializeConnection() {
		ByteArrayInputStream is = new ByteArrayInputStream(CONN_STRING.getBytes());
		Connection actConnection = serializer.deserialize(is);
		assertEquals("Exp. the connections to be the same", conn, actConnection);
		assertEquals(conn.getToken(), actConnection.getToken());
	}

}
