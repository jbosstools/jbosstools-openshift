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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IStatus;
import org.jboss.tools.foundation.core.properties.PropertiesHelper;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * @author Red Hat Developers
 *
 */
public class SandboxProcessor {

	/**
	 * 
	 */
	private static final String SANDBOX_API_ENDPOINT_DEFAULT = "https://registration-service-toolchain-host-operator.apps.sandbox.x8i5.p1.openshiftapps.com";

	private static final String SANDBOX_API_ENDPOINT_PROPERTY_KEY = "jboss.sandbox.api.endpoint";

	private static final String SANDBOX_API_ENDPOINT = PropertiesHelper.getPropertiesProvider()
			.getValue(SANDBOX_API_ENDPOINT_PROPERTY_KEY, SANDBOX_API_ENDPOINT_DEFAULT);

	public enum State {
		NONE(false), NEEDS_SIGNUP(false), NEEDS_VERIFICATION(true), CONFIRM_VERIFICATION(true), NEEDS_APPROVAL(false),
		READY(true);

		private boolean needsInteraction;

		private State(boolean needsInteraction) {
			this.needsInteraction = needsInteraction;
		}

		/**
		 * @return the needsInteraction
		 */
		public boolean isNeedsInteraction() {
			return needsInteraction;
		}
	}

	private SandboxAPI api;

	private String token;

	private State state = State.NONE;

	private ObjectNode signupPayload;

	/**
	 * 
	 */
	public SandboxProcessor(String token, String url) {
		this.token = "Bearer " + token;
		OkHttpClient client = new OkHttpClient.Builder().callTimeout(1, TimeUnit.MINUTES)
				.readTimeout(30, TimeUnit.SECONDS).build();
		this.api = new Retrofit.Builder().baseUrl(url).addConverterFactory(JacksonConverterFactory.create())
				.client(client).build().create(SandboxAPI.class);
	}

	public SandboxProcessor(String token) {
		this(token, SANDBOX_API_ENDPOINT);
	}

	public State getState() {
		return state;
	}

	private void getSignupState() throws IOException {
		Response<ObjectNode> response = api.signupState(token).execute();
		if (response.code() == 404) {
			state = State.NEEDS_SIGNUP;
		} else if (response.code() == 200) {
			signupPayload = response.body();
			JsonNode status = signupPayload.get("status");
			if (status != null) {
				boolean ready = status.get("ready").asBoolean(false);
				if (ready) {
					state = State.READY;
				} else {
					boolean needsVerification = status.get("verificationRequired").asBoolean();
					if (needsVerification) {
						state = State.NEEDS_VERIFICATION;
					} else {
						state = State.NEEDS_APPROVAL;
					}
				}
			}
		} else {
			throw new IOException("Sandbox API returned code: " + response.code());
		}
	}

	private void processSignup() throws IOException {
		api.signup(token).execute();
		getSignupState();
	}

	private void startVerification(String countryCode, String phoneNumber) throws IOException {
		ObjectNode body = JsonNodeFactory.instance.objectNode();
		body.put("country_code", countryCode);
		body.put("phone_number", phoneNumber);
		Response<ResponseBody> response = api.verify(token, body).execute();
		if (response.code() != 204) {
			throw new IOException("Start verification returned invalid status code:" + response.code());
		}
		state = State.CONFIRM_VERIFICATION;
	}

	private void processVerification(String confirmCode) throws IOException {
		api.completeVerify(token, confirmCode).execute();
		getSignupState();
	}

	public State advance(String countryCode, String phoneNumber, String confirmCode) throws IOException {
		switch (state) {
		case NONE:
		case NEEDS_APPROVAL:
			getSignupState();
			break;
		case NEEDS_SIGNUP:
			processSignup();
			break;
		case NEEDS_VERIFICATION:
			startVerification(countryCode, phoneNumber);
			break;
		case CONFIRM_VERIFICATION:
			processVerification(confirmCode);
			break;
		}
		return state;
	}

	/**
	 * @return the signupPayload
	 */
	public String getClusterURL() {
		if (signupPayload != null) {
			if (signupPayload.has("apiEndpoint")) {
				return signupPayload.get("apiEndpoint").asText();
			}
			return patch(signupPayload.get("consoleURL").asText());
		}
		throw new IllegalStateException();
	}

	/**
	 * @param asText
	 * @return
	 */
	private String patch(String consoleURL) {
		URL url;
		try {
			url = new URL(consoleURL);
			String host = url.getHost();
			int index = host.indexOf('.');
			index = host.indexOf('.', index + 1);
			host = host.substring(index + 1);
			return "https://api." + host + ":6443";
		} catch (MalformedURLException e) {
			OpenShiftUIActivator.log(IStatus.WARNING, e.getLocalizedMessage(), e);
			return consoleURL;
		}
	}
}
