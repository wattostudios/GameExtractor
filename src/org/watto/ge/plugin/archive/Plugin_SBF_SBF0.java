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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio_Chunks;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SBF_SBF0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SBF_SBF0() {

    super("SBF_SBF0", "SBF_SBF0");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("sbf");
    setGames("Delta Force",
        "Delta Force: Black Hawk Down",
        "Delta Force: Black Hawk Down: Team Sabre",
        "Delta Force: Task Force Dagger",
        "Delta Force Land Warrior",
        "Delta Force Xtreme",
        "Delta Force Xtreme 2",
        "F-16 Multirole Fighter",
        "Joint Operations Combined Arms",
        "Tachyon: The Fringe");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("SBF0")) {
        rating += 50;
      }

      fm.skip(4);

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header (SBF0) // note the zero, it is not an O
      // 4 - Unknown (256)
      // 8 - Version? (1)
      // 4 - Unknown (24)
      fm.skip(20);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename
        String filename = fm.readNullString(16) + ".wav";
        FieldValidator.checkFilename(filename);

        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Type ID?
        fm.skip(8);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
      }

      // Now go through the archive, work out the offsets/lengths of each audio data chunk, and set the exporter appropriately
      for (int i = 0; i < numFiles; i++) {
        Resource currentResource = resources[i];

        long offset = currentResource.getOffset();
        long length = currentResource.getLength();

        fm.seek(offset);

        // Read the first block size, to get an approx number of blocks in the file
        int numBlocks = (int) (length / (fm.readInt() + 8)); // +8 for the header fields
        FieldValidator.checkNumFiles(numBlocks);

        fm.seek(offset);

        long[] offsets = new long[numBlocks];
        long[] lengths = new long[numBlocks];

        int realNumBlocks = 0;
        while (length > 0) {
          if (realNumBlocks >= numBlocks) {
            // hit the end of the file - it "thinks" there's more data because the last block is less than the full block size
            break;
          }

          // 4 - Block Length
          int blockSize = fm.readInt();
          FieldValidator.checkLength(blockSize, arcSize);

          // 4 - Other Data
          fm.skip(4);

          // X - Raw Audio Data
          offsets[realNumBlocks] = fm.getOffset();
          lengths[realNumBlocks] = blockSize;

          fm.skip(blockSize);

          realNumBlocks++;
          length -= (blockSize + 8); // +8 for the header fields
        }

        if (realNumBlocks < numBlocks) {
          long[] oldOffsets = offsets;
          offsets = new long[realNumBlocks];
          System.arraycopy(oldOffsets, 0, offsets, 0, realNumBlocks);

          long[] oldLengths = lengths;
          lengths = new long[realNumBlocks];
          System.arraycopy(oldLengths, 0, lengths, 0, realNumBlocks);
        }

        // Convert the Resource into one that stores the required audio data and chunk data
        Resource_WAV_RawAudio_Chunks resource = new Resource_WAV_RawAudio_Chunks();
        resource.copyFrom(currentResource);
        resource.setAudioProperties(22050, (short) 8, (short) 2);
        resource.setLengths(lengths);
        resource.setOffsets(offsets);

        resources[i] = resource;
      }

      fm.close();

      return resources;

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
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 4 - Header (SBF0) // note the zero, it is not an O
      // 4 - Unknown (256)
      // 8 - Version? (1)
      // 4 - Unknown (24)
      // 4 - numFiles
      fm.writeBytes(src.readBytes(24));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 24 + (32 * numFiles);
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 16 - Filename
        // 4 - Offset
        // 4 - Length
        src.skip(32);
        fm.writeNullString(resources[i].getName(), 16);
        fm.writeInt((int) offset);
        fm.writeInt((int) length);

        // 8 - Type ID?
        fm.writeBytes(src.readBytes(8));

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}