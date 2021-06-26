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

import com.dorkbox.cabParser.structure.CabConstants;
import com.dorkbox.cabParser.structure.CabException;

public interface Decompressor extends CabConstants {

  void decompress(byte[] inputBytes, byte[] outputBytes, int inputLength, int outputLength) throws CabException;

  int getMaxGrowth();

  void init(int windowBits) throws CabException;

  void reset(int windowBits) throws CabException;
}
