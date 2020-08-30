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

package org.watto.ge.plugin.viewer;

import java.io.File;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_OggVorbisAudio;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.UE4Helper;
import org.watto.ge.helper.UE4Helper_Short;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PAK_38;
import org.watto.ge.plugin.archive.Plugin_UE4_6;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.resource.Resource_PAK_38;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.Task_ExportFiles;

/**
 **********************************************************************************************
 * Plugin for loading and previewing Ogg Vorbis audio files. Requires: - JOrbis 0.013+
 **********************************************************************************************
 **/
public class Viewer_UE4_SoundWave_6 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_UE4_SoundWave_6() {
    super("UE4_SoundWave_6", "Unreal Engine 4 SoundWave (Version 6)");
    setExtensions("soundwave"); // lowercase

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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      // PAK_38 for the original archive, UE4_6 for exported uasset files
      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_PAK_38 || readPlugin instanceof Plugin_UE4_6) {
        rating += 50;
      }

      // Check for a UE4 archives with Version = 6
      int helperRating = UE4Helper.getMatchRating(fm, 6);
      if (helperRating > rating) {
        rating = helperRating;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  If a related resource is found with the same name and matching fileExtension, it is extracted
  either to a bytebuffer or to a temporary directory
  **********************************************************************************************
  **/
  public FileManipulator extractRelatedResource(FileManipulator fm, String fileExtension, boolean extractToMemory) {
    try {

      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected != null && selected instanceof Resource_PAK_38) {

        String uexpName = selected.getName();
        int dotPos = uexpName.lastIndexOf(".uasset");
        if (dotPos > 0) {
          uexpName = uexpName.substring(0, dotPos + 1) + fileExtension;

          // search for the related file
          Resource[] resources = ((Resource_PAK_38) selected).getRelatedResources();
          int numResources = resources.length;

          for (int i = 0; i < numResources; i++) {
            if (resources[i].getName().equals(uexpName)) {
              // found the uexp file
              Resource relatedResource = resources[i];

              // extract it
              if (extractToMemory) {
                // to a bytebuffer

                fm.close();

                /*
                fm = new FileManipulator(relatedResource.getSource(), false);
                fm.seek(relatedResource.getOffset());
                byte[] fileData = fm.readBytes((int) relatedResource.getLength());
                
                fm.close();
                fm = new FileManipulator(new ByteBuffer(fileData));
                */
                byte[] fileData = new byte[(int) relatedResource.getDecompressedLength()];
                fm = new FileManipulator(new ByteBuffer(fileData));
                relatedResource.extract(fm);
                fm.seek(0);

                return fm;
              }
              else {
                // to a file
                File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

                Task_ExportFiles exportTask = new Task_ExportFiles(directory, relatedResource);
                exportTask.setShowPopups(false);
                exportTask.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
                exportTask.redo();

                File path = relatedResource.getExportedPath();

                fm.close();
                fm = new FileManipulator(path, false);

                return fm;
              }

            }
          }
        }
      }

      return null;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }

  }

  /**
  **********************************************************************************************
  Reads Version 6. If it finds version 5, it passes to readVersion5() instead
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      if (UE4Helper.getVersion() == 5) {
        return readVersion5(fm);
      }

      // FIRST UP, WE NEED TO READ THROUGH TO DISCOVER IF THIS IS A SoundWave UASSET OR NOT.
      // IF NOT, WE RETURN NULL AT THAT POINT, HANDING OVER TO ANOTHER VIEWER PLUGIN
      long arcSize = fm.getLength();
      long uassetSize = arcSize; // used to allow proper seeking when the file is split into separate uasset + uexp files

      // 4 - Unreal Header (193,131,42,158)
      // 4 - Version (6) (XOR with 255)
      // 16 - null
      // 4 - File Directory Offset?
      // 4 - Unknown (5)
      // 4 - Package Name (None)
      // 4 - null
      // 1 - Unknown (128)
      fm.skip(41);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      FieldValidator.checkNumFiles(nameCount);

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 8 - null
      // 4 - Number Of Exports
      // 4 - Exports Directory Offset
      fm.skip(16);

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      // 16 - null
      // 4 - [optional] null
      // 16 - GUID Hash
      fm.skip(32);

      // 4 - Unknown (1)
      if (fm.readInt() != 1) { // this is to skip the OPTIONAL 4 bytes in MOST circumstances
        fm.skip(4);
      }

      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - Padding Offset
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 8 - null
      fm.skip(68);

      // 4 - Number of ???
      int numToSkip = fm.readInt();
      if (numToSkip > 0 && numToSkip < 10) {
        // 4 - Unknown
        fm.skip(numToSkip * 4);
      }

      // 4 - Unknown (-1)
      fm.skip(4);

      // 4 - Files Data Offset
      long filesDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(filesDirOffset, arcSize + 1);

      // Read the Names Directory
      fm.seek(nameDirOffset);
      UE4Helper.readNamesDirectory(fm, nameCount);

      // Read the Import Directory
      fm.seek(importDirOffset);
      UnrealImportEntry[] imports = UE4Helper.readImportDirectory(fm, importCount);

      int numFiles = importCount;

      boolean foundSoundWaveClass = false;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];

        if (entry.getType().equals("Class") && entry.getName().equals("SoundWave")) {
          // found a Class, and its a Texture2D, so we're happy
          foundSoundWaveClass = true;
          break;
        }

      }

      // If we haven't found a SoundWave Class, return null and let another ViewerPlugin handle it
      if (!foundSoundWaveClass) {
        return null;
      }

      // NOW THAT HE HAVE A SoundWave UASSET, WE CAN GO TO THE FILE DATA, SKIP OVER THE UNREAL PROPERTIES,
      // AND FIND THE AUDIO DATA FOR PLAYING

      // First up, see if the uassets file has the Exports in it, or if it's been put in a separate *.uexp file
      boolean inUExp = false;
      if (filesDirOffset == arcSize || filesDirOffset + 8 == arcSize || filesDirOffset + 12 == arcSize) {
        // probably in a separate *.uexp file - see if we can find one
        FileManipulator extractedFM = extractRelatedResource(fm, "uexp", true);
        //FileManipulator extractedFM = extractRelatedResource(fm, "uexp", false);
        if (extractedFM != null) {
          fm = extractedFM;
          filesDirOffset = 0; // so when we seek down further, it goes to the start of the uexp file
          arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
          inUExp = true;
        }
      }

      // still keep reading the same file
      fm.seek(filesDirOffset);
      UE4Helper.readProperties(fm); // discard all this - we don't need it, we just need to get passed it all to find the image data

      // 4 - null
      fm.skip(4);

      // 2 - Flags (1/3)
      // 2 - Flags (1/0)
      // 4 - Unknown (1)
      fm.skip(8);

      // 8 - Type ID (points to "OGG" for a Sound File)
      long typeID = fm.readLong();
      String type = UE4Helper.getName(typeID);

      if (type == null || type.equals("OGG") || type.startsWith("OGG")) { // some files have OGG, others have OGG10025600-1-1-1-1-1
        // 4 - Unknown (72)
        fm.skip(4);

        // 4 - File Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Data Length
        fm.skip(4);

        // 4 - File Data Offset (relative to the start of the UASSET file, *NOT* the UEXP file)
        int extraData = (int) (fm.readInt() - fm.getOffset());
        if (inUExp) {
          extraData -= uassetSize;
        }

        // X - [OPTIONAL] - extra data
        fm.skip(extraData);

        // X - File Data (OGG Vorbis)
        // 20 - CRC or something

        // Extract it to a temporary file, then call the OGG preview panel
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        File tempOggFile = new File(new File(Settings.get("TempDirectory")).getAbsolutePath() + File.separatorChar + selected.getName() + ".ogg");
        tempOggFile = FilenameChecker.correctFilename(tempOggFile);

        FileManipulator oggFM = new FileManipulator(tempOggFile, true, length);
        for (int i = 0; i < length; i++) {
          oggFM.writeByte(fm.readByte());
        }
        oggFM.close();

        fm.close();

        PreviewPanel_OggVorbisAudio preview = new PreviewPanel_OggVorbisAudio(tempOggFile);
        return preview;

      }

      System.out.println("[Viewer_UE4_SoundWave_6] Unknown SoundWave Format: " + type);
      return null;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel readVersion5(FileManipulator fm) {
    try {

      // FIRST UP, WE NEED TO READ THROUGH TO DISCOVER IF THIS IS A SoundWave UASSET OR NOT.
      // IF NOT, WE RETURN NULL AT THAT POINT, HANDING OVER TO ANOTHER VIEWER PLUGIN
      long arcSize = fm.getLength();
      long uassetSize = arcSize; // used to allow proper seeking when the file is split into separate uasset + uexp files

      // 4 - Unreal Header (193,131,42,158)
      // 4 - Version (6) (XOR with 255)
      // 16 - null
      // 4 - File Directory Offset?
      // 4 - Unknown (5)
      // 4 - Package Name (None)
      // 4 - null
      // 1 - Unknown (128)
      fm.skip(41);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      FieldValidator.checkNumFiles(nameCount);

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 8 - null
      // 4 - Number Of Exports
      // 4 - Exports Directory Offset
      fm.skip(16);

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      long filesDirOffset = 0;
      //   8 - Unknown
      fm.skip(8);

      //   8 - Files Data Offset
      filesDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(filesDirOffset, arcSize + 1);

      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - null
      // 4 - Files Data Offset
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 8 - null
      // 4 - Number of ???
      // for each ???
      //   4 - Unknown

      // Read the Names Directory
      fm.seek(nameDirOffset);
      UE4Helper_Short.readNamesDirectory(fm, nameCount);

      // Read the Import Directory
      fm.seek(importDirOffset);
      UnrealImportEntry[] imports = UE4Helper_Short.readImportDirectory(fm, importCount);

      int numFiles = importCount;

      boolean foundSoundWaveClass = false;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];

        if (entry.getType().equals("Class") && entry.getName().equals("SoundWave")) {
          // found a Class, and its a Texture2D, so we're happy
          foundSoundWaveClass = true;
          break;
        }

      }

      // If we haven't found a SoundWave Class, return null and let another ViewerPlugin handle it
      if (!foundSoundWaveClass) {
        return null;
      }

      // NOW THAT HE HAVE A SoundWave UASSET, WE CAN GO TO THE FILE DATA, SKIP OVER THE UNREAL PROPERTIES,
      // AND FIND THE AUDIO DATA FOR PLAYING

      // First up, see if the uassets file has the Exports in it, or if it's been put in a separate *.uexp file
      boolean inUExp = false;
      if (filesDirOffset == arcSize || filesDirOffset + 8 == arcSize) {
        // probably in a separate *.uexp file - see if we can find one
        FileManipulator extractedFM = extractRelatedResource(fm, "uexp", true);
        //FileManipulator extractedFM = extractRelatedResource(fm, "uexp", false);
        if (extractedFM != null) {
          fm = extractedFM;
          filesDirOffset = 0; // so when we seek down further, it goes to the start of the uexp file
          arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
          inUExp = true;
        }
      }

      // still keep reading the same file
      fm.seek(filesDirOffset);

      // 4 - null
      fm.skip(4);

      UE4Helper_Short.readProperties(fm); // discard all this - we don't need it, we just need to get passed it all to find the image data

      // 4 - null
      fm.skip(4);

      // 2 - Flags (1/3)
      // 2 - Flags (1/0)
      fm.skip(4);

      // 8 - Type ID (points to "OGG" for a Sound File)
      long typeID = fm.readLong();
      String type = UE4Helper_Short.getName(typeID);
      if (type == null) {
        // just in case there was another 4-byte field in there somewhere, let's try to correct that
        typeID >>= 32;
        type = UE4Helper_Short.getName(typeID);
        if (type != null) {
          fm.skip(4); // to get it back to the correct spot
        }
      }

      if (type.equals("OGG")) {
        // X - fields until the next one
        // 4 - Unknown (72)
        for (int f = 0; f < 10; f++) {
          if (fm.readInt() == 72) {
            break;
          }
        }

        // 4 - File Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Data Length
        fm.skip(4);

        // 4 - File Data Offset (relative to the start of the UASSET file, *NOT* the UEXP file)
        int extraData = (int) (fm.readInt() - fm.getOffset());
        if (inUExp) {
          extraData -= uassetSize;
        }

        // X - [OPTIONAL] - extra data
        fm.skip(extraData);

        // X - File Data (OGG Vorbis)
        // 20 - CRC or something

        // Extract it to a temporary file, then call the OGG preview panel
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        File tempOggFile = new File(new File(Settings.get("TempDirectory")).getAbsolutePath() + File.separatorChar + selected.getName() + ".ogg");
        tempOggFile = FilenameChecker.correctFilename(tempOggFile);

        FileManipulator oggFM = new FileManipulator(tempOggFile, true, length);
        for (int i = 0; i < length; i++) {
          oggFM.writeByte(fm.readByte());
        }
        oggFM.close();

        fm.close();

        PreviewPanel_OggVorbisAudio preview = new PreviewPanel_OggVorbisAudio(tempOggFile);
        return preview;

      }

      System.out.println("[Viewer_UE4_SoundWave_6] Unknown SoundWave Format: " + type);
      return null;

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