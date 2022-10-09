/*
 * Copyright 2004, Andrzej Bialecki <ab@getopt.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.getopt.util.hash;

/**
 * Implementation of FNV1 - a fast hash function.
 * <p>This implementation uses 64-bit operations.</p>
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class FNV164 extends FNV1 {

  /**
   * Create a hash
   *
   */
  public FNV164() {
    INIT = FNV1_64_INIT;
  }

  protected long fnv(byte[] buf, int offset, int len, long seed) {
    for (int i = offset; i < offset + len; i++) {
      seed += (seed << 1) + (seed << 4) + (seed << 5) +
        (seed << 7) + (seed << 8) + (seed << 40);
      seed ^= buf[i];
    }
    return seed;
  }
}
