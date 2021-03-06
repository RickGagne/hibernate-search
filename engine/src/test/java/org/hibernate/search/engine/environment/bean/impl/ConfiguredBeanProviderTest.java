/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.bean.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurationContext;
import org.hibernate.search.engine.environment.bean.spi.BeanConfigurer;
import org.hibernate.search.engine.environment.bean.spi.BeanFactory;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.testsupport.util.AbstractConfigurationPropertySourcePartialMock;
import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

@SuppressWarnings({ "unchecked", "rawtypes" }) // Raw types are the only way to mock parameterized types with EasyMock
public class ConfiguredBeanProviderTest extends EasyMockSupport {

	private ClassResolver classResolverMock = createMock( ClassResolver.class );
	private BeanResolver beanResolverMock = createMock( BeanResolver.class );
	private ConfigurationPropertySource configurationSourceMock =
			partialMockBuilder( AbstractConfigurationPropertySourcePartialMock.class ).mock();

	@Test
	public void getBean_withoutBeanConfigurer() {
		// Setup
		resetAll();
		expect( classResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andStubReturn( Collections.emptyList() );
		expect( configurationSourceMock.get( EngineSpiSettings.BEAN_CONFIGURERS ) )
				.andStubReturn( Optional.empty() );
		replayAll();
		BeanProvider beanProvider =
				new ConfiguredBeanProvider( classResolverMock, beanResolverMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// getBean(Class)
		resetAll();
		expect( beanResolverMock.resolve( Type1.class ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// getBean(Class) through BeanReference
		resetAll();
		expect( beanResolverMock.resolve( Type1.class ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( BeanReference.of( Type1.class ) ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// getBean(Class, String)
		resetAll();
		expect( beanResolverMock.resolve( Type2.class, "someName" ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// getBean(Class, String) through BeanReference
		resetAll();
		expect( beanResolverMock.resolve( Type2.class, "someName" ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( BeanReference.of( Type2.class, "someName" ) ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// getBeans(List<BeanReference>)
		resetAll();
		expect( beanResolverMock.resolve( Type3.class ) ).andReturn( type3BeanHolder1 );
		expect( beanResolverMock.resolve( Type3.class, "someOtherName" ) ).andReturn( type3BeanHolder2 );
		replayAll();
		BeanHolder<List<Type3>> beans = beanProvider.getBeans(
				Arrays.asList( BeanReference.of( Type3.class ), BeanReference.of( Type3.class, "someOtherName" ) )
		);
		verifyAll();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void getBean_withBeanConfigurer() {
		// Setup
		BeanConfigurer beanConfigurer1Mock = createMock( BeanConfigurer.class );
		BeanConfigurer beanConfigurer2Mock = createMock( BeanConfigurer.class );

		BeanFactory<Type1> beanFactory1Mock = createMock( BeanFactory.class );
		BeanFactory<Type2> beanFactory2Mock = createMock( BeanFactory.class );
		BeanFactory<Type3> beanFactory3Mock = createMock( BeanFactory.class );
		BeanFactory<Type3> beanFactory4Mock = createMock( BeanFactory.class );

		resetAll();
		expect( classResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andReturn( Collections.singletonList( beanConfigurer1Mock ) );
		expect( configurationSourceMock.get( EngineSpiSettings.BEAN_CONFIGURERS ) )
				.andReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2Mock ) ) );
		beanConfigurer1Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type1.class, beanFactory1Mock );
			context.define( Type2.class, "someName", beanFactory2Mock );
			context.define( Type3.class, beanFactory3Mock );
			return null;
		} );
		beanConfigurer2Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, "someOtherName", beanFactory4Mock );
			return null;
		} );
		replayAll();
		BeanProvider beanProvider =
				new ConfiguredBeanProvider( classResolverMock, beanResolverMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type1> type1BeanHolder = BeanHolder.of( new Type1() );
		BeanHolder<Type2> type2BeanHolder = BeanHolder.of( new Type2() );
		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );

		// getBean(Class)
		resetAll();
		expect( beanResolverMock.resolve( Type1.class ) )
				.andThrow( new SearchException( "cannot find Type1" ) );
		expect( beanFactory1Mock.create( EasyMock.anyObject() ) ).andReturn( type1BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( Type1.class ) ).isSameAs( type1BeanHolder );
		verifyAll();

		// getBean(Class, String)
		resetAll();
		expect( beanResolverMock.resolve( Type2.class, "someName" ) )
				.andThrow( new SearchException( "cannot find Type2#someName" ) );
		expect( beanFactory2Mock.create( EasyMock.anyObject() ) ).andReturn( type2BeanHolder );
		replayAll();
		assertThat( beanProvider.getBean( Type2.class, "someName" ) ).isSameAs( type2BeanHolder );
		verifyAll();

		// getBeans(List<BeanReference>)
		resetAll();
		expect( beanResolverMock.resolve( Type3.class ) )
				.andThrow( new SearchException( "cannot find Type3" ) );
		expect( beanResolverMock.resolve( Type3.class, "someOtherName" ) )
				.andThrow( new SearchException( "cannot find Type3#someOtherName" ) );
		expect( beanFactory3Mock.create( EasyMock.anyObject() ) ).andReturn( type3BeanHolder1 );
		expect( beanFactory4Mock.create( EasyMock.anyObject() ) ).andReturn( type3BeanHolder2 );
		replayAll();
		BeanHolder<List<Type3>> beans = beanProvider.getBeans(
				Arrays.asList( BeanReference.of( Type3.class ), BeanReference.of( Type3.class, "someOtherName" ) )
		);
		verifyAll();
		assertThat( beans.get() )
				.containsExactly( type3BeanHolder1.get(), type3BeanHolder2.get() );
	}

	@Test
	public void getBeansWithRole() {
		BeanConfigurer beanConfigurer1Mock = createMock( BeanConfigurer.class );
		BeanConfigurer beanConfigurer2Mock = createMock( BeanConfigurer.class );

		BeanFactory<Type3> beanFactory1Mock = createMock( BeanFactory.class );
		BeanFactory<Type3> beanFactory2Mock = createMock( BeanFactory.class );
		BeanFactory<Type3> beanFactory3Mock = createMock( BeanFactory.class );

		resetAll();
		expect( classResolverMock.loadJavaServices( BeanConfigurer.class ) )
				.andReturn( Collections.singletonList( beanConfigurer1Mock ) );
		expect( configurationSourceMock.get( EngineSpiSettings.BEAN_CONFIGURERS ) )
				.andReturn( (Optional) Optional.of( Collections.singletonList( beanConfigurer2Mock ) ) );
		beanConfigurer1Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, beanFactory1Mock );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class ) );
			return null;
		} );
		beanConfigurer2Mock.configure( EasyMock.anyObject() );
		expectLastCall().andAnswer( () -> {
			BeanConfigurationContext context = (BeanConfigurationContext) EasyMock.getCurrentArguments()[0];
			context.define( Type3.class, "someName", beanFactory2Mock );
			context.define( Type3.class, "someOtherName", beanFactory3Mock );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class, "someNameWithNoAssignedBeanFactory" ) );
			context.assignRole( RoleType.class, BeanReference.of( Type3.class, "someOtherName" ) );
			return null;
		} );
		replayAll();
		BeanProvider beanProvider =
				new ConfiguredBeanProvider( classResolverMock, beanResolverMock, configurationSourceMock );
		verifyAll();

		BeanHolder<Type3> type3BeanHolder1 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder2 = BeanHolder.of( new Type3() );
		BeanHolder<Type3> type3BeanHolder3 = BeanHolder.of( new Type3() );

		// getBeansWithRole
		resetAll();
		expect( beanResolverMock.resolve( Type3.class ) )
				.andThrow( new SearchException( "cannot find Type3" ) );
		expect( beanFactory1Mock.create( EasyMock.anyObject() ) ).andReturn( type3BeanHolder1 );
		expect( beanResolverMock.resolve( Type3.class, "someNameWithNoAssignedBeanFactory" ) )
				.andReturn( type3BeanHolder2 );
		expect( beanResolverMock.resolve( Type3.class, "someOtherName" ) )
				.andThrow( new SearchException( "cannot find Type3#someOtherName" ) );
		expect( beanFactory3Mock.create( EasyMock.anyObject() ) ).andReturn( type3BeanHolder3 );
		replayAll();
		BeanHolder<List<RoleType>> beansWithRole = beanProvider.getBeansWithRole( RoleType.class );
		verifyAll();
		assertThat( beansWithRole.get() )
				.containsExactlyInAnyOrder( type3BeanHolder1.get(), type3BeanHolder2.get(), type3BeanHolder3.get() );

		// Roles should ignore inheritance
		resetAll();
		replayAll();
		BeanHolder<List<Object>> beansWithObjectRole = beanProvider.getBeansWithRole( Object.class );
		verifyAll();
		assertThat( beansWithObjectRole.get() ).isEmpty();

		// Unassigned roles should result in an empty list
		resetAll();
		replayAll();
		BeanHolder<List<NonRoleType>> beansWithNonRole = beanProvider.getBeansWithRole( NonRoleType.class );
		verifyAll();
		assertThat( beansWithNonRole.get() ).isEmpty();

		// Assigned roles should not affect the behavior of getBean()
		resetAll();
		expect( beanResolverMock.resolve( RoleType.class ) ).andReturn( (BeanHolder) type3BeanHolder3 );
		replayAll();
		assertThat( beanProvider.getBean( RoleType.class ) ).isSameAs( type3BeanHolder3 );
		verifyAll();
	}

	private static class Type1 {
	}

	private static class Type2 {
	}

	private interface RoleType {
	}

	private interface NonRoleType {
	}

	private static class Type3 implements RoleType {
	}

}