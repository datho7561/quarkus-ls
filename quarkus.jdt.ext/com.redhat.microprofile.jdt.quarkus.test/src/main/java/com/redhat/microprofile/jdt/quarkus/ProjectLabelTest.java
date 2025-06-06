/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.redhat.microprofile.jdt.quarkus;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.ProjectLabelManager;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest.GradleProjectName;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest.MicroProfileMavenProjectName;
import org.eclipse.lsp4mp.jdt.core.utils.JDTMicroProfileUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * Project label tests
 *
 */
public class ProjectLabelTest {

	@Test
	public void getProjectLabelInfoOnlyMaven() throws Exception {
		IJavaProject maven = BasePropertiesManagerTest.loadMavenProject(MicroProfileMavenProjectName.empty_maven_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, maven);
		assertLabels(projectLabelEntries, maven, "maven");
	}

	@Test
	public void getProjectLabelInfoOnlyGradle() throws Exception {
		IJavaProject gradle = BasePropertiesManagerTest.loadGradleProject(GradleProjectName.empty_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, gradle);
		assertLabels(projectLabelEntries, gradle, "gradle");
	}

	@Test
	public void getProjectLabelQuarkusMaven() throws Exception {
		IJavaProject quarkusMaven = BasePropertiesManagerTest.loadMavenProject(MicroProfileMavenProjectName.using_vertx);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, quarkusMaven);
		assertLabels(projectLabelEntries, quarkusMaven, "quarkus", "microprofile", "maven");
	}

	@Test
	public void getProjectLabelQuarkusGradle() throws Exception {
		IJavaProject quarkusGradle = BasePropertiesManagerTest
				.loadGradleProject(GradleProjectName.quarkus_gradle_project);
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();
		assertProjectLabelInfoContainsProject(projectLabelEntries, quarkusGradle);
		assertLabels(projectLabelEntries, quarkusGradle, "quarkus", "microprofile", "gradle");
	}

	@Test
	public void getProjectLabelMultipleProjects() throws Exception {
		IJavaProject [] projects = BasePropertiesManagerTest.loadJavaProjects(new String [] {
				"maven/" + MicroProfileMavenProjectName.using_vertx,
				"gradle/" + GradleProjectName.quarkus_gradle_project,
				"maven/" + MicroProfileMavenProjectName.empty_maven_project,
				"gradle/" + GradleProjectName.empty_gradle_project
				});
		List<ProjectLabelInfoEntry> projectLabelEntries = ProjectLabelManager.getInstance().getProjectLabelInfo();

		assertProjectLabelInfoContainsProject(projectLabelEntries, projects[0], projects[1], projects[2], projects[3]);
		assertLabels(projectLabelEntries, projects[0], "quarkus", "microprofile", "maven");
		assertLabels(projectLabelEntries, projects[1], "quarkus", "microprofile", "gradle");
		assertLabels(projectLabelEntries, projects[2], "maven");
		assertLabels(projectLabelEntries, projects[3], "gradle");
	}

	private void assertProjectLabelInfoContainsProject(List<ProjectLabelInfoEntry> projectLabelEntries,
			IJavaProject... javaProjects) throws CoreException {
		List<String> actualProjectPaths = projectLabelEntries.stream().map(e -> e.getUri())
				.collect(Collectors.toList());
		for (IJavaProject javaProject : javaProjects) {
			assertContains(actualProjectPaths, JDTMicroProfileUtils.getProjectURI(javaProject.getProject()));
		}
	}

	private void assertLabels(List<ProjectLabelInfoEntry> projectLabelEntries, IJavaProject javaProject,
			String... expectedLabels) throws CoreException {
		String javaProjectPath = JDTMicroProfileUtils.getProjectURI(javaProject.getProject());
		List<String> actualLabels = getLabelsFromProjectPath(projectLabelEntries, javaProjectPath);
		Assert.assertEquals(
				"Test project labels size for '" + javaProjectPath + "' with labels ["
						+ actualLabels.stream().collect(Collectors.joining(",")) + "]",
				expectedLabels.length, actualLabels.size());
		for (String expectedLabel : expectedLabels) {
			assertContains(actualLabels, expectedLabel);
		}
	}

	private List<String> getLabelsFromProjectPath(List<ProjectLabelInfoEntry> projectLabelEntries, String projectPath) {
		for (ProjectLabelInfoEntry entry : projectLabelEntries) {
			if (entry.getUri().equals(projectPath)) {
				return entry.getLabels();
			}
		}
		return Collections.emptyList();
	}

	private void assertContains(List<String> list, String strToFind) {
		for (String str : list) {
			if (str.equals(strToFind)) {
				return;
			}
		}
		Assert.fail("Expected List to contain <\"" + strToFind + "\">.");
	}
}