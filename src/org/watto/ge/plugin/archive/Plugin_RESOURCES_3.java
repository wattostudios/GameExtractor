/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RESOURCES_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RESOURCES_3() {

    super("RESOURCES_3", "RESOURCES_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RAGE");
    setExtensions("resources", "streamed"); // MUST BE LOWER CASE
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

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      Exporter_Deflate exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      //
      //
      // FOR THIS PLUGIN, WE'RE GOING TO OPEN THE gameresources.resources FILE, READ THE WAVS FROM IT, AND ONLY DISPLAY THAT
      //
      //
      File resPath = new File(path.getParent() + File.separatorChar + "gameresources.resources");
      if (!resPath.exists()) {
        return null;
      }
      else if (resPath.equals(path)) {
        return null; // open with RESOURCE_2 plugin instead
      }

      FileManipulator fm = new FileManipulator(resPath, false);

      long arcSize = fm.getLength();

      //long sourceSize = path.length();
      String sourceFile = path.getName().toLowerCase();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Directory Offset
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown
      // 4 - null
      fm.relativeSeek(dirOffset);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      long[] sizes = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        boolean foundFile = false;

        // 4 - File ID Number
        fm.skip(4);

        // 4 - Data Type Name Length (LITTLE)
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Data Type Name
        //fm.skip(nameLength);
        String dataTypeName = fm.readString(nameLength);

        // 4 - Source Data Name Length (LITTLE)
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Source Data Name
        //fm.skip(nameLength);
        String sourceName = fm.readString(nameLength);

        // 4 - Filename Length (LITTLE) (can be null)
        nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength + 1); // +1 to allow nulls

        // X - Filename
        String filename = fm.readString(nameLength);

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        offsets[realNumFiles] = offset;

        // 4 - Decompressed Length
        long decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        sizes[realNumFiles] = length;

        // 4 - Extra Length [*24]
        int extraLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkRange(extraLength, 0, 100); // guess

        // X - Extra Data
        if (dataTypeName.equals("sample") && sourceName.endsWith(".wav")) {
          //System.out.println(dataTypeName + "\t" + sourceName + "\t" + filename);

          if (extraLength == 1) {
            if (sourceFile.equals("streamed.resources")) {
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              // 4 - Unknown
              fm.skip(16);

              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              fm.skip((extraLength * 24));
            }
          }
          else if (extraLength == 7) {
            // ENGLISH

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("english.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // FRENCH

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("french.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // ITALIAN

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("italian.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // GERMAN

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("german.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // SPANISH

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("spanish.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // RUSSIAN

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("russian.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }

            // JAPANESE

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(16);

            if (sourceFile.equals("japanese.streamed")) {
              // 4 - Source Offset
              offset = IntConverter.changeFormat(fm.readInt());

              // 4 - Source Length
              length = IntConverter.changeFormat(fm.readInt());

              foundFile = true;
            }
            else {
              // 4 - Source Offset
              // 4 - Source Length
              fm.skip(8);
            }
          }

        }
        else {
          fm.skip(extraLength * 24);
        }

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (LITTLE)
        // 4 - Unknown (LITTLE)
        fm.skip(20);

        if (length == 0 && filename.length() == 0) {
          filename = Resource.generateFilename(i);
        }

        filename += ".wav";

        if (foundFile) {
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource_WAV_RawAudio(path, filename, offset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);
      numFiles = realNumFiles;

      // go through and get the audio properties from the idmsa files

      int decompLength = 870;
      if (sourceFile.equals("streamed.resources")) {
        decompLength = 132;
      }

      fm.getBuffer().setBufferSize(decompLength);

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(offsets[i]);

        int size = (int) sizes[i];
        byte[] compBytes = fm.readBytes(size);
        byte[] decompBytes = new byte[decompLength];

        FileManipulator compFM = new FileManipulator(new ByteBuffer(compBytes));
        exporter.open(compFM, size, decompLength);
        for (int b = 0; b < decompLength; b++) {
          exporter.available();
          decompBytes[b] = (byte) exporter.read();
        }
        compFM.close();

        FileManipulator decompFM = new FileManipulator(new ByteBuffer(decompBytes));

        if (sourceFile.equals("streamed.resources")) {
          decompFM.seek(0x52);
        }
        else if (sourceFile.equals("english.streamed")) {
          decompFM.seek(0xE2);
        }
        else if (sourceFile.equals("french.streamed")) {
          decompFM.seek(0x145);
        }
        else if (sourceFile.equals("italian.streamed")) {
          decompFM.seek(0x1A8);
        }
        else if (sourceFile.equals("german.streamed")) {
          decompFM.seek(0x20B);
        }
        else if (sourceFile.equals("spanish.streamed")) {
          decompFM.seek(0x26E);
        }
        else if (sourceFile.equals("russian.streamed")) {
          decompFM.seek(0x2D1);
        }
        else if (sourceFile.equals("japanese.streamed")) {
          decompFM.seek(0x334);
        }

        // 2 - Codec ID
        short codec = decompFM.readShort();

        // 2 - Channels
        short channels = decompFM.readShort();

        // 4 - Frequency
        int frequency = decompFM.readInt();

        // 4 - BPS
        int bps = decompFM.readInt();

        // 2 - Align
        short align = decompFM.readShort();

        decompFM.close();

        Resource_WAV_RawAudio resource = (Resource_WAV_RawAudio) resources[i];
        resource.setAudioProperties(frequency, bps, channels);
        resource.setCodec(codec);
        resource.setBlockAlign(align);

        TaskProgressManager.setValue(i);
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("wav")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "FFMPEG_Audio_WAV");
      //return new Viewer_FFMPEG_Audio_WAV();
    }
    return null;
  }

}
