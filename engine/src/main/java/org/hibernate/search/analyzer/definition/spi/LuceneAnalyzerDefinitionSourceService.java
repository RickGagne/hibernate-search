/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.definition.spi;

import org.hibernate.search.engine.service.spi.Service;

/**
 * This service allows to inject a custom {@link org.hibernate.search.analyzer.definition.spi.LuceneAnalyzerDefinitionProvider}.
 *
 * Integrators which prefer to inject an alternative service by reference rather than setting a configuration
 * property can provide an alternative Service implementation by overriding {@link org.hibernate.search.cfg.spi.SearchConfiguration.getProvidedServices()}.
 *
 * When not injecting one, the default implementation will be used: {@link PropertiesBasedLuceneAnalyzerDefinitionSourceService}.
 *
 * @author Sanne Grinovero
 */
public interface LuceneAnalyzerDefinitionSourceService extends Service {

	LuceneAnalyzerDefinitionProvider getLuceneAnalyzerDefinitionProvider();

}