/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateFieldSetContext;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class MatchPredicateFieldSetContextImpl<B>
		implements MatchPredicateFieldSetContext, AbstractBooleanMultiFieldPredicateCommonState.FieldSetContext<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final CommonState<B> commonState;

	private final List<String> absoluteFieldPaths;
	private final List<MatchPredicateBuilder<B>> predicateBuilders = new ArrayList<>();

	private Float fieldSetBoost;

	MatchPredicateFieldSetContextImpl(CommonState<B> commonState, List<String> absoluteFieldPaths) {
		this.commonState = commonState;
		this.commonState.add( this );
		this.absoluteFieldPaths = absoluteFieldPaths;
		SearchPredicateBuilderFactory<?, B> predicateFactory = commonState.getFactory();
		for ( String absoluteFieldPath : absoluteFieldPaths ) {
			predicateBuilders.add( predicateFactory.match( absoluteFieldPath ) );
		}
	}

	@Override
	public MatchPredicateFieldSetContext orFields(String... absoluteFieldPaths) {
		return new MatchPredicateFieldSetContextImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}

	@Override
	public MatchPredicateFieldSetContext boostedTo(float boost) {
		this.fieldSetBoost = boost;
		return this;
	}

	@Override
	public MatchPredicateTerminalContext matching(Object value, DslConverter dslConverter) {
		return commonState.matching( value, dslConverter );
	}

	@Override
	public List<String> getAbsoluteFieldPaths() {
		return absoluteFieldPaths;
	}

	@Override
	public void contributePredicateBuilders(Consumer<B> collector) {
		for ( MatchPredicateBuilder<B> predicateBuilder : predicateBuilders ) {
			// Perform last-minute changes, since it's the last call that will be made on this field set context
			commonState.applyBoostAndConstantScore( fieldSetBoost, predicateBuilder );

			collector.accept( predicateBuilder.toImplementation() );
		}
	}

	static class CommonState<B> extends AbstractBooleanMultiFieldPredicateCommonState<CommonState<B>, B, MatchPredicateFieldSetContextImpl<B>>
			implements MatchPredicateTerminalContext {

		CommonState(SearchPredicateBuilderFactory<?, B> factory) {
			super( factory );
		}

		MatchPredicateTerminalContext matching(Object value, DslConverter dslConverter) {
			if ( value == null ) {
				throw log.matchPredicateCannotMatchNullValue( getEventContext() );
			}

			for ( MatchPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.value( value, dslConverter );
				}
			}
			return this;
		}

		@Override
		public MatchPredicateTerminalContext fuzzy(int maxEditDistance, int exactPrefixLength) {
			if ( maxEditDistance < 0 || 2 < maxEditDistance ) {
				throw log.invalidFuzzyMaximumEditDistance( maxEditDistance );
			}
			if ( exactPrefixLength < 0 ) {
				throw log.invalidExactPrefixLength( exactPrefixLength );
			}

			for ( MatchPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.fuzzy( maxEditDistance, exactPrefixLength );
				}
			}
			return this;
		}

		@Override
		public MatchPredicateTerminalContext analyzer(String analyzerName) {
			for ( MatchPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.analyzer( analyzerName );
				}
			}
			return this;
		}

		@Override
		public MatchPredicateTerminalContext skipAnalysis() {
			for ( MatchPredicateFieldSetContextImpl<B> fieldSetContext : getFieldSetContexts() ) {
				for ( MatchPredicateBuilder<B> predicateBuilder : fieldSetContext.predicateBuilders ) {
					predicateBuilder.skipAnalysis();
				}
			}
			return this;
		}

		@Override
		protected CommonState<B> thisAsS() {
			return this;
		}
	}

}
