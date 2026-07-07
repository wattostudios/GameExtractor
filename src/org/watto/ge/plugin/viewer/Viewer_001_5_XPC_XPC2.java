/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_001_5;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_001_5_XPC_XPC2 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_001_5_XPC_XPC2() {
    super("001_5_XPC_XPC2", "001_5_XPC_XPC2 Image");
    setExtensions("xpc");

    setGames("Deadly Premonition");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canReplace(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_001_5) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("XPC2")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      fm.skip(24);

      if (fm.readInt() == 64) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  
  THIS READS ALL FRAMES
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (XPC2)
      // 4 - File Length
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 12 - null
      fm.skip(32);

      // 4 - Directory Offset
      int directoryOffset = fm.readInt();
      FieldValidator.checkOffset(directoryOffset, arcSize);

      // 4 - File Data Offset
      int numFiles = (fm.readInt() - directoryOffset) / 32;
      FieldValidator.checkNumFiles(numFiles);

      // 24 - null
      fm.relativeSeek(directoryOffset);

      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      int[] decompLengths = new int[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename (null terminated, filled with nulls)
        fm.skip(16);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[realNumFiles] = offset;

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[realNumFiles] = length;

        // 4 - Unknown
        fm.skip(4);

        // 4 - Decompressed File Length [>>8]
        int decompLength = fm.readInt() >> 8;
        FieldValidator.checkLength(decompLength);
        decompLengths[realNumFiles] = decompLength;

        if (offset == 0 && length == 0) { // had some images, then some empty padding
          continue;
        }
        realNumFiles++;

      }

      numFiles = realNumFiles;

      ImageResource[] images = new ImageResource[numFiles];

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(offsets[i]);

        // X - File Data (ZLib Compression, DDS Image)
        int compLength = lengths[i];
        byte[] sourceBytes = fm.readBytes(compLength);

        int decompLength = decompLengths[i];
        byte[] decompBytes = new byte[decompLength];

        FileManipulator compFM = new FileManipulator(new ByteBuffer(sourceBytes));

        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(compFM, compLength, decompLength);

        for (int b = 0; b < decompLength; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            decompBytes[b] = (byte) exporter.read();
          }
        }

        exporter.close();
        compFM.close();

        FileManipulator decompFM = new FileManipulator(new ByteBuffer(decompBytes));
        ImageResource image = new Viewer_DDS_DDS().readThumbnail(decompFM);
        decompFM.close();

        image = ImageFormatReader.flipVertically(image);

        images[i] = image;

      }

      fm.close();

      if (numFiles > 0) {
        ImageFormatReader.createFrameTransitions(images);
        images[0].setManualFrameTransition(true);
      }

      ImageResource imageResource = images[0];

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  
  NOTE: THIS READS ONLY THE FIRST FRAME!!!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (XPC2)
      // 4 - File Length
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 12 - null
      fm.skip(32);

      // 4 - Directory Offset
      int directoryOffset = fm.readInt();
      FieldValidator.checkOffset(directoryOffset, arcSize);

      // 4 - File Data Offset
      int numFiles = (fm.readInt() - directoryOffset) / 32;
      FieldValidator.checkNumFiles(numFiles);

      // CHANGED HERE, TO ONLY RETURN 1 IMAGE!!!
      numFiles = 1;

      // 24 - null
      fm.relativeSeek(directoryOffset);

      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      int[] decompLengths = new int[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename (null terminated, filled with nulls)
        fm.skip(16);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        if (offset == 0 && length == 0) { // had some images, then some empty padding
          numFiles = i;
          break;
        }

        // 4 - Unknown
        fm.skip(4);

        // 4 - Decompressed File Length [>>8]
        int decompLength = fm.readInt() >> 8;
        FieldValidator.checkLength(decompLength);
        decompLengths[i] = decompLength;
      }

      ImageResource[] images = new ImageResource[numFiles];

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(offsets[i]);

        // X - File Data (ZLib Compression, DDS Image)
        int decompLength = decompLengths[i];
        byte[] decompBytes = new byte[decompLength];

        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, lengths[i], decompLength);

        for (int b = 0; b < decompLength; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            decompBytes[b] = (byte) exporter.read();
          }
        }

        FileManipulator decompFM = new FileManipulator(new ByteBuffer(decompBytes));
        ImageResource image = new Viewer_DDS_DDS().readThumbnail(decompFM);
        decompFM.close();

        image = ImageFormatReader.flipVertically(image);

        images[i] = image;

      }

      fm.close();

      if (numFiles > 0) {
        ImageFormatReader.createFrameTransitions(images);
        images[0].setManualFrameTransition(true);
      }

      return images[0];

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}