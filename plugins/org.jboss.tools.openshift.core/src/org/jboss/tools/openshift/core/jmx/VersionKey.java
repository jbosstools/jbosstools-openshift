package org.jboss.tools.openshift.core.jmx;

public final class VersionKey {
	public VersionKey(ServerType serverType, String version) {
		this.serverType = serverType;
		this.version = version;
	}

	private ServerType serverType;
	private String version;
	
	public ServerType getServerType() {
		return serverType;
	}
	
	public String getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != getClass()) {
			return false;
		}
		VersionKey other = (VersionKey) o;
		return serverType.equals(other.serverType) && version.equals(other.version);
	}

	@Override
	public int hashCode() {
		return serverType.hashCode() ^ version.hashCode();
	}
}