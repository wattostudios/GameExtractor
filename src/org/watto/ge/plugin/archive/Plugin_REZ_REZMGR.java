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
import java.util.Arrays;
import java.util.HashMap;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.WSPluginException;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.datatype.REZ_REZMGR_ResourceSorter;
import org.watto.ge.plugin.viewer.Viewer_REZ_REZMGR_DTX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_REZ_REZMGR extends ArchivePlugin {

  int i = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_REZ_REZMGR() {

    super("REZ_REZMGR", "Monolith Studios REZ");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("rez");
    setGames("Alien Vs Predator 2",
        "Army Ranger: Mogadishu",
        "Blood 2",
        "Contract JACK",
        "Die Hard Nakatomi Plaza",
        "Global Operations",
        "I, The Gangster",
        "KISS Psycho Circus: The Nightmare Child",
        "Marine Sharpshooter",
        "Marine Sharpshooter 2: Jungle Warfare",
        "Noone Lives Forever",
        "Noone Lives Forever 2",
        "Purge",
        "Sanity Aiken's Artifact",
        "Sentinel",
        "Shogo: Mobile Armor Division",
        "TerraWars: New York Invasion",
        "Tron 2.0",
        "World War 2 Normandy",
        "World War 2 Sniper - Call To Victory");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("dtx", "DTX Image", FileType.TYPE_IMAGE));

    setCanConvertOnReplace(true);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, long offset, long size, String directoryName) throws Exception {

    long currentPos = offset;
    long endPos = offset + size;
    int readLength = 0;
    while (currentPos < endPos) {
      fm.seek(currentPos);

      // 4 - EntryType
      int entryType = fm.readInt();

      // 4 - Data Offset
      offset = fm.readInt();

      // 4 - File Length
      long length = fm.readInt();

      // 4 - DateTime
      fm.skip(4);

      if (entryType == 1) {

        // X - filename (null)
        String dirFilename = fm.readNullString();
        FieldValidator.checkFilename(dirFilename);

        currentPos = (int) fm.getOffset();

        if (length > 0) {
          analyseDirectory(fm, path, resources, offset, length, directoryName + dirFilename + File.separator);
        }

      }
      else if (entryType == 0) {
        // 4 - ID number
        int fileID = fm.readInt();

        // 4 - Extension (reversed)
        String ext = StringConverter.reverse(fm.readNullString(4));

        // 4 - null
        fm.skip(4);

        // X - filename (null)
        String filename = directoryName + fm.readNullString();

        currentPos = (int) fm.getOffset() + 1;

        if (!ext.equals("")) {
          filename += "." + ext;
        }

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.addProperty("OriginalOffset", offset);
          resources[i] = resource;

          TaskProgressManager.setValue(readLength);
          readLength += length;
          i++;
        }
      }
      else {
        throw new WSPluginException("Invalid entry type");
      }

    }

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
      if (fm.readString(50).equals((char) 13 + (char) 10 + "RezMgr Version 1 Copyright (C) 1995 MONOLITH INC")) {
        rating += 50;
      }

      fm.skip(77);

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("msc") || extension.equalsIgnoreCase("sc") || extension.equalsIgnoreCase("vsh") || extension.equalsIgnoreCase("fcf") || extension.equalsIgnoreCase("fxf") || extension.equalsIgnoreCase("scr") || extension.equalsIgnoreCase("uif")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      i = 0;

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 127 - Header (13 10 'RezMgr Version 1 Copyright (C) 1995 MONOLITH INC.           ' 13 10 'LithTech Resource File                                      ' 13 10 26)
      //           | (13 10 'RezMgr Version 1 Copyright (C) 1995 MONOLITH INC.           ' 13 10 '                                                            ' 13 10 26)
      // 4 - Version (1)
      fm.skip(131);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - dirLength
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Empty
      // 4 - IdxOffset
      // 4 - DateTime
      // 4 - Empty
      // 4 - LongestFoldernameLength
      // 4 - LongestFilenameLength
      fm.skip(24);

      int numFiles = Archive.getMaxFiles(4);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, dirOffset, dirLength, "");
      resources = resizeResources(resources, i);

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long headerSize = 168;
      long directorySize = 0; // We'll read the directory size from the source archive, it's going to stay the same
      long fileDataSize = 0;
      for (int i = 0; i < numFiles; i++) {
        fileDataSize += resources[i].getDecompressedLength();
      }

      // Before we do anything, lets sort the resources by their original offsets, so we put them all back in the right order...
      REZ_REZMGR_ResourceSorter[] sorter = new REZ_REZMGR_ResourceSorter[numFiles];
      for (int i = 0; i < numFiles; i++) {
        sorter[i] = new REZ_REZMGR_ResourceSorter(resources[i]);
      }
      Arrays.sort(sorter);
      for (int i = 0; i < numFiles; i++) {
        resources[i] = sorter[i].getResource();
      }

      // Write Header Data

      // 2 - Line Break ((byte)13,10)
      // 60 - Header (RezMgr Version 1 Copyright (C) 1995 MONOLITH INC.           )
      // 2 - Line Break ((byte)13,10)
      // 60 - Description (LithTech Resource File                                      ) // can also sometimes be all spaces
      // 2 - Line Break ((byte)13,10)
      // 1 - Unknown (26)
      // 4 - Version (1)
      fm.writeBytes(src.readBytes(131));

      // 4 - Root Directory Offset
      int srcRootOffset = src.readInt();

      // 4 - Root Directory Length
      int srcRootLength = src.readInt();

      // 4 - Unknown
      int srcUnknown = src.readInt();

      // 4 - Sub-Directories Offset
      int srcSubOffset = src.readInt();

      directorySize = src.getLength() - srcSubOffset - srcRootLength;

      int rootOffset = (int) (headerSize + fileDataSize + directorySize);
      int subOffset = (int) (headerSize + fileDataSize);

      fm.writeInt(rootOffset);
      fm.writeInt(srcRootLength);
      fm.writeInt(srcUnknown);
      fm.writeInt(subOffset);

      // 4 - Timestamp
      // 4 - null
      // 4 - Longest Directory Name Length
      // 4 - Longest Filename Length
      // 4 - null
      // 1 - Unknown (1)
      fm.writeBytes(src.readBytes(21));

      // Write Files
      // as we're writing the files, want to map the original offsets to the Resources they represent
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      HashMap<Long, Long> offsetMap = new HashMap<Long, Long>(numFiles);
      HashMap<Long, Resource> resourceMap = new HashMap<Long, Resource>(numFiles);
      //write(resources, fm);
      for (int i = 0; i < resources.length; i++) {
        long newOffset = fm.getOffset();

        Resource resource = resources[i];
        write(resource, fm);

        long originalOffset = Long.parseLong(resource.getProperty("OriginalOffset"));
        offsetMap.put(originalOffset, newOffset);
        resourceMap.put(originalOffset, resource);

        TaskProgressManager.setValue(i);
      }

      // read all the sub-directories, work out the order they're in, and the sizes
      src.relativeSeek(srcRootOffset);

      replaceOffsets = new long[1000]; // guess max
      replaceLengths = new long[1000]; // guess max
      subdirCount = 0;

      analyseDirectoryForReplace(src, srcRootOffset, srcRootLength);

      // now we have all the offsets and lengths of the subdirectories in the src, so we know how to rebuild the sub-directories

      // first, find the order of the subdirectories
      long[] replaceOffsetsSorted = new long[subdirCount];
      System.arraycopy(replaceOffsets, 0, replaceOffsetsSorted, 0, subdirCount);
      Arrays.sort(replaceOffsetsSorted);

      long[] replaceLengthsSorted = new long[subdirCount]; // will be populated down further

      // work out the subdirectory offset adjustment (ie because the file size is larger/smaller with the replaced files in it)
      int dirOffsetAdjustment = subOffset - srcSubOffset;

      // go through each sub-directory, write them out, with the adjusted subdiroffsets and file offsets/lengths

      // Write Directories
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int realNumFiles = 0;
      for (int d = 0; d < subdirCount; d++) {
        long dirOffset = replaceOffsetsSorted[d];

        // find which position in the original replaceOffsets/replaceLengths match with the sorted offset
        for (int s = 0; s < subdirCount; s++) {
          if (replaceOffsets[s] == dirOffset) {
            // found the match
            replaceLengthsSorted[d] = replaceLengths[s];
            break;
          }
        }

        // if this isn't the first directory, write the null padding between sub-directories
        if (d != 0) {
          // work out how much padding
          int paddingSize = (int) (replaceOffsetsSorted[d] - replaceOffsetsSorted[d - 1] - replaceLengthsSorted[d - 1]);

          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }

        long replaceOffset = replaceOffsetsSorted[d];
        long replaceLength = replaceLengthsSorted[d];

        src.relativeSeek(replaceOffset);

        long currentPos = replaceOffset;
        long endPos = replaceOffset + replaceLength;
        while (currentPos < endPos) {

          // 4 - EntryType
          int entryType = src.readInt();
          fm.writeInt(entryType);

          if (entryType == 1) {
            // directory

            // 4 - SubDirectory Offset
            int srcOffset = src.readInt();
            fm.writeInt(srcOffset + dirOffsetAdjustment);

            // 4 - SubDirectory Length
            // 4 - DateTime
            fm.writeBytes(src.readBytes(8));

            // X - Directory Name
            // 1 - null Directory Name Terminator
            String dirFilename = src.readNullString();
            fm.writeString(dirFilename);
            fm.writeByte(0);

            currentPos = (int) src.getOffset();
          }
          else if (entryType == 0) {
            // file

            // 4 - File Offset
            long srcOffset = src.readInt();

            if (srcOffset == 0) {
              // hopefully the transition between SubDirectories and Files
              fm.writeInt(0);

              // 4 - File Length (should be null)
              fm.writeBytes(src.readBytes(4));
            }
            else {
              // should be a real file

              // find the Resource matching this offset
              Resource resource = resourceMap.get(srcOffset);
              long offset = offsetMap.get(srcOffset);

              if (resource == null) {
                ErrorLogger.log("[REZ_REZMGR] Couldn't find file to replace: " + srcOffset);
                return; // early exit
              }

              fm.writeInt(offset);

              // 4 - File Length
              src.skip(4);
              fm.writeInt(resource.getDecompressedLength());

              TaskProgressManager.setValue(realNumFiles);
              realNumFiles++;
            }

            // 4 - DateTime
            // 4 - ID number
            // 4 - Extension (reversed)
            // 4 - null
            fm.writeBytes(src.readBytes(16));

            // X - Filename
            // 2 - null Filename Terminator
            String filename = src.readNullString();
            src.skip(1);

            fm.writeString(filename);
            fm.writeShort(0);

            currentPos = (int) src.getOffset();
          }
          else {
            ErrorLogger.log("[REZ_REZMGR] Invalid entry type, for some reason: " + entryType);
            return; // early exit
          }

        }
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  long[] replaceOffsets = null;

  long[] replaceLengths = null;

  int subdirCount = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectoryForReplace(FileManipulator fm, long offset, long size) throws Exception {

    replaceOffsets[subdirCount] = offset;
    replaceLengths[subdirCount] = size;
    subdirCount++;

    long currentPos = offset;
    long endPos = offset + size;
    while (currentPos < endPos) {
      fm.seek(currentPos);

      // 4 - EntryType
      int entryType = fm.readInt();

      // 4 - Data Offset
      offset = fm.readInt();

      // 4 - File Length
      long length = fm.readInt();

      // 4 - DateTime
      fm.skip(4);

      if (entryType == 1) {

        // X - filename (null)
        fm.readNullString();

        currentPos = (int) fm.getOffset();

        if (length > 0) {
          analyseDirectoryForReplace(fm, offset, length);
        }

      }
      else if (entryType == 0) {
        // found the files, no more sub-directories, return now
        return;
      }
      else {
        // not sure, something invalid
        return;
      }

    }

  }

  /**
   **********************************************************************************************
   When replacing dtx images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a dtx image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {

    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("dtx")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("dtx")) {
        // if the fileToReplace already has a dtx extension, assume it's already a compatible dtx file and doesn't need to be converted
        return fileToReplaceWith;
      }

      //
      //
      // if we're here, we want to scan to see if we can find an Image ViewerPlugin that can read the file into an ImageResource,
      // which we can then convert into a dtx using plugin Viewer_REZ_REZMGR_DTX
      //
      //

      // 1. Open the file
      FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return fileToReplaceWith;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(fileToReplaceWith, false);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file

        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);
        if (imagePreviewPanel != null) {
          // found a previewer
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null) {
        // no plugins were able to open this file successfully
        return fileToReplaceWith;
      }

      //
      //
      // If we're here, we have a rendered image, so we want to convert it into DTX
      //
      //
      Viewer_REZ_REZMGR_DTX converterPlugin = new Viewer_REZ_REZMGR_DTX();

      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    else {
      return fileToReplaceWith;
    }
  }

}