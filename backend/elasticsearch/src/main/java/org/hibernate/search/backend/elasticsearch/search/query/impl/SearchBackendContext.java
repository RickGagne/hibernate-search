/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public class SearchBackendContext {
	private final EventContext eventContext;

	private final ElasticsearchWorkBuilderFactory workFactory;
	private final ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory;
	private final Gson userFacingGson;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchWorkOrchestrator orchestrator;

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	private final DocumentReferenceExtractorHelper documentReferenceExtractorHelper;

	public SearchBackendContext(EventContext eventContext,
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			Gson userFacingGson,
			Function<String, String> indexNameConverter,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestrator orchestrator) {
		this.eventContext = eventContext;
		this.workFactory = workFactory;
		this.searchResultExtractorFactory = searchResultExtractorFactory;
		this.userFacingGson = userFacingGson;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;

		this.documentReferenceExtractorHelper =
				new DocumentReferenceExtractorHelper( indexNameConverter, multiTenancyStrategy );

		this.searchProjectionBackendContext = new SearchProjectionBackendContext(
				documentReferenceExtractorHelper,
				userFacingGson
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	DocumentReferenceExtractorHelper getDocumentReferenceExtractorHelper() {
		return documentReferenceExtractorHelper;
	}

	SearchProjectionBackendContext getSearchProjectionBackendContext() {
		return searchProjectionBackendContext;
	}

	ElasticsearchSearchContext createSearchContext(MappingContextImplementor mappingContext) {
		return new ElasticsearchSearchContext( mappingContext, userFacingGson, multiTenancyStrategy );
	}

	<T> ElasticsearchSearchQueryBuilder<T> createSearchQueryBuilder(
			Set<URLEncodedString> indexNames,
			SessionContextImplementor sessionContext,
			ProjectionHitMapper<?, ?> projectionHitMapper,
			ElasticsearchSearchProjection<?, T> rootProjection) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );
		return new ElasticsearchSearchQueryBuilder<>(
				workFactory, searchResultExtractorFactory, orchestrator, multiTenancyStrategy,
				indexNames, sessionContext, projectionHitMapper, rootProjection
		);
	}

}
