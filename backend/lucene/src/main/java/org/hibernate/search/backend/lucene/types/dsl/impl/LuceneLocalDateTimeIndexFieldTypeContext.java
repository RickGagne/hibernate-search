/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDateTime;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateTimeFieldCodec;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneNumericFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;

class LuceneLocalDateTimeIndexFieldTypeContext
		extends AbstractLuceneStandardIndexFieldTypeContext<LuceneLocalDateTimeIndexFieldTypeContext, LocalDateTime> {

	private Sortable sortable = Sortable.DEFAULT;

	LuceneLocalDateTimeIndexFieldTypeContext(LuceneIndexFieldTypeBuildContext buildContext) {
		super( buildContext, LocalDateTime.class );
	}

	@Override
	public LuceneLocalDateTimeIndexFieldTypeContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	public LuceneIndexFieldType<LocalDateTime> toIndexFieldType() {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		ToDocumentFieldValueConverter<?, ? extends LocalDateTime> dslToIndexConverter =
				createDslToIndexConverter();
		FromDocumentFieldValueConverter<? super LocalDateTime, ?> indexToProjectionConverter =
				createIndexToProjectionConverter();
		LuceneLocalDateTimeFieldCodec codec = new LuceneLocalDateTimeFieldCodec( resolvedProjectable, resolvedSortable );

		return new LuceneIndexFieldType<>(
				codec,
				new LuceneNumericFieldPredicateBuilderFactory<>( dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneNumericFieldSortBuilderFactory<>( resolvedSortable, dslToIndexConverter, createToDocumentRawConverter(), codec ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, indexToProjectionConverter, createFromDocumentRawConverter(), codec )
		);
	}

	@Override
	protected LuceneLocalDateTimeIndexFieldTypeContext thisAsS() {
		return this;
	}
}
