/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.project;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(DataProviderRunner.class)
public class ProjectLifeCycleListenersImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProjectLifeCycleListener listener1 = mock(ProjectLifeCycleListener.class);
  private ProjectLifeCycleListener listener2 = mock(ProjectLifeCycleListener.class);
  private ProjectLifeCycleListener listener3 = mock(ProjectLifeCycleListener.class);
  private ProjectLifeCycleListenersImpl underTestNoListeners = new ProjectLifeCycleListenersImpl();
  private ProjectLifeCycleListenersImpl underTestWithListeners = new ProjectLifeCycleListenersImpl(
    new ProjectLifeCycleListener[] {listener1, listener2, listener3});

  @Test
  public void onProjectsDeleted_throws_NPE_if_set_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projects can't be null");

    underTestWithListeners.onProjectsDeleted(null);
  }

  @Test
  public void onProjectsDeleted_throws_NPE_if_set_is_null_even_if_no_listeners() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projects can't be null");

    underTestNoListeners.onProjectsDeleted(null);
  }

  @Test
  public void onProjectsDeleted_has_no_effect_if_set_is_empty() {
    underTestNoListeners.onProjectsDeleted(Collections.emptySet());

    underTestWithListeners.onProjectsDeleted(Collections.emptySet());
    verifyZeroInteractions(listener1, listener2, listener3);
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectsDeleted_does_not_fail_if_there_is_no_listener(Set<Project> projects) {
    underTestNoListeners.onProjectsDeleted(projects);
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectsDeleted_calls_all_listeners_in_order_of_addition_to_constructor(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectsDeleted_calls_all_listeners_even_if_one_throws_an_Exception(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new RuntimeException("Faking listener2 throwing an exception"))
      .when(listener2)
      .onProjectsDeleted(any());

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  @UseDataProvider("oneOrManyProjects")
  public void onProjectsDeleted_calls_all_listeners_even_if_one_throws_an_Error(Set<Project> projects) {
    InOrder inOrder = Mockito.inOrder(listener1, listener2, listener3);
    doThrow(new Error("Faking listener2 throwing an Error"))
      .when(listener2)
      .onProjectsDeleted(any());

    underTestWithListeners.onProjectsDeleted(projects);

    inOrder.verify(listener1).onProjectsDeleted(same(projects));
    inOrder.verify(listener2).onProjectsDeleted(same(projects));
    inOrder.verify(listener3).onProjectsDeleted(same(projects));
    inOrder.verifyNoMoreInteractions();
  }

  @DataProvider
  public static Object[][] oneOrManyProjects() {
    return new Object[][] {
      {singleton(newUniqueProject())},
      {IntStream.range(0, 1 + new Random().nextInt(10)).mapToObj(i -> newUniqueProject()).collect(MoreCollectors.toSet())}
    };
  }

  private static int counter = 3_989;

  private static Project newUniqueProject() {
    int base = counter++;
    return new Project(base + "_uuid", base + "_key", base + "_name");
  }
}
