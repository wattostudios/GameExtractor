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

package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_AUDIOPKG_V18 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AUDIOPKG_V18() {

    super("AUDIOPKG_V18", "AUDIOPKG_V18");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Area 51");
    setExtensions("audiopkg"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("v1.8")) {
        rating += 25;
      }

      fm.skip(12);

      if (fm.readString(7).equals("Windows")) {
        rating += 25;
      }

      fm.skip(9);

      if (fm.readString(10).equals("mschaefgen")) {
        rating += 20;
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 16 - Version ("v1.8" + nulls to fill)
      // 16 - Operating System ("Windows" + nulls to fill)
      // 16 - Program ("mschaefgen" + nulls to fill)
      // 16 - Datestamp ("03/30/05 21:56" + nulls to fill)
      // 4 - Mram
      // 4 - Aram
      // 4 - Bits
      // 4 - Flags
      // 4 - Pitch
      // 4 - PitchVariance
      // 4 - Volume
      // 4 - VolumeVariance
      // 4 - VolumeCenter
      // 4 - VolumeLFE
      // 4 - VolumeDuck
      // 4 - UserData
      // 4 - ReplayDelay
      // 4 - LastPlay
      // 4 - Pan2d
      // 4 - Priority
      // 4 - EffectSend
      // 4 - NearFalloff
      // 4 - FarFalloff
      // 4 - RolloffCurve
      // 4 - NearDiffuse
      // 4 - FarDiffuse
      // 4 - PlayPercent
      // 4 - Pan 1
      // 4 - Pan 2
      // 4 - Pan 3
      // 4 - Pan 4
      // 4 - Pan 5
      // 4 - Pan 6
      // 4 - Pan 7
      // 4 - Pan 8
      // 4 - Pan 9
      fm.skip(192);

      // 4 - Number of Descriptors
      int numDescriptors = fm.readInt();
      FieldValidator.checkNumFiles(numDescriptors + 1);

      // 4 - Number of Identifiers
      int numIdentifiers = fm.readInt();
      FieldValidator.checkNumFiles(numIdentifiers + 1);

      // 4 - Descriptor Directory Length
      int descriptorDirLength = fm.readInt();
      FieldValidator.checkLength(descriptorDirLength, arcSize);

      // 4 - String Directory Length
      int stringDirLength = fm.readInt();
      FieldValidator.checkLength(stringDirLength, arcSize);

      // 4 - Lip Sync Directory Length
      int lipsyncDirLength = fm.readInt();
      FieldValidator.checkLength(lipsyncDirLength, arcSize);

      // 4 - Breakpoint Directory Length
      int breakpointDirLength = fm.readInt();
      FieldValidator.checkLength(breakpointDirLength, arcSize);

      // 4 - Music Data Length
      int musicDataDirLength = fm.readInt();
      FieldValidator.checkLength(musicDataDirLength, arcSize);

      // for (3)
      // 4 - Number of Sample Headers
      int numHot = fm.readInt();
      FieldValidator.checkNumFiles(numHot + 1);
      int numCold = fm.readInt();
      FieldValidator.checkNumFiles(numCold + 1);
      int numWarm = fm.readInt();
      FieldValidator.checkNumFiles(numWarm + 1);

      int numFiles = numHot + numCold + numWarm;
      FieldValidator.checkNumFiles(numFiles);

      // for (3)
      // 4 - Number of Sample Indices
      int numHotIndices = fm.readInt();
      FieldValidator.checkNumFiles(numHotIndices + 1);
      int numColdIndices = fm.readInt();
      FieldValidator.checkNumFiles(numColdIndices + 1);
      int numWarmIndices = fm.readInt();
      FieldValidator.checkNumFiles(numWarmIndices + 1);

      // for (3)
      // 4 - Compression Type (0=ADPCM, 1=PCM, 2=MP3)
      int hotCompression = fm.readInt();
      int coldCompression = fm.readInt();
      int warmCompression = fm.readInt();

      String hotCompressionString = "";
      if (hotCompression == 0) {
        hotCompressionString = ".adpcm";
      }
      else if (hotCompression == 1) {
        hotCompressionString = ".pcm";
      }
      else if (hotCompression == 2) {
        hotCompressionString = ".mp3";
      }

      String coldCompressionString = "";
      if (coldCompression == 0) {
        coldCompressionString = ".adpcm";
      }
      else if (coldCompression == 1) {
        coldCompressionString = ".pcm";
      }
      else if (coldCompression == 2) {
        coldCompressionString = ".mp3";
      }

      String warmCompressionString = "";
      if (warmCompression == 0) {
        warmCompressionString = ".adpcm";
      }
      else if (warmCompression == 1) {
        warmCompressionString = ".pcm";
      }
      else if (warmCompression == 2) {
        warmCompressionString = ".mp3";
      }

      // for (3)
      // 4 - Sample Entry Length (40)

      // 4 - null
      fm.skip(16);

      // STRING DIRECTORY
      fm.skip(stringDirLength);

      // MUSIC DATA
      fm.skip(musicDataDirLength);

      // LIPSYNC DATA
      fm.skip(lipsyncDirLength);

      // BREAKPOINT DATA
      fm.skip(breakpointDirLength);

      // IDENTIFIERS
      fm.skip(numIdentifiers * 8);

      // DESCRIPTOR OFFSETS
      fm.skip(numDescriptors * 4);

      // DESCRIPTOR DIRECTORY
      fm.skip(descriptorDirLength);

      // SAMPLE HEADER INDICES
      if (numHotIndices != 0) {
        fm.skip((numHotIndices + 1) * 2);
      }
      if (numColdIndices != 0) {
        fm.skip((numColdIndices + 1) * 2);
      }
      if (numWarmIndices != 0) {
        fm.skip((numWarmIndices + 1) * 2);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numHot; i++) {
        // 4 - Audio Ram Offset (null)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - LipSync Offset (-1)
        // 4 - Breakpoint Offset (-1)
        // 4 - Compression Method (0=Mono, 1=Non-Interleaved Stereo, 2=Mono Streamed, 3=Interleaved Stereo)
        // 4 - Number of Samples (Half File Length)
        // 4 - Audio Frequency (eg 32000)
        // 4 - Loop Start (0/-1)
        // 4 - Loop End (0/-1)
        fm.skip(28);

        String filename = "Hot " + Resource.generateFilename(i) + hotCompressionString;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      for (int i = 0; i < numCold; i++) {
        // 4 - Audio Ram Offset (null)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - LipSync Offset (-1)
        // 4 - Breakpoint Offset (-1)
        // 4 - Compression Method (0=Mono, 1=Non-Interleaved Stereo, 2=Mono Streamed, 3=Interleaved Stereo)
        // 4 - Number of Samples (Half File Length)
        // 4 - Audio Frequency (eg 32000)
        // 4 - Loop Start (0/-1)
        // 4 - Loop End (0/-1)
        fm.skip(28);

        String filename = "Cold " + Resource.generateFilename(i) + coldCompressionString;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      for (int i = 0; i < numWarm; i++) {
        // 4 - Audio Ram Offset (null)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - LipSync Offset (-1)
        // 4 - Breakpoint Offset (-1)
        // 4 - Compression Method (0=Mono, 1=Non-Interleaved Stereo, 2=Mono Streamed, 3=Interleaved Stereo)
        // 4 - Number of Samples (Half File Length)
        // 4 - Audio Frequency (eg 32000)
        // 4 - Loop Start (0/-1)
        // 4 - Loop End (0/-1)
        fm.skip(28);

        String filename = "Warm " + Resource.generateFilename(i) + warmCompressionString;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
