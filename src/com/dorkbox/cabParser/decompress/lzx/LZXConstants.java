/*
 * Copyright 2012 dorkbox, llc
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

package com.dorkbox.cabParser.decompress.lzx;

public interface LZXConstants {

  public static final int PRETREE_NUM_ELEMENTS = 20;
  public static final int NUM_CHARS = 256;

  public static final int SECONDARY_NUM_ELEMENTS = 249;

  public static final int ALIGNED_NUM_ELEMENTS = 8;
  public static final int NUM_PRIMARY_LENGTHS = 7;

  public static final int MIN_MATCH = 2;
  public static final int MAX_MATCH = 257;

  public static final int NUM_REPEATED_OFFSETS = 3;
  public static final int MAX_GROWTH = 6144;

  public static final int E8_DISABLE_THRESHOLD = 32768;

  public static final int BLOCKTYPE_VERBATIM = 1;
  public static final int BLOCKTYPE_ALIGNED = 2;
  public static final int BLOCKTYPE_UNCOMPRESSED = 3;
  public static final int BLOCKTYPE_INVALID = 4;
}
