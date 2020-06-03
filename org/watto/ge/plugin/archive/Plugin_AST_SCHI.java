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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.datatype.SplitChunkResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AST_SCHI extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static SplitChunkResource[] resizeSplitChunkResources(SplitChunkResource[] resources, int numResources) {
    SplitChunkResource[] temp = resources;
    resources = new SplitChunkResource[numResources];
    System.arraycopy(temp, 0, resources, 0, numResources);
    return resources;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_AST_SCHI() {

    // TEST - it uses SplitChunkResource!

    super("AST_SCHI", "AST_SCHI");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ast", "mus", "sng", "asf");
    setGames("Need For Speed Underground",
        "Need For Speed Underground 2",
        "MVP Baseball 2005",
        "FIFA 2004",
        "FIFA 2005",
        "FIFA 08",
        "Harry Potter And The Prizoner Of Azkaban",
        "Harry Potter: Quidditch World Cup");
    setPlatforms("PC", "XBox");

    setFileTypes("csf", "CSF Chunked File");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals("SCHl")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();
      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();
      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];

      int k = 0;
      long curOffset = 0;

      TaskProgressManager.setMaximum(arcSize);

      while (curOffset < fm.getLength()) {

        /*
        int compressionType1 = 0;
        int compressionType2 = 0;
        int compressionType3 = 0;
        int numChannels = 0;
        int numSamples = 0;
        int sampleRate = 0;
        
        // 4 - Archive Header
        String header = fm.readString(4);
        if (header.equals("Hrln")) {
          fm.skip(36);
          curOffset = fm.getOffset();
        }
        else if (header.equals("HrSz")) {
          fm.skip(4);
          curOffset = fm.getOffset();
        }
        else if (header.equals("SCHl")) {
        
          // 4 - Directory Offset
          long dirOffset = fm.readInt() + curOffset;
          FieldValidator.checkOffset(dirOffset, arcSize);
        
          // 2 - PT Header
          if (fm.readString(2).equals("PT")) {
            // 2 - platform
            fm.skip(2);
        
            // read through the patch tags
            while (fm.getOffset() < dirOffset) {
              // 1 - Tag
              int tag = ByteConverter.unsign(fm.readByte());
        
              if (tag == 138 || tag == 252 || tag == 253 || tag == 2554 || tag == 255) {
                // these tags have no length or data
                continue;
              }
        
              // 1 - Length
              int tagLength = ByteConverter.unsign(fm.readByte());
        
              // 1 - Data
              if (tag == 128) {
                // compression type
                compressionType1 = fm.readByte();
              }
              else if (tag == 131) {
                // compression type
                compressionType2 = fm.readByte();
              }
              else if (tag == 160) {
                // compression type
                compressionType3 = fm.readByte();
              }
              else if (tag == 133) {
                // number of samples
                if (tagLength == 1) {
                  numSamples = ByteConverter.unsign(fm.readByte());
                }
                else if (tagLength == 2) {
                  numSamples = ShortConverter.changeFormat(fm.readShort());
                }
                else if (tagLength == 3) {
                  numSamples = IntConverter.convertBig(new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() });
                }
                else if (tagLength == 4) {
                  numSamples = IntConverter.changeFormat(fm.readInt());
                }
                else {
                  fm.skip(tagLength);
                }
              }
              else if (tag == 130) {
                // number of channels
                numChannels = fm.readByte();
              }
              else if (tag == 132) {
                // sample rate
                if (tagLength == 1) {
                  sampleRate = ByteConverter.unsign(fm.readByte());
                }
                else if (tagLength == 2) {
                  sampleRate = ShortConverter.changeFormat(fm.readShort());
                }
                else if (tagLength == 3) {
                  sampleRate = IntConverter.convertBig(new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() });
                }
                else if (tagLength == 4) {
                  sampleRate = IntConverter.changeFormat(fm.readInt());
                }
                else {
                  fm.skip(tagLength);
                }
              }
              else {
                fm.skip(tagLength);
              }
            }
          }
        
          // DIRECTORY
          fm.seek(dirOffset);
          // 4 - Directory Header
        
          // 4 - Directory Length
        
          // 4 - Number Of Files
          fm.skip(8);
          int innerNumFiles = fm.readInt();
        
          long[] offsets = new long[innerNumFiles];
          long[] lengths = new long[innerNumFiles];
        
          // ENTRIES
        
          long offset = dirOffset + 16;
          for (int i = 0; i < innerNumFiles; i++) {
            // 4 - Data Header
            fm.skip(4);
        
            // 4 - Compressed Length
            int compLength = fm.readInt() - 16;
            FieldValidator.checkLength(compLength, arcSize);
        
            // 4 - Length
            fm.skip(8);
        
            // 4 - Blank
        
            // X - File Data
            offsets[i] = (int) fm.getOffset();
            lengths[i] = compLength;
            fm.skip(compLength);
        
            offset += compLength + 16;
          }
        
          curOffset = offset + 4;
          fm.skip(8);
          int temp = fm.readByte();
          while (temp == 0 && curOffset < fm.getLength()) {
            temp = fm.readByte();
            curOffset++;
          }
          fm.seek(curOffset);
        
          String filename = Resource.generateFilename(k) + ".asf";
        
          //path,id,name,offset,length,decompLength,exporter
          resources[k] = new SplitChunkResource(path, filename, offsets, lengths);
        
          TaskProgressManager.setValue(offset);
          k++;
        
        }// end if(SCHl)
        */

        // 4 - Archive Header
        String header = fm.readString(4);
        if (header.equals("Hrln")) {
          fm.skip(36);
          curOffset = fm.getOffset();
        }
        else if (header.equals("HrSz")) {
          fm.skip(4);
          curOffset = fm.getOffset();
        }
        else if (header.equals("SCHl")) {
          long startOffset = curOffset;

          // 4 - Directory Offset
          long dirOffset = fm.readInt() + curOffset;
          FieldValidator.checkOffset(dirOffset, arcSize);

          // 2 - PT Header
          // X -  PT tag data

          // DIRECTORY
          fm.seek(dirOffset);

          // 4 - Directory Header
          // 4 - Directory Length
          fm.skip(8);

          // 4 - Number Of Files
          int innerNumFiles = fm.readInt();

          // ENTRIES
          long offset = dirOffset + 16;
          for (int i = 0; i < innerNumFiles; i++) {
            // 4 - Data Header
            fm.skip(4);

            // 4 - Compressed Length
            int compLength = fm.readInt() - 16;
            FieldValidator.checkLength(compLength, arcSize);

            // 4 - Length
            // 4 - Blank
            fm.skip(8);

            // X - File Data
            fm.skip(compLength);

            offset += compLength + 16;
          }

          curOffset = offset + 4;
          fm.skip(8);
          int temp = fm.readByte();
          while (temp == 0 && curOffset < fm.getLength()) {
            temp = fm.readByte();
            curOffset++;
          }
          fm.seek(curOffset);

          String filename = Resource.generateFilename(k) + ".asf";

          long length = curOffset - startOffset;
          //path,id,name,offset,length,decompLength,exporter
          resources[k] = new Resource(path, filename, startOffset, length);

          TaskProgressManager.setValue(offset);
          k++;

        }// end if(SCHl)

      } // end while

      resources = resizeResources(resources, k);

      fm.close();

      return resources;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}