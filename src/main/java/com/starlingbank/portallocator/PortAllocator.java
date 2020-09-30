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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PortAllocator {

  public static PortAllocator instance() {
    return INSTANCE;
  }

  /* We only ever want one instance. However, this class is mostly about coordinating with other
   * JVMs that may be running on the same host, via the temp file mechanism. */
  private static final PortAllocator INSTANCE = new PortAllocator();
  private static final File BASE_DIR = new File(javaIoTmpdir(), PortAllocator.class.getCanonicalName());

  static { BASE_DIR.mkdirs(); }

  /*
   * Map of previously allocated ports (purpose -> port number).
   *
   * Remember that we are potentially competing against other JVMs for an allocation so this map
   * won't necessarily be exhaustive.
   */
  private final ConcurrentHashMap<String,Integer> allocations = new ConcurrentHashMap<>();

  PortAllocator() {}

  public int allocatePort(final String purpose) {
    return allocations.computeIfAbsent(purpose, allocate);
  }

  private static final Function<String,Integer> allocate = purpose -> {
    int attempts = 0;
    boolean allocated;

    do {
      try (final ServerSocket socket = new ServerSocket(0)) {
        final int candidatePort = socket.getLocalPort();
        allocated = writeTempFile(purpose, candidatePort);
        if (allocated)
          return candidatePort;

      } catch (IOException e) {
        throw new IllegalStateException("Unable to allocate port", e);
      }
    } while (attempts++ < 32);

    final String message = String.format("Unable to allocate port after 32 attempts; something is wrong. Check %s", BASE_DIR.getAbsolutePath());
    throw new IllegalStateException(message);
  };

  private static boolean writeTempFile(final String purpose,
                                       final int port) throws IOException {
    final File tmpFile = new File(BASE_DIR, Integer.toString(port));
    if (!tmpFile.createNewFile()) {
      return false;
    }

    tmpFile.deleteOnExit();
    try (final Writer writer = new FileWriter(tmpFile)) {
      writer.write(purpose);
      writer.write("\n");
    }

    return true;
  }

  private static File javaIoTmpdir() {
    final String value = System.getProperty("java.io.tmpdir");
    if (value == null) {
      throw new RuntimeException("Unable to determine java.io.tmpdir value");
    }
    return new File(value);
  }

}
