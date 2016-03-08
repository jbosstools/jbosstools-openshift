package org.jboss.tools.openshift.internal.ui.wizard.deployimage.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.linuxtools.docker.core.IRepositoryTag;
import org.jboss.dmr.ModelNode;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;

/**
 * The default implementation is the Docker Hub registry (running Docker
 * registry version 0.6.3)
 */
@SuppressWarnings("restriction")
public class DockerHubRegistry {

	private static final String REGISTRY_LOCATION = "https://registry.hub.docker.com/"; //$NON-NLS-1$

	/**
	 * Searches for the tags of the given image on the Docker registry.
	 * @param repoName the repository/name of the image to search
	 * @return the list of tags for the given repository, or empty list if none was found
	 */
	public List<String> getTags(final String repoName) {
		final HttpClient httpClient = HttpClientBuilder.create().build();
		try {
			// check that the registry supports the version 1 API
			// see
			// https://github.com/docker/docker-registry/blob/master/docker_registry/app.py
			final HttpGet pingApiV1Request = new HttpGet(
					URIBuilder.target(REGISTRY_LOCATION).path("v1").path("_ping").toString());
			pingApiV1Request.setHeader("Accept", "application/json");
			final HttpResponse pingApiV1Response = httpClient.execute(pingApiV1Request);
			if (pingApiV1Response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new InvalidDockerRegistryException(REGISTRY_LOCATION);
			}
			// now perform the call to retrieve the list of tags for the given
			// repo/name
			final HttpGet getTagsRequest = new HttpGet(URIBuilder.target(REGISTRY_LOCATION).path("v1")
					.path("repositories").path(repoName).path("tags").toString());
			getTagsRequest.setHeader("Accept", "application/json");
			final HttpResponse getTagsResponse = httpClient.execute(getTagsRequest);
			if (getTagsResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return Collections.emptyList();
			}
			final ModelNode tags = ModelNode.fromJSONStream(getTagsResponse.getEntity().getContent());
			return tags.asList().stream().map(node -> node.get("name").asString()).collect(Collectors.toList());
		} catch (final IOException e) {
			OpenShiftUIActivator.getDefault().getLogger().logError("Failed to retrieve the tags for image named '" + repoName + "'", e);
		}
		return Collections.emptyList();
	}
	
	/**
	 * URI Builder
	 */
	private static class URIBuilder {
		
		private final StringBuilder uri; 
		private URIBuilder(final String target) {
			this.uri = new StringBuilder(target);
		}
		public static URIBuilder target(final String target) {
			return new URIBuilder(target);
		}
		
		/**
		 * Appends the given {@code path} to the current URI. 
		 * @param path the path to append
		 * @return this {@link URIBuilder}, for fluent chaining with other paths.
		 */
		public URIBuilder path(final String path) {
			if(path.charAt(0) != '/') {
				this.uri.append('/');
			}
			this.uri.append(path);
			return this;
		}
		
		@Override
		public String toString() {
			return this.uri.toString();
		}
		
	}
	
	/**
	 * Repository tag retrieved from Docker Registry version 0.6.3
	 *
	 */
	@Deprecated
	public class RepositoryTag implements IRepositoryTag {

		private String layer;

		private String name;

		/**
		 * @return the layer
		 */
		@Override
		public String getLayer() {
			return layer;
		}

		/**
		 * @param layer
		 *            the layer to set
		 */
		public void setLayer(String layer) {
			this.layer = layer;
		}

		/**
		 * @return the name
		 */
		@Override
		public String getName() {
			return name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

	}


}
