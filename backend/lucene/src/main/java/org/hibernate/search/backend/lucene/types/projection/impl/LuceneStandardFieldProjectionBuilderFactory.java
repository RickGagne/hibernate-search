/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneFieldProjectionBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <F> The field type exposed to the mapper.
 * @see LuceneFieldCodec
 */
public class LuceneStandardFieldProjectionBuilderFactory<F> implements LuceneFieldProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean projectable;

	private final FromDocumentFieldValueConverter<? super F, ?> converter;
	private final FromDocumentFieldValueConverter<? super F, F> rawConverter;

	private final LuceneFieldCodec<F> codec;

	public LuceneStandardFieldProjectionBuilderFactory(boolean projectable,
			FromDocumentFieldValueConverter<? super F, ?> converter, FromDocumentFieldValueConverter<? super F, F> rawConverter,
			LuceneFieldCodec<F> codec) {
		this.projectable = projectable;
		this.converter = converter;
		this.rawConverter = rawConverter;
		this.codec = codec;
	}

	@Override
	@SuppressWarnings("unchecked") // We check the cast is legal by asking the converter
	public <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(String absoluteFieldPath,
			Class<T> expectedType, ProjectionConverter projectionConverter) {
		checkProjectable( absoluteFieldPath, projectable );

		FromDocumentFieldValueConverter<? super F, ?> requestConverter = getConverter( projectionConverter );
		if ( !requestConverter.isConvertedTypeAssignableTo( expectedType ) ) {
			throw log.invalidProjectionInvalidType( absoluteFieldPath, expectedType,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}

		return (FieldProjectionBuilder<T>) new LuceneFieldProjectionBuilder<>( absoluteFieldPath, requestConverter, codec );
	}

	@Override
	public DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(String absoluteFieldPath,
			GeoPoint center) {
		throw log.distanceOperationsNotSupportedByFieldType(
				EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	@Override
	public boolean hasCompatibleCodec(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneStandardFieldProjectionBuilderFactory<?> castedOther =
				(LuceneStandardFieldProjectionBuilderFactory<?>) other;
		return projectable == castedOther.projectable && codec.isCompatibleWith( castedOther.codec );
	}

	@Override
	public boolean hasCompatibleConverter(LuceneFieldProjectionBuilderFactory other) {
		if ( !getClass().equals( other.getClass() ) ) {
			return false;
		}
		LuceneStandardFieldProjectionBuilderFactory<?> castedOther =
				(LuceneStandardFieldProjectionBuilderFactory<?>) other;
		return converter.isCompatibleWith( castedOther.converter );
	}

	private static void checkProjectable(String absoluteFieldPath, boolean projectable) {
		if ( !projectable ) {
			throw log.nonProjectableField( absoluteFieldPath,
					EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
		}
	}

	private FromDocumentFieldValueConverter<? super F, ?> getConverter(ProjectionConverter projectionConverter) {
		return ( projectionConverter.isEnabled() ) ? converter : rawConverter;
	}
}
