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

import com.dorkbox.cabParser.structure.CorruptCabException;

final class DecompressLzxTree implements LZXConstants {

  private static final byte[] array = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
  private int size;
  private int[] aa;

  int[] LENS;
  private int[] a1;
  private int[] a2;

  private int[] table;
  private int b1;
  private int b2;
  private int b3;

  private int b4;
  private DecompressLzx decompressor;

  private DecompressLzxTree root;
  private int[] c1 = new int[17];
  private int[] c2 = new int[17];
  private int[] c3 = new int[18];

  DecompressLzxTree(int size, int paramInt2, DecompressLzx decompressor, DecompressLzxTree root) {
    this.size = size;
    this.b1 = paramInt2;
    this.decompressor = decompressor;
    this.root = root;
    this.b2 = 1 << this.b1;
    this.b3 = this.b2 - 1;
    this.b4 = 32 - this.b1;
    this.a1 = new int[this.size * 2];
    this.a2 = new int[this.size * 2];
    this.table = new int[this.b2];
    this.aa = new int[this.size];
    this.LENS = new int[this.size];
  }

  void buildTable() throws CorruptCabException {
    int[] table = this.table;
    int[] c3 = this.c3;
    int b1 = this.b1;
    int i = 1;

    do {
      this.c1[i] = 0;
      i++;
    }
    while (i <= 16);

    for (i = 0; i < this.size; i++) {
      this.c1[this.LENS[i]]++;
    }

    c3[1] = 0;
    i = 1;
    do {
      c3[i + 1] = c3[i] + (this.c1[i] << 16 - i);
      i++;
    }
    while (i <= 16);
    if (c3[17] != 65536) {
      if (c3[17] == 0) {
        for (i = 0; i < this.b2; i++) {
          table[i] = 0;
        }
        return;
      }
      throw new CorruptCabException();
    }

    int i2 = 16 - b1;
    for (i = 1; i <= b1; i++) {
      c3[i] >>>= i2;
      this.c2[i] = 1 << b1 - i;
    }

    while (i <= 16) {
      this.c2[i] = 1 << 16 - i;
      i++;
    }

    i = c3[b1 + 1] >>> i2;
    if (i != 65536) {
      while (i < this.b2) {
        table[i] = 0;
        i++;
      }
    }

    int k = this.size;
    for (int j = 0; j < this.size; j++) {
      int i1 = this.LENS[j];
      if (i1 != 0) {
        int m = c3[i1] + this.c2[i1];
        if (i1 <= b1) {
          if (m > this.b2) {
            throw new CorruptCabException();
          }
          for (i = c3[i1]; i < m; i++) {
            table[i] = j;
          }
          c3[i1] = m;
        }
        else {
          int n = c3[i1];
          c3[i1] = m;
          int i6 = n >>> i2;
          int i5 = 2;
          i = i1 - b1;
          n <<= b1;

          do {
            int i4;
            if (i5 == 2) {
              i4 = table[i6];
            }
            else if (i5 == 0) {
              i4 = this.a1[i6];
            }
            else {
              i4 = this.a2[i6];
            }

            if (i4 == 0) {
              this.a1[k] = 0;
              this.a2[k] = 0;
              if (i5 == 2) {
                table[i6] = -k;
              }
              else if (i5 == 0) {
                this.a1[i6] = -k;
              }
              else {
                this.a2[i6] = -k;
              }
              i4 = -k;
              k++;
            }

            i6 = -i4;
            if ((n & 0x8000) == 0) {
              i5 = 0;
            }
            else {
              i5 = 1;
            }
            n <<= 1;
            i--;
          }
          while (i != 0);

          if (i5 == 0) {
            this.a1[i6] = j;
          }
          else {
            this.a2[i6] = j;
          }
        }
      }
    }
  }

  int decodeElement() {
    int i = this.table[this.decompressor.bitsLeft >>> this.b4 & this.b3];

    while (i < 0) {
      int j = 1 << this.b4 - 1;
      do {
        i = -i;
        if ((this.decompressor.bitsLeft & j) == 0) {
          i = this.a1[i];
        }
        else {
          i = this.a2[i];
        }
        j >>>= 1;
      }
      while (i < 0);
    }

    this.decompressor.readNumberBits(this.LENS[i]);
    return i;
  }

  void read() {
    System.arraycopy(this.LENS, 0, this.aa, 0, this.size);
  }

  void readLengths() {
    for (int i = 0; i < this.size; i++) {
      this.LENS[i] = this.decompressor.readBits(3);
    }
  }

  void readLengths(int first, int last) throws CorruptCabException {
    for (int i = 0; i < 20; i++) {
      this.root.LENS[i] = (byte) this.decompressor.readBits(4);
    }

    this.root.buildTable();

    for (int i = first; i < last; i++) {
      int k = this.root.decodeElement();
      int j;

      if (k == 17) {
        j = this.decompressor.readBits(4) + 4;
        if (i + j >= last) {
          j = last - i;
        }
        while (j-- > 0) {
          this.LENS[i++] = 0;
        }
        i--;
      }
      else if (k == 18) {
        j = this.decompressor.readBits(5) + 20;
        if (i + j >= last) {
          j = last - i;
        }
        while (j-- > 0) {
          this.LENS[i++] = 0;
        }
        i--;
      }
      else if (k == 19) {
        j = this.decompressor.readBits(1) + 4;
        if (i + j >= last) {
          j = last - i;
        }

        k = this.root.decodeElement();
        int m = array[this.aa[i] - k + 17];

        while (j-- > 0) {
          this.LENS[i++] = m;
        }
        i--;
      }
      else {
        this.LENS[i] = array[this.aa[i] - k + 17];
      }
    }
  }

  void reset() {
    for (int i = 0; i < this.size; i++) {
      this.LENS[i] = 0;
      this.aa[i] = 0;
    }
  }
}
