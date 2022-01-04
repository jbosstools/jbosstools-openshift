/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.test.ui.wizard.applicationexplorer.sandbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox.SandboxProcessor;
import org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer.sandbox.SandboxProcessor.State;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

/**
 * @author Jeff MAURY
 *
 */
public class SandboxProcessTest {
	
	private MockServerClient mockServer;
	
	@Before
	public void init() {
		mockServer = ClientAndServer.startClientAndServer(0);
	}
	
	@After
	public void shutdown() {
		mockServer.stop();
	}
	
	@Test
	public void checkProcessorIsInitialized() {
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:1080");
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
	}

	@Test
	public void checkNeedsSignupIf404() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.notFoundResponse());
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.NEEDS_SIGNUP, state);
	}
	
	@Test
	public void checkNeedsVerification() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.response("{ \"status\": { \"ready\": false, \"verificationRequired\": true}}"));
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.NEEDS_VERIFICATION, state);
	}
	
	@Test
	public void checkNeedsApproval() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.response("{ \"status\": { \"ready\": false, \"verificationRequired\": false}}"));
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.NEEDS_APPROVAL, state);
	}

	@Test
	public void checkReady() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.response("{ \"status\": { \"ready\": true}}"));
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.READY, state);
	}

	@Test
	public void checkConfirmVerification() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.response("{ \"status\": { \"ready\": false, \"verificationRequired\": true}}"));
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup/verification")
				.withMethod("PUT"))
		.respond(HttpResponse.response().withStatusCode(204));
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.NEEDS_VERIFICATION, state);
		state = processor.advance("", "", "");
		assertEquals(State.CONFIRM_VERIFICATION, state);
	}
	
	@Test
	public void checkConfirmVerificationTimeout() throws IOException {
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup")
				.withMethod("GET"))
		.respond(HttpResponse.response("{ \"status\": { \"ready\": false, \"verificationRequired\": true}}"));
		mockServer.when(HttpRequest.request()
				.withPath("/api/v1/signup/verification")
				.withMethod("PUT"))
		.respond(HttpResponse.response().withStatusCode(204).withDelay(TimeUnit.SECONDS, 45));
		SandboxProcessor processor = new SandboxProcessor("token", "http://localhost:" + mockServer.getPort());
		assertEquals(SandboxProcessor.State.NONE, processor.getState());
		State state = processor.advance("", "", "");
		assertEquals(State.NEEDS_VERIFICATION, state);
		try {
			state = processor.advance("", "", "");
			fail("should timeout");
		} catch (SocketTimeoutException e) {
		}
	}
}
