/*
 * The MIT License
 * Copyright © 2020 Starling Bank Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.starlingbank.portallocator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PortAllocatorTest {

  @Test
  void allocatorDoesAllocatePort() {
    // Given
    final PortAllocator allocator = new PortAllocator();

    // When
    final int thisPort = allocator.allocatePort("this");

    // Then
    assertThat(thisPort).isBetween(1, Double.valueOf(Math.pow(2, 16) - 1).intValue());
  }

  @Test
  void allocatorDoesAllocateUniquePortPerPurpose() {
    // Given
    final PortAllocator allocator = new PortAllocator();

    // When
    final int thisPort = allocator.allocatePort("this");
    final int thatPort = allocator.allocatePort("that");

    // Then
    assertThat(thisPort).isNotEqualTo(thatPort);
  }

  @Test
  void allocatorDoesReturnPreviouslyAllocatedPortForSamePurpose() {
    // Given
    final PortAllocator allocator = new PortAllocator();

    // When
    final int theFirstPort = allocator.allocatePort("some purpose");
    final int theSecondPort = allocator.allocatePort("some purpose");

    // Then
    assertThat(theFirstPort).isEqualTo(theSecondPort);
  }

  @Test
  void allocatorIsThreadSafe() throws InterruptedException {
    // Given
    final PortAllocator allocator = new PortAllocator();

    // When
    final List<Callable<Integer>> allocators = new ArrayList<>();
    for (int n = 0; n < 10; n++) {
      allocators.add(() -> allocator.allocatePort("foo"));
    }
    final Set<Integer> allocations = Executors.newCachedThreadPool()
                                              .invokeAll(allocators)
                                              .stream()
                                              .map(PortAllocatorTest::getAllocation)
                                              .collect(Collectors.toSet());

    // Then
    assertThat(allocations.size()).isEqualTo(1);
  }

  private static int getAllocation(final Future<Integer> allocation) {
    try {
      return allocation.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
