/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDate;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class LocalDateFieldTypeDescriptor extends FieldTypeDescriptor<LocalDate> {

	LocalDateFieldTypeDescriptor() {
		super( LocalDate.class );
	}

	@Override
	public Optional<IndexingExpectations<LocalDate>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				LocalDate.of( 1970, 1, 1 ),
				LocalDate.of( 1980, 1, 1 ),
				LocalDate.of( 2017, 7, 7 ),
				LocalDate.of( 1980, 12, 31 ),
				LocalDate.of( 2004, 2, 29 ),
				LocalDate.of( 1900, 1, 1 ),
				LocalDate.of( 1600, 2, 28 ),
				LocalDate.of( -52, 10, 11 ),
				LocalDate.of( 22500, 10, 11 )
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<LocalDate>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDate.of( 1980, 10, 11 ),
				LocalDate.of( 1984, 10, 7 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<LocalDate>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDate.of( 2018, 2, 1 ),
				LocalDate.of( 2018, 3, 1 ),
				LocalDate.of( 2018, 4, 1 ),
				// Values around what is indexed
				LocalDate.of( 2018, 2, 15 ),
				LocalDate.of( 2018, 3, 15 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<LocalDate> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalDate.of( 1970, 1, 1 ),
				LocalDate.of( 1984, 10, 7 )
		);
	}

	@Override
	public Optional<FieldSortExpectations<LocalDate>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalDate.of( 2018, 2, 1 ),
				LocalDate.of( 2018, 3, 1 ),
				LocalDate.of( 2018, 4, 1 ),
				// Values around what is indexed
				LocalDate.of( 2018, 1, 1 ),
				LocalDate.of( 2018, 2, 15 ),
				LocalDate.of( 2018, 3, 15 ),
				LocalDate.of( 2018, 5, 1 )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<LocalDate>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalDate.of( 2018, 2, 1 ),
				LocalDate.of( 2018, 3, 1 ),
				LocalDate.of( 2018, 4, 1 )
		) );
	}
}
