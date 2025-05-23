/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.jdt.quarkus;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.PropertiesManager;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test with find MicroProfile definition.
 * 
 * @author Angelo ZERR
 *
 */
public class PropertiesManagerLocationTest extends BasePropertiesManagerTest {

	@BeforeClass
	public static void setupTests() throws CoreException, Exception {
		loadMavenProject(MicroProfileMavenProjectName.using_vertx);
	}

	@Test
	public void usingVertxTest() throws Exception {
		// Enable classFileContentsSupport to generate jdt Location
		enableClassFileContentsSupport();

		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.using_vertx);

		// Test with JAR
		// quarkus.datasource.url
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"io.quarkus.reactive.pg.client.runtime.DataSourceConfig", "url", null, JDT_UTILS,
				new NullProgressMonitor());
		Assert.assertNotNull("Definition from JAR", location);

		// Test with deployment JAR
		// quarkus.arc.auto-inject-fields
		location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"io.quarkus.arc.deployment.ArcConfig", "autoInjectFields", null, JDT_UTILS,
				new NullProgressMonitor());
		Assert.assertNotNull("Definition deployment from JAR", location);

		// Test with Java sources
		// myapp.schema.create
		location = PropertiesManager.getInstance().findPropertyLocation(javaProject, "org.acme.vertx.FruitResource",
				"schemaCreate", null, JDT_UTILS, new NullProgressMonitor());
		Assert.assertNotNull("Definition from Java Sources", location);
	}

	@Test
	public void configPropertiesTest() throws Exception {
		// Enable classFileContentsSupport to generate jdt Location
		enableClassFileContentsSupport();

		IJavaProject javaProject = loadMavenProject(MicroProfileMavenProjectName.config_properties);

		// Test with method
		// greetingInterface.name
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.IGreetingConfiguration", null, "getName()QOptional<QString;>;",
				JDT_UTILS, new NullProgressMonitor());
		
		Assert.assertNotNull("Definition from IGreetingConfiguration#getName() method", location);
	}
	
	@Test
	public void configPropertiesMethodTest() throws Exception {
		// Enable classFileContentsSupport to generate jdt Location
		enableClassFileContentsSupport();

		IJavaProject javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);

		// Test with method with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingMethodResource", null, "setMessage(QString;)V",
				JDT_UTILS, new NullProgressMonitor());
		
		Assert.assertNotNull("Definition from GreetingMethodResource#setMessage() method", location);
	}
	
	@Test
	public void configPropertiesConstructorTest() throws Exception {
		// Enable classFileContentsSupport to generate jdt Location
		enableClassFileContentsSupport();

		IJavaProject javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);

		// Test with constructor with parameters
		// greeting.constructor.message
		Location location = PropertiesManager.getInstance().findPropertyLocation(javaProject,
				"org.acme.config.GreetingConstructorResource", null,
				"GreetingConstructorResource(QString;QString;QOptional<QString;>;)V",
				JDT_UTILS, new NullProgressMonitor());
		
		Assert.assertNotNull("Definition from GreetingConstructorResource constructor", location);
	}
	
	private static void enableClassFileContentsSupport() {
		Map<String, Object> extendedClientCapabilities = new HashMap<>();
		extendedClientCapabilities.put("classFileContentsSupport", "true");
		JavaLanguageServerPlugin.getPreferencesManager().updateClientPrefences(new ClientCapabilities(),
				extendedClientCapabilities);
	}

}