package com.dealit.dealit.domain.search.service;

import com.dealit.dealit.domain.search.config.OpenSearchProperties;
import com.dealit.dealit.domain.search.dto.SearchReindexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexStartupRunner implements ApplicationRunner {

	private final OpenSearchProperties properties;
	private final SearchService searchService;

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isEnabled() || !properties.isAutoReindexOnStartup()) {
			return;
		}
		try {
			SearchReindexResponse response = searchService.rebuildIndex();
			log.info(
				"OpenSearch index rebuilt on startup. indexName={}, indexedCount={}",
				response.indexName(),
				response.indexedCount()
			);
		} catch (RuntimeException exception) {
			log.warn("Failed to rebuild OpenSearch index on startup.", exception);
		}
	}
}
