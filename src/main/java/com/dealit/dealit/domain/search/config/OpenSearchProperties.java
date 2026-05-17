package com.dealit.dealit.domain.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.search.opensearch")
public class OpenSearchProperties {

	private boolean enabled;
	private boolean autoReindexOnStartup;
	private String uri = "http://localhost:9200";
	private String indexName = "dealit-search";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isAutoReindexOnStartup() {
		return autoReindexOnStartup;
	}

	public void setAutoReindexOnStartup(boolean autoReindexOnStartup) {
		this.autoReindexOnStartup = autoReindexOnStartup;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
}
