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

package com.dorkbox.cabParser.structure;

public interface CabConstants {

  int CAB_BLOCK_SIZE = 32768;
  int CAB_BLOCK_SIZE_THRESH = 32767;

  int COMPRESSION_TYPE_NONE = 0;
  int COMPRESSION_TYPE_MSZIP = 1;
  int COMPRESSION_TYPE_QUANTUM = 2;
  int COMPRESSION_TYPE_LZX = 3;

  int RESERVED_CFHEADER = 1;
  int RESERVED_CFFOLDER = 2;

  int RESERVED_CFDATA = 3;

  int CAB_PROGRESS_INPUT = 1;

  /**
   * FLAG_PREV_CABINET is set if this cabinet file is not the first in a set
   * of cabinet files. When this bit is set, the szCabinetPrev and szDiskPrev
   * fields are present in this CFHEADER.
   */
  int FLAG_PREV_CABINET = 0x0001;

  /**
   * FLAG_NEXT_CABINET is set if this cabinet file is not the last in a set of
   * cabinet files. When this bit is set, the szCabinetNext and szDiskNext
   * fields are present in this CFHEADER.
   */
  int FLAG_NEXT_CABINET = 0x0002;

  /**
   * FLAG_RESERVE_PRESENT is set if this cabinet file contains any reserved
   * fields. When this bit is set, the cbCFHeader, cbCFFolder, and cbCFData
   * fields are present in this CFHEADER.
   */
  int FLAG_RESERVE_PRESENT = 0x0004;
}
