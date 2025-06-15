/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STK() {

    super("STK", "STK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Adi's Comprehensive Learning System",
        "Gobliiins",
        "Gobliins 2: The Prince Buffoon",
        "Goblins Quest 3");
    setExtensions("stk", "itk", "ltk");
    setPlatforms("PC");

    setFileTypes(new FileType("snd", "SND Audio", FileType.TYPE_AUDIO));

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      Exporter_LZSS exporter = Exporter_LZSS.getInstance();

      long arcSize = fm.getLength();

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      int realNumFiles = 0;
      boolean[] fileCompressed = new boolean[numFiles];
      boolean[] fileSND = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 13 - Filename (null)
        String filename = fm.readNullString(13);

        // 4 - File Length
        long length = fm.readInt();

        // 4 - File Offset
        long offset = fm.readInt();

        // 1 - Compression (0/1)
        int compression = fm.readByte();

        if (length == 0) {
          continue;
        }
        FieldValidator.checkFilename(filename);
        FieldValidator.checkOffset(offset, arcSize);

        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);

        if (compression == 1) {
          resource.setExporter(exporter);
          fileCompressed[realNumFiles] = true;
        }
        else {
          fileCompressed[realNumFiles] = false;
        }

        // If it's an SND file, attach the WAV exporter for previews etc
        if (filename.toUpperCase().endsWith("SND")) {
          fileSND[realNumFiles] = true;
        }
        else {
          fileSND[realNumFiles] = false;
        }

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
      }

      //go and set the decompressed lengths
      fm.getBuffer().setBufferSize(4);
      for (int i = 0; i < realNumFiles; i++) {
        if (fileSND[i]) {
          // Compressed as well as SND
          if (fileCompressed[i]) {
            // work out the compression details
            Resource resource = resources[i];
            fm.seek(resource.getOffset());

            // 4 - Decompressed Length
            int decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            resource.setOffset(resource.getOffset() + 4);
            resource.setLength(resource.getLength() - 4);
            resource.setDecompressedLength(decompLength);
            TaskProgressManager.setValue(i);

            // now decompress it to a temporary file

            byte[] decompBytes = new byte[decompLength];
            exporter.open(fm, (int) resource.getLength() - 4);

            for (int b = 0; b < decompLength; b++) {
              if (exporter.available()) { // make sure we read the next bit of data, if required
                decompBytes[b] = (byte) exporter.read();
              }
            }
            //exporter.close();

            String tempFilename = Settings.getString("TempDirectory") + File.separatorChar + resource.getName() + ".uncompressed.snd";
            File tempPath = new File(tempFilename);
            FileManipulator tempFM = new FileManipulator(tempPath, true);
            tempFM.writeBytes(decompBytes);
            tempFM.close();

            // Make sure we don't delete the temporary files after the Read has finished
            SingletonManager.set("BulkExport_KeepTempFiles", true);

            // now that we know the frequency store the details as a WAV file

            // 2 - Frequency
            short frequency = ShortConverter.convertBig(new byte[] { decompBytes[4], decompBytes[5] });

            Resource_WAV_RawAudio resourceWAV = new Resource_WAV_RawAudio(tempPath, resource.getName(), 8, decompLength - 8);
            resourceWAV.setAudioProperties(frequency, (short) 8, (short) 1, false);
            resourceWAV.forceNotAdded(true);
            resources[i] = resourceWAV;

            TaskProgressManager.setValue(i);

          }
          else {
            // just SND
            Resource resource = resources[i];
            fm.seek(resource.getOffset() + 4);

            // 2 - Frequency
            short frequency = ShortConverter.changeFormat(fm.readShort());

            Resource_WAV_RawAudio resourceWAV = new Resource_WAV_RawAudio(path, resource.getName(), resource.getOffset() + 8, resource.getLength() - 8);
            resourceWAV.setAudioProperties(frequency, (short) 8, (short) 1, false);
            resources[i] = resourceWAV;

            TaskProgressManager.setValue(i);
          }
        }
        else if (fileCompressed[i]) {
          Resource resource = resources[i];
          fm.seek(resource.getOffset());

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          resource.setOffset(resource.getOffset() + 4);
          resource.setLength(resource.getLength() - 4);
          resource.setDecompressedLength(decompLength);
          TaskProgressManager.setValue(i);
        }
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
