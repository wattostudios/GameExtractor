/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.io.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
**********************************************************************************************
  An InputStream that contains an overwriting offset and length. The InputStream works as
  usual - reading from wherever it is currently positioned in the file, but it will show an
  EOF whenever the fakeLength has been reached, regardless of the EOF of the underlying File.
  Useful for code that read until EOF, however you want it to stop prematurely, such as when
  the file is stored in an archive - so as to stop at the end of a single file rather than the
  end of the whole archive.
**********************************************************************************************
**/

public class FakeFileInputStream extends FileInputStream {

  long fakePointer = 0;
  long fakeLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeFileInputStream(File path) throws FileNotFoundException {
    super(path);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeFileInputStream(String path) throws FileNotFoundException {
    super(path);
  }

  // FAKE METHODS

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int available() throws IOException {
    return (int) (fakeLength - fakePointer);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() throws IOException {
    fakePointer++;
    if (fakePointer > fakeLength) {
      return -1;
    }
    return super.read();
  }

  // NORMAL METHODS

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read(byte[] byteArray) throws IOException {
    int length = byteArray.length;
    if (fakePointer + length > fakeLength) {
      length = (int) (fakeLength - fakePointer);
    }
    fakePointer += length;
    super.read(byteArray, 0, length);

    return length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read(byte[] byteArray, int offset, int length) throws IOException {
    if (fakePointer + length > fakeLength) {
      length = (int) (fakeLength - fakePointer);
    }
    fakePointer += length;

    return super.read(byteArray, offset, length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFakeLength(long fakeLength) {
    this.fakeLength = fakeLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFakePointer(long fakePointer) {
    this.fakePointer = fakePointer;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long skip(long numToSkip) throws IOException {
    fakePointer += numToSkip;
    return super.skip(numToSkip);
  }

}