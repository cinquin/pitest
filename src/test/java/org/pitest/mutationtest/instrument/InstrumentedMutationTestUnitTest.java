/*
 * Copyright 2010 Henry Coles
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package org.pitest.mutationtest.instrument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pitest.Description;
import org.pitest.extension.ResultCollector;
import org.pitest.internal.IsolationUtils;
import org.pitest.junit.JUnitCompatibleConfiguration;
import org.pitest.mutationtest.MutationConfig;
import org.pitest.mutationtest.Mutator;

public class InstrumentedMutationTestUnitTest {

  private InstrumentedMutationTestUnit testee;

  @Mock
  private ResultCollector              rc;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  public static class One {
    public static int increment() {
      int i = 0;
      i++;
      return i;
    }

    public static int incrementAgain() {
      int i = 0;
      i++;
      return i;
    }

    public static int moreIncrements() {
      int i = 0;
      i++;
      return i;
    }

  }

  public static class TestOne {
    @Test
    public void test() {
      final int i = One.increment();
      System.err.println("Running test 1 = " + i);
      System.err.flush();
      assertEquals(1, i);
    }

    @Test
    public void test2() {
      final int i = One.incrementAgain();
      System.err.println("Running test 2 = " + i);
      System.err.flush();
      assertEquals(1, i);
    }

    @Test
    public void test3() {

    }

    @Test
    public void test4() throws InterruptedException {

    }

  }

  @Test
  public void testReportsSuccessIfMoreThanThresholdLevelOfMutationsDetected() {
    this.testee = this.createFor(TestOne.class, One.class, 66);
    execute();
    verify(this.rc).notifyEnd(any(Description.class));
  }

  @Test
  public void testReportsFailureIfLessThanThresholdLevelOfMutationsDetected() {
    this.testee = this.createFor(TestOne.class, One.class, 100);
    execute();
    verify(this.rc).notifyEnd(any(Description.class), any(Throwable.class));
  }

  @Test
  public void testReportsSkippedIfNoMutationsDetected() {
    this.testee = new InstrumentedMutationTestUnit(TestOne.class, One.class,
        new MutationConfig(66, Mutator.SWITCHES),
        new JUnitCompatibleConfiguration(), null);
    execute();
    verify(this.rc).notifySkipped(any(Description.class));
  }

  static class HideFromJUnit {
    public static class FailingTest {
      @Test
      public void fail() {
        assertTrue(false);
      }
    }
  }

  @Test
  public void testReportsFailureIfUnmutatedTestsDoNotRunGreen() {
    this.testee = createFor(HideFromJUnit.FailingTest.class, One.class, 66);
    execute();
    verify(this.rc)
        .notifyEnd(any(Description.class), any(AssertionError.class));
  }

  public static class HasStaticInitializer {
    static int i = 0;
    static int j = 0;
    static int k = 0;
    static {
      i++;
      j++;
      k++;
    }
  }

  public static class MutationTestStaticInitializerWithMissingTest {
    @Test()
    public void testFirstMutationPoint() {
      System.out.println("i is " + HasStaticInitializer.i);
      assertEquals(1, HasStaticInitializer.i);
    }

    @Test()
    public void testSeondMutationPoint() {
      System.out.println("j is " + HasStaticInitializer.j);
      assertEquals(1, HasStaticInitializer.j);
    }

  }

  public static class FullyMutationTestStaticInitializer extends
      MutationTestStaticInitializerWithMissingTest {
    @Test()
    public void testThirdMutationPoint() {
      System.out.println("j is " + HasStaticInitializer.k);
      assertEquals(1, HasStaticInitializer.k);
    }

  }

  @Test
  public void testCanCorrectlyDetectMissedMutationInStaticInitializers() {
    // class has three mutations but only 1 test so should fail
    this.testee = createFor(MutationTestStaticInitializerWithMissingTest.class,
        HasStaticInitializer.class, 100);
    execute();
    verify(this.rc).notifyEnd(any(Description.class), any(Throwable.class));
  }

  @Test
  public void testPassesIfAllMutationsInStaticInitializerDetected() {
    this.testee = createFor(FullyMutationTestStaticInitializer.class,
        HasStaticInitializer.class, 100);
    execute();
    verify(this.rc).notifyEnd(any(Description.class));
  }

  private void execute() {
    this.testee.execute(IsolationUtils.getContextClassLoader(), this.rc);
  }

  private InstrumentedMutationTestUnit createFor(final Class<?> test,
      final Class<?> mutee, final int threshold) {
    return new InstrumentedMutationTestUnit(test, mutee, new MutationConfig(
        threshold, Mutator.INCREMENTS), new JUnitCompatibleConfiguration(),
        null);
  }

}
