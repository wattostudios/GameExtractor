/*
 * Copyright 2010 Srikanth Reddy Lingala
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lingala.zip4j.crypto.PBKDF2;

import static net.lingala.zip4j.util.Zip4jUtil.convertCharArrayToByteArray;

/*
 * Source referred from Matthias Gartner's PKCS#5 implementation -
 * see http://rtner.de/software/PBKDF2.html
 */

public class PBKDF2Engine {

  private PBKDF2Parameters parameters;
  private PRF prf;

  public PBKDF2Engine(PBKDF2Parameters parameters) {
    this(parameters, null);
  }

  public PBKDF2Engine(PBKDF2Parameters parameters, PRF prf) {
    this.parameters = parameters;
    this.prf = prf;
  }

  public byte[] deriveKey(char[] inputPassword, int dkLen, boolean useUtf8ForPassword) {
    byte p[];
    if (inputPassword == null) {
      throw new NullPointerException();
    }

    p = convertCharArrayToByteArray(inputPassword, useUtf8ForPassword);

    assertPRF(p);
    if (dkLen == 0) {
      dkLen = prf.getHLen();
    }
    return PBKDF2(prf, parameters.getSalt(), parameters.getIterationCount(), dkLen);
  }

  private void assertPRF(byte[] P) {
    if (prf == null) {
      prf = new MacBasedPRF(parameters.getHashAlgorithm());
    }
    prf.init(P);
  }

  private byte[] PBKDF2(PRF prf, byte[] S, int c, int dkLen) {
    if (S == null) {
      S = new byte[0];
    }
    int hLen = prf.getHLen();
    int l = ceil(dkLen, hLen);
    int r = dkLen - (l - 1) * hLen;
    byte T[] = new byte[l * hLen];
    int ti_offset = 0;
    for (int i = 1; i <= l; i++) {
      _F(T, ti_offset, prf, S, c, i);
      ti_offset += hLen;
    }
    if (r < hLen) {
      // Incomplete last block
      byte DK[] = new byte[dkLen];
      System.arraycopy(T, 0, DK, 0, dkLen);
      return DK;
    }
    return T;
  }

  private int ceil(int a, int b) {
    int m = 0;
    if (a % b > 0) {
      m = 1;
    }
    return a / b + m;
  }

  private void _F(byte[] dest, int offset, PRF prf, byte[] S, int c,
                  int blockIndex) {
    int hLen = prf.getHLen();
    byte U_r[] = new byte[hLen];

    // U0 = S || INT (i);
    byte U_i[] = new byte[S.length + 4];
    System.arraycopy(S, 0, U_i, 0, S.length);
    INT(U_i, S.length, blockIndex);

    for (int i = 0; i < c; i++) {
      U_i = prf.doFinal(U_i);
      xor(U_r, U_i);
    }
    System.arraycopy(U_r, 0, dest, offset, hLen);
  }

  private void xor(byte[] dest, byte[] src) {
    for (int i = 0; i < dest.length; i++) {
      dest[i] ^= src[i];
    }
  }

  protected void INT(byte[] dest, int offset, int i) {
    dest[offset] = (byte) (i / (256 * 256 * 256));
    dest[offset + 1] = (byte) (i / (256 * 256));
    dest[offset + 2] = (byte) (i / (256));
    dest[offset + 3] = (byte) (i);
  }

  public PBKDF2Parameters getParameters() {
    return parameters;
  }

  public void setParameters(PBKDF2Parameters parameters) {
    this.parameters = parameters;
  }

  public void setPseudoRandomFunction(PRF prf) {
    this.prf = prf;
  }
}
