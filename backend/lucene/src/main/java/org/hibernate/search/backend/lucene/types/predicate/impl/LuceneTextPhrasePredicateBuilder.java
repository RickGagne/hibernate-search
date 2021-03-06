/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneTextFieldCodec;
import org.hibernate.search.backend.lucene.util.impl.AnalyzerUtils;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.QueryBuilder;

class LuceneTextPhrasePredicateBuilder extends AbstractLuceneSearchPredicateBuilder
		implements PhrasePredicateBuilder<LuceneSearchPredicateBuilder> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final String absoluteFieldPath;
	protected final LuceneTextFieldCodec<?> codec;

	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private int slop;
	private String phrase;
	private Analyzer analyzer;

	LuceneTextPhrasePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath,
			LuceneTextFieldCodec<?> codec,
			Analyzer analyzerOrNormalizer) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		this.analyzer = analyzerOrNormalizer;
		this.analysisDefinitionRegistry = searchContext.getAnalysisDefinitionRegistry();
	}

	@Override
	public void slop(int slop) {
		this.slop = slop;
	}

	@Override
	public void phrase(String phrase) {
		this.phrase = phrase;
	}

	@Override
	public void analyzer(String analyzerName) {
		this.analyzer = analysisDefinitionRegistry.getAnalyzerDefinition( analyzerName );
		if ( analyzer == null ) {
			throw log.unknownAnalyzer( analyzerName, EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	@Override
	public void skipAnalysis() {
		this.analyzer = AnalyzerUtils.KEYWORD_ANALYZER;
	}

	@Override
	protected Query doBuild(LuceneSearchPredicateContext context) {
		if ( analyzer != null ) {
			Query analyzed = new QueryBuilder( analyzer ).createPhraseQuery( absoluteFieldPath, phrase, slop );
			if ( analyzed == null ) {
				// Either the value was an empty string
				// or the analysis removed all tokens (that can happen if the value contained only stopwords, for example)
				// In any case, use the same behavior as Elasticsearch: don't match anything
				analyzed = new MatchNoDocsQuery( "No tokens after analysis of the phrase to match" );
			}
			return analyzed;
		}
		else {
			// we are in the case where we a have a normalizer here as the analyzer case has already been treated by
			// the queryBuilder case above

			return new TermQuery( new Term( absoluteFieldPath, codec.normalize( absoluteFieldPath, phrase ) ) );
		}
	}
}
