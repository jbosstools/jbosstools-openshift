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

import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * @author Red Hat Developers
 *
 */

public interface SandboxAPI {

   @GET("api/v1/signup")
   Call<ObjectNode> signupState(@Header("Authorization") String token);

   @POST("api/v1/signup")
   Call<ResponseBody> signup(@Header("Authorization") String token);

   @PUT("api/v1/signup/verification")
   Call<ResponseBody> verify(@Header("Authorization") String token, @Body ObjectNode body);

   @GET("api/v1/signup/verification/{code}")
   Call<ResponseBody> completeVerify(@Header("Authorization") String token, @Path("code") String code);
}
