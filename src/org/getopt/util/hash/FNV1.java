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
 * A family of fast hash functions, originally created by Glenn Fowler, Phong Vo,
 * and improved by Landon Curt Noll.
 * 
 * <p>FNV1 hashes are designed to be fast while maintaining a low collision rate.
 * The FNV1 speed allows one to quickly hash lots of data while maintaining a
 * reasonable collision rate. The high dispersion of the FNV1 hashes makes them
 * well suited for hashing nearly identical strings such as URLs, hostnames,
 * filenames, text, IP addresses, etc.</p>
 * 
 * <p>FNV1a is a variant of FNV1, which is slightly better suited for hashing
 * short values (< 4 octets).</p>
 * 
 * <p>This is a straightforward port of the public domain C version,
 * written by Landon Curt Noll (one of the authors), available from
 * <a href="http://www.isthe.com/chongo/tech/comp/fnv/">his website</a>.</p>
 * 
 * <p>The usage pattern is as follows: to compute the initial hash value
 * you call one of the <code>init(...)</code> methods. After that you may
 * update the hash zero or more times with additional values using the
 * <code>update(...)</code> methods. When you are done, you can retrieve the
 * final hash value with {@link #getHash()}.</p>
 * <p>Individual instances of FNV1 are reusable after you call one of
 * the <code>init(...)</code> methods. However, these implementations are NOT
 * synchronized, so proper care should be taken when using this hash in a multi-threaded
 * environment.</p>
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public abstract class FNV1 {
  /** Initial seed for 32-bit hashes. */
  public static final long FNV1_32_INIT  = 0x811c9dc5L;
  /** Initial seed for 64-bit hashes. */
  public static final long FNV1_64_INIT  = 0xcbf29ce484222325L;
  
  /** Current initial seed. */
  protected long INIT = 0L;
  /** Current hash value. */
  protected long hash = 0L;
  
  /**
   * Initialize this hash instance. Any previous state is reset, and the new
   * hash value is computed.
   * @param s the method {@link String#getBytes(java.lang.String)} is called on
   * this argument, with the UTF-8 encoding (or with the default encoding if that
   * fails), and the hash is computed from the resulting byte array; cannot be null.
   */
  public void init(String s) {
    byte[] buf = null;
    try {
      buf = s.getBytes("UTF-8");
    } catch (Exception e) {
      buf = s.getBytes();
    }
    init(buf, 0, buf.length);
  }
  
  /**
   * Initialize this hash instance. Any previous state is reset, and the new
   * hash value is computed.
   * 
   * @param buf byte buffer from which to compute the hash
   * @param offset starting position in the buffer
   * @param len number of bytes after the starting position
   */
  public void init(byte[] buf, int offset, int len) {
    hash = fnv(buf, offset, len, INIT);
  }
  
  /**
   * Update the hash value. Repeated calls to this method update the hash
   * value accordingly, and they are equivalent to calling the <code>init(...)</code>
   * method once with a concatenated value of all parameters.
   * @param s see (@link #init(String)}
   */
  public void update(String s) {
    byte[] buf = null;
    try {
      buf = s.getBytes("UTF-8");
    } catch (Exception e) {
      buf = s.getBytes();
    }
    update(buf, 0, buf.length);
  }
  
  /**
   * Update the hash value. Repeated calls to this method update the hash
   * value accordingly, and they are equivalent to calling the <code>init(...)</code>
   * method once with a concatenated value of all parameters.
   * 
   * @param buf byte buffer from which to compute the hash
   * @param offset starting position in the buffer
   * @param len number of bytes after the starting position
   */
  public void update(byte[] buf, int offset, int len) {
    hash = fnv(buf, offset, len, hash);
  }
  
  /**
   * Retrieve the hash value
   * @return hash value
   */
  public long getHash() {
    return hash;
  }
  
  /**
   * Compute the hash value.
   * @param buf byte buffer from which to compute the hash
   * @param offset starting position in the buffer
   * @param len number of bytes after the starting position
   * @param seed initial seed (or previous hash value)
   * @return the next hash value
   */
  protected abstract long fnv(byte[] buf, int offset, int len, long seed);
}
