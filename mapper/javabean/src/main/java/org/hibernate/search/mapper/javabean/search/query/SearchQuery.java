/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.query;

/**
 * @param <T> The type of results.
 */
public interface SearchQuery<T> {

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	default SearchResult<T> fetch() {
		return fetch( (Long) null, null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	default SearchResult<T> fetch(long limit) {
		return fetch( limit, null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	default SearchResult<T> fetch(int limit) {
		return fetch( (long) limit, null );
	}

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @param offset The number of hits to skip before adding the hits to the {@link SearchResult}. {@code null} means no offset.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	SearchResult<T> fetch(Long limit, Long offset);

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @param limit The maximum number of hits to be included in the {@link SearchResult}. {@code null} means no limit.
	 * @param offset The number of hits to skip before adding the hits to the {@link SearchResult}. {@code null} means no offset.
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	default SearchResult<T> fetch(Integer limit, Integer offset) {
		return fetch( limit == null ? null : (long) limit, offset == null ? null : (long) offset );
	}

	/**
	 * Execute the query and return the total hit count.
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long fetchTotalHitCount();

	/**
	 * @return A textual representation of the query.
	 */
	String getQueryString();

}
