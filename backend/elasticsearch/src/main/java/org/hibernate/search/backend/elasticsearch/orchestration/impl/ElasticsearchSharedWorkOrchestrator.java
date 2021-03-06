/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

/**
 * An {@link ElasticsearchWorkOrchestrator} that is designed to be shared among multiple threads.
 */
public interface ElasticsearchSharedWorkOrchestrator extends ElasticsearchWorkOrchestrator {

	/**
	 * Start any resource necessary to operate the orchestrator at runtime.
	 * <p>
	 * Called by the owner of this orchestrator once after bootstrap,
	 * before any other method is called.
	 */
	void start();

	/**
	 * Block until there is no more work to execute.
	 * <p>
	 * N.B. if more works are submitted in the meantime, this might delay the wait.
	 *
	 * @throws InterruptedException if thread interrupted while waiting
	 */
	void awaitCompletion() throws InterruptedException;

}
