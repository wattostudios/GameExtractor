////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.io;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.ManipulatorBuffer;

/***********************************************************************************************
Utilities for copying <code>File</code>s from 1 location to another
***********************************************************************************************/
public class FileCopier {

  /** the source file **/
  ManipulatorBuffer source = null;

  /** the target file **/
  ManipulatorBuffer target = null;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FileCopier() {
  }

  /***********************************************************************************************
  Sets up the <code>File</code>s for copying
  @param source the <code>File</code> to copy from
  @param target the <code>File</code> to copy to
  ***********************************************************************************************/
  public FileCopier(File source, File target) {
    this.source = new FileBuffer(source, false);
    this.target = new FileBuffer(source, true);
  }

  /***********************************************************************************************
  Sets up the <code>Manipulator</code>s for copying
  @param source the <code>Manipulator</code> to copy from
  @param target the <code>Manipulator</code> to copy to
  ***********************************************************************************************/
  public FileCopier(Manipulator source, Manipulator target) {
    this.source = source.getBuffer();
    this.target = target.getBuffer();
  }

  /***********************************************************************************************
  Sets up the <code>ManipulatorBuffer</code>s for copying
  @param source the <code>ManipulatorBuffer</code> to copy from
  @param target the <code>ManipulatorBuffer</code> to copy to
  ***********************************************************************************************/
  public FileCopier(ManipulatorBuffer source, ManipulatorBuffer target) {
    this.source = source;
    this.target = target;
  }

  /***********************************************************************************************
  Copies the <code>source</code> to the <code>target</code>
  ***********************************************************************************************/
  public void copy() {
    copy(source, target);
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes from the <code>source</code> to the <code>target</code>
  @param length the number of bytes to copy
  ***********************************************************************************************/
  public void copy(long length) {
    copy(source, target, length);
  }

  /***********************************************************************************************
  Sets the source <code>File</code>
  @param source the <code>File</code> to copy from
  ***********************************************************************************************/
  public void setSource(File source) {
    this.source = new FileBuffer(source, false);
  }

  /***********************************************************************************************
  Sets the source <code>Manipulator</code>
  @param source the <code>Manipulator</code> to copy from
  ***********************************************************************************************/
  public void setSource(Manipulator source) {
    this.source = source.getBuffer();
  }

  /***********************************************************************************************
  Sets the source <code>ManipulatorBuffer</code>
  @param source the <code>ManipulatorBuffer</code> to copy from
  ***********************************************************************************************/
  public void setSource(ManipulatorBuffer source) {
    this.source = source;
  }

  /***********************************************************************************************
  Sets the target <code>File</code>
  @param target the <code>File</code> to copy to
  ***********************************************************************************************/
  public void setTarget(File target) {
    this.target = new FileBuffer(target, true);
  }

  /***********************************************************************************************
  Sets the target <code>Manipulator</code>
  @param target the <code>Manipulator</code> to copy to
  ***********************************************************************************************/
  public void setTarget(Manipulator target) {
    this.target = target.getBuffer();
  }

  /***********************************************************************************************
  Sets the target <code>ManipulatorBuffer</code>
  @param target the <code>ManipulatorBuffer</code> to copy to
  ***********************************************************************************************/
  public void setTarget(ManipulatorBuffer target) {
    this.target = target;
  }

  /***********************************************************************************************
  Copies the <code>source</code> <code>File</code> to the <code>target</code> <code>File</code>
  @param source the <code>File</code> to copy from
  @param target the <code>File</code> to copy to
  ***********************************************************************************************/
  public static void copy(File source, File target) {
    FileBuffer sourceBuffer = new FileBuffer(source, false);
    FileBuffer targetBuffer = new FileBuffer(target, true);

    copy(sourceBuffer, targetBuffer);

    sourceBuffer.close();
    targetBuffer.close();
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes from the <code>source</code> <code>File</code> to
  the <code>target</code> <code>File</code>
  @param source the <code>File</code> to copy from
  @param target the <code>File</code> to copy to
  @param length the number of bytes to copy
  ***********************************************************************************************/
  public static void copy(File source, File target, long length) {
    FileBuffer sourceBuffer = new FileBuffer(source, false);
    FileBuffer targetBuffer = new FileBuffer(target, true);

    copy(sourceBuffer, targetBuffer, length);

    sourceBuffer.close();
    targetBuffer.close();
  }

  /***********************************************************************************************
  Copies the <code>source</code> <code>Manipulator</code> to the <code>target</code>
  <code>Manipulator</code>
  @param source the <code>Manipulator</code> to copy from
  @param target the <code>Manipulator</code> to copy to
  ***********************************************************************************************/
  public static void copy(Manipulator source, Manipulator target) {
    copy(source.getBuffer(), target.getBuffer());
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes from the <code>source</code> <code>Manipulator</code> to
  the <code>target</code> <code>Manipulator</code>
  @param source the <code>Manipulator</code> to copy from
  @param target the <code>Manipulator</code> to copy to
  @param length the number of bytes to copy
  ***********************************************************************************************/
  public static void copy(Manipulator source, Manipulator target, long length) {
    copy(source.getBuffer(), target.getBuffer(), length);
  }

  /***********************************************************************************************
  Copies the <code>source</code> <code>ManipulatorBuffer</code> to the <code>target</code>
  <code>ManipulatorBuffer</code>
  @param source the <code>ManipulatorBuffer</code> to copy from
  @param target the <code>ManipulatorBuffer</code> to copy to
  ***********************************************************************************************/
  public static void copy(ManipulatorBuffer source, ManipulatorBuffer target) {
    copy(source, target, source.length());
  }

  /***********************************************************************************************
  Copies <code>length</code> bytes from the <code>source</code> <code>ManipulatorBuffer</code> to
  the <code>target</code> <code>ManipulatorBuffer</code>
  @param source the <code>ManipulatorBuffer</code> to copy from
  @param target the <code>ManipulatorBuffer</code> to copy to
  @param length the number of bytes to copy
  ***********************************************************************************************/
  public static void copy(ManipulatorBuffer source, ManipulatorBuffer target, long length) {
    try {

      target.forceWrite();

      // find the smallest of the lengths of the buffers
      int sourceBufferSize = source.getBufferSize();
      int targetBufferSize = target.getBufferSize();

      int bufferSize = sourceBufferSize;
      if (targetBufferSize < bufferSize) {
        bufferSize = targetBufferSize;
      }

      // copy the buffer in lengths of bufferSize
      byte[] buffer = new byte[bufferSize];
      while (length > bufferSize) {
        source.read(buffer);
        target.write(buffer);
        length -= bufferSize;
      }

      if (length != 0) {
        // copy the remaining data, which is smaller than the bufferSize
        source.read(buffer, 0, (int) length);
        target.write(buffer, 0, (int) length);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }
}