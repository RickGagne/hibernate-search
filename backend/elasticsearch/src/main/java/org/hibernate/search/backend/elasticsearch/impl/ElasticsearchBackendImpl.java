/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSharedWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorProvider;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchResultExtractorFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryContextProvider;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.dsl.impl.ElasticsearchIndexSchemaRootNodeBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.index.impl.IndexingBackendContext;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.spi.LogErrorHandler;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchBackendImpl implements BackendImplementor<ElasticsearchDocumentObjectBuilder>,
		ElasticsearchBackend {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchClientProvider clientProvider;

	private final String name;
	private final ElasticsearchWorkOrchestratorProvider orchestratorProvider;

	private final ElasticsearchIndexFieldTypeFactoryContextProvider typeFactoryContextProvider;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final MultiTenancyStrategy multiTenancyStrategy;

	private final ElasticsearchSharedWorkOrchestrator queryOrchestrator;

	private final Map<String, String> hibernateSearchIndexNamesByElasticsearchIndexNames = new ConcurrentHashMap<>();

	private final EventContext eventContext;
	private final IndexingBackendContext indexingContext;
	private final SearchBackendContext searchContext;

	ElasticsearchBackendImpl(ElasticsearchClientProvider clientProvider,
			GsonProvider dialectSpecificGsonProvider, String name,
			ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchIndexFieldTypeFactoryContextProvider typeFactoryContextProvider,
			ElasticsearchSearchResultExtractorFactory searchResultExtractorFactory,
			Gson userFacingGson,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			MultiTenancyStrategy multiTenancyStrategy) {
		this.clientProvider = clientProvider;
		this.name = name;

		this.orchestratorProvider = new ElasticsearchWorkOrchestratorProvider(
				"Elasticsearch parallel work orchestrator for backend " + name,
				clientProvider, dialectSpecificGsonProvider, workFactory,
				// TODO the LogErrorHandler should be replaced with a user-configurable instance at some point. See HSEARCH-3110.
				new LogErrorHandler()
		);
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.queryOrchestrator = orchestratorProvider.createParallelOrchestrator( "Elasticsearch query orchestrator for backend " + name );

		this.typeFactoryContextProvider = typeFactoryContextProvider;

		this.eventContext = EventContexts.fromBackendName( name );
		this.indexingContext = new IndexingBackendContext( eventContext, workFactory, multiTenancyStrategy,
				orchestratorProvider
		);
		this.searchContext = new SearchBackendContext(
				eventContext, workFactory, searchResultExtractorFactory, userFacingGson,
				( String elasticsearchIndexName ) -> {
					String result = hibernateSearchIndexNamesByElasticsearchIndexNames.get( elasticsearchIndexName );
					if ( result == null ) {
						throw log.elasticsearchResponseUnknownIndexName( elasticsearchIndexName, eventContext );
					}
					return result;
				},
				multiTenancyStrategy, queryOrchestrator
		);
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( name )
				.append( "]" )
				.toString();
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ElasticsearchWorkOrchestrator::close, queryOrchestrator );
			closer.push( ElasticsearchWorkOrchestratorProvider::close, orchestratorProvider );
			// Close the client after the orchestrators, when we're sure all works have been performed
			closer.push( ElasticsearchClientProvider::onStop, clientProvider );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToShutdownBackend( e, eventContext );
		}
	}

	@Override
	public void start(BackendStartContext context) {
		clientProvider.onStart( context.getConfigurationPropertySource() );
		orchestratorProvider.start();
		queryOrchestrator.start();
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchBackend.class ) ) {
			return (T) this;
		}
		throw log.backendUnwrappingWithUnknownType( clazz, ElasticsearchBackend.class, eventContext );
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	@Override
	public <T> T getClient(Class<T> clientClass) {
		return clientProvider.get().unwrap( clientClass );
	}

	@Override
	public IndexManagerBuilder<ElasticsearchDocumentObjectBuilder> createIndexManagerBuilder(
			String hibernateSearchIndexName, boolean multiTenancyEnabled, BackendBuildContext buildContext, ConfigurationPropertySource propertySource) {
		if ( multiTenancyEnabled && !multiTenancyStrategy.isMultiTenancySupported() ) {
			throw log.multiTenancyRequiredButNotSupportedByBackend( hibernateSearchIndexName, eventContext );
		}

		String elasticsearchIndexName = ElasticsearchIndexNameNormalizer.normalize( hibernateSearchIndexName );
		String existingHibernateSearchIndexName = hibernateSearchIndexNamesByElasticsearchIndexNames.putIfAbsent(
				elasticsearchIndexName, hibernateSearchIndexName
		);
		if ( existingHibernateSearchIndexName != null ) {
			throw log.duplicateNormalizedIndexNames(
					existingHibernateSearchIndexName, hibernateSearchIndexName, elasticsearchIndexName,
					eventContext
			);
		}

		EventContext indexEventContext = EventContexts.fromIndexName( hibernateSearchIndexName );

		ElasticsearchIndexFieldTypeFactoryContext typeFactoryContext =
				typeFactoryContextProvider.create( indexEventContext );

		ElasticsearchIndexSchemaRootNodeBuilder indexSchemaRootNodeBuilder =
				new ElasticsearchIndexSchemaRootNodeBuilder(
						indexEventContext,
						multiTenancyStrategy,
						typeFactoryContext
				);

		ElasticsearchIndexSettingsBuilder settingsBuilder =
				new ElasticsearchIndexSettingsBuilder( analysisDefinitionRegistry );

		return new ElasticsearchIndexManagerBuilder(
				indexingContext, searchContext,
				hibernateSearchIndexName, elasticsearchIndexName,
				indexSchemaRootNodeBuilder, settingsBuilder
		);
	}
}
