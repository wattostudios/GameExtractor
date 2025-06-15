/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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

import org.watto.Language;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.ge.plugin.viewer.Viewer_ARC_14_TXFL_TXFL;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_14 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_14() {

    super("ARC_14", "ARC_14");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("The Urbz: Sims in the City",
        "The Sims 2");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

    setCanConvertOnReplace(true);

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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Unknown (65536)
      if (fm.readInt() == 65536) {
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

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      setCanScanForFileTypes(false);

      String extension = null;
      String arcName = path.getName().toLowerCase();
      boolean audio = false;
      if (arcName.equals("animatio.arc")) {
        extension = ".ani";
      }
      else if (arcName.equals("ambients.arc")) {
        extension = ".amb";
      }
      else if (arcName.equals("audiostr.arc")) {
        extension = ".aud";
      }
      else if (arcName.equals("characte.arc")) {
        extension = ".chr";
      }
      else if (arcName.equals("datasets.arc")) {
        extension = ".set";
      }
      else if (arcName.equals("edithtre.arc")) {
        extension = ".edt";
      }
      else if (arcName.equals("effectsa.arc")) {
        extension = ".efa";
      }
      else if (arcName.equals("effectse.arc")) {
        extension = ".efe";
      }
      else if (arcName.equals("emitters.arc")) {
        extension = ".emt";
      }
      else if (arcName.equals("flashes.arc")) {
        extension = ".big";
      }
      else if (arcName.equals("fonts.arc")) {
        extension = ".fnt";
      }
      else if (arcName.equals("levels.arc")) {
        extension = ".lvl";
      }
      else if (arcName.equals("models.arc")) {
        extension = ".mdl";
      }
      else if (arcName.equals("movies.arc")) {
        extension = ".xmv";
      }
      else if (arcName.equals("quickdat.arc")) {
        extension = ".dat";
      }
      else if (arcName.equals("samples.arc") || arcName.startsWith("samples")) {
        audio = true;
        extension = ".wav";
      }
      else if (arcName.equals("shaders.arc")) {
        extension = ".shd";
      }
      else if (arcName.equals("soundeve.arc")) {
        extension = ".sev";
      }
      else if (arcName.equals("soundtra.arc")) {
        extension = ".str";
      }
      else if (arcName.equals("textures.arc")) {
        extension = ".txfl";
      }
      else if (arcName.equals("binaries.arc")) {
        // do nothing - already contains file extensions
      }
      else {
        setCanScanForFileTypes(true);
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        if (extension != null) {
          filename += extension;
        }

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter

        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      if (audio) {
        // go through the archive, find the audio frequencies, and set the as WAV files
        fm.getBuffer().setBufferSize(29); // small quick reads

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];

          fm.seek(resource.getOffset() + 4);

          // 4 - Frequency
          int frequency = fm.readInt();

          Resource_WAV_RawAudio resourceWAV = new Resource_WAV_RawAudio(path, resource.getName(), resource.getOffset() + 29, resource.getDecompressedLength() - 29);
          resourceWAV.setAudioProperties(frequency, (short) 4, (short) 1, true);
          resourceWAV.setCodec((short) 0x0069);
          //resource.setBlockAlign((short) align);
          resources[i] = resourceWAV;
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

      // For this archive, lets read in all the source details first, store the items we need, then we'll rebuild the archive.
      // This is partly because we want to see whether the archive has padding or not.

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      // 4 - Details Directory Offset
      int srcDirOffset = src.readInt();

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?
      byte[] headerBytes = src.readBytes(12);

      src.seek(srcDirOffset);

      // 4 - Number Of Files
      src.skip(4);

      int[] fileHashes1 = new int[numFiles];
      long[] fileHashes2 = new long[numFiles];
      int[] originalLengths = new int[numFiles];
      int[] originalOffsets = new int[numFiles];
      String[] names = new String[numFiles];
      int paddingSize = 0;

      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fileHashes1[i] = src.readInt();

        // 4 - File Offset
        if (i == 0) {
          int firstOffset = src.readInt();
          if (firstOffset != 16) {
            paddingSize = firstOffset;
          }
          originalOffsets[i] = firstOffset;
        }
        else {
          //src.skip(4);
          originalOffsets[i] = src.readInt();
        }

        // 4 - File Length
        //src.skip(4);
        originalLengths[i] = src.readInt();

        // X - Filename
        // 1 - null Filename Terminator
        names[i] = src.readNullString();

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fileHashes2[i] = src.readLong();
      }

      // Now we have the padding size, and all the hash details that we need.
      // So, lets build the new archive.

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 16;
      if (paddingSize != 0) {
        dirOffset = paddingSize;
      }
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (!resource.isReplaced()) {
          dirOffset += originalLengths[i]; // mainly for WAV files, so we pick up the original length if the file hasn't changed
        }
        else {
          dirOffset += resource.getDecompressedLength();
        }

        if (paddingSize != 0) {
          dirOffset += calculatePadding(dirOffset, paddingSize);
        }
      }

      // Write Header Data

      // 4 - Details Directory Offset
      fm.writeInt(dirOffset);

      // 4 - Unknown (65536)
      // 4 - Hash/CRC?
      // 4 - Hash/CRC?
      fm.writeBytes(headerBytes);

      // [OPTIONAL] X - Padding to a multiple of 4096 bytes
      for (int p = 16; p < paddingSize; p++) { // if paddingSize == 0, this loop won't run
        fm.writeByte(0);
      }

      // Write Files
      long[] offsets = new long[numFiles];
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        offsets[i] = fm.getOffset();

        if (resource.isReplaced()) {
          // add the file normally (uncompressed)

          // X - File Data
          write(resource, fm);
        }
        else {
          // add the original file, as copied from the originalOffset and originalLength
          src.seek(originalOffsets[i]);
          int length = originalLengths[i];

          int blockSize = src.getBuffer().getBufferSize();
          while (length > 0) {
            int sizeToCopy = length;
            if (sizeToCopy > blockSize) {
              sizeToCopy = blockSize;
            }

            fm.writeBytes(src.readBytes(sizeToCopy));
            length -= sizeToCopy;
          }
        }

        if (paddingSize != 0) {

          long length = resource.getDecompressedLength();

          if (!resource.isReplaced()) {
            length = originalLengths[i]; // mainly for WAV files, so we pick up the original length if the file hasn't changed
          }

          // [OPTIONAL] X - Padding to a multiple of 4096 bytes
          int padding = calculatePadding(length, paddingSize);
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }

        TaskProgressManager.setValue(i);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // 4 - Number of Files
      fm.writeInt(numFiles);

      long[] offsetsForIND = new long[numFiles];
      long[] lengthsForIND = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        if (!resource.isReplaced()) {
          length = originalLengths[i]; // mainly for WAV files, so we pick up the original length if the file hasn't changed
        }

        // 4 - Hash?
        fm.writeInt(fileHashes1[i]);

        // 4 - File Offset
        fm.writeInt(offsets[i]);
        offsetsForIND[i] = offsets[i];

        // 4 - File Length
        fm.writeInt(length);
        lengthsForIND[i] = length;

        // X - Filename
        fm.writeString(names[i]);

        // 1 - null Filename Terminator
        fm.writeByte(0);

        // 8 - Hash/CRC? (maybe TGI like in The Sims?)
        fm.writeLong(fileHashes2[i]);

      }

      // Now update the corresponding IND file with the new offsets/lengths
      File indexFile = findIND(src.getFile());
      if (indexFile != null) {
        updateIND(indexFile, src.getFile(), path, offsetsForIND, lengthsForIND);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   Finds the IND file that contains this archive
   **********************************************************************************************
   **/
  public File findIND(File sourceFile) {
    try {
      // get all the index*.ind files in the current directory
      File parentDirectory = sourceFile.getParentFile();
      if (parentDirectory == null) {
        return null;
      }

      File[] fileList = parentDirectory.listFiles();
      int numFiles = fileList.length;

      for (int f = 0; f < numFiles; f++) {
        File currentFile = fileList[f];
        String currentFileName = currentFile.getName();
        if (currentFileName.startsWith("index") && currentFileName.endsWith(".ind")) {
          // found an index file

          FileManipulator fm = new FileManipulator(currentFile, false);

          // 4 - Number of Archives in this IND [/2]
          int numArchives = fm.readInt() / 2;
          int[] nameOffsets = new int[numArchives];
          for (int a = 0; a < numArchives; a++) {
            // 4 - Archive Name Offset
            int nameOffset = fm.readInt();
            FieldValidator.checkOffset(nameOffset, currentFile.length());
            nameOffsets[a] = nameOffset;

            // 4 - Archive Directory Offset (offset to the NumberOfFiles field)
            fm.skip(4);
          }

          for (int a = 0; a < numArchives; a++) {
            fm.relativeSeek(nameOffsets[a]);

            // X - Archive Name (need to convert to lowercase, crop to 8 chars, and append ".arc" to get the matching filename)
            // 1 - null Name Terminator
            String arcName = fm.readNullString().toLowerCase();
            if (arcName.length() > 8) {
              arcName = arcName.substring(0, 8);
            }
            arcName += ".arc";

            if (arcName.equals(sourceFile.getName())) {
              // found the Index file that contains this archive data
              fm.close();
              return currentFile;
            }
          }

          fm.close();
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
   Generates a new IND file with the updated offsets and lengths
   **********************************************************************************************
   **/
  public void updateIND(File indexFile, File sourceFile, File newFile, long[] offsets, long[] lengths) {
    try {

      /*
      // First, lets generate a new index file, with the next numerical value
      boolean foundFilename = false;
      String newFilename = indexFile.getParent() + File.separatorChar + "index";
      for (int i = 1; i < 100; i++) {
        if (new File(newFilename + i + ".ind").exists()) {
          // file exists, look for the next one
          continue;
        }
      
        // found a file that doesn't exist - store the filename
        newFilename += i + ".ind";
        foundFilename = true;
        break;
      }
      
      if (!foundFilename) {
        // can't generate a new index file
        return;
      }
      */
      String newFilename = indexFile.getAbsolutePath() + ".updated" + System.currentTimeMillis();

      FileManipulator fm = new FileManipulator(new File(newFilename), true);
      FileManipulator src = new FileManipulator(indexFile, false);

      long arcSize = src.getLength();

      // 4 - Number of Archives in this IND [/2]
      int numArchives = src.readInt();
      fm.writeInt(numArchives);

      numArchives /= 2;

      int[] nameOffsets = new int[numArchives];
      int[] dirOffsets = new int[numArchives];
      for (int a = 0; a < numArchives; a++) {
        // 4 - Archive Name Offset
        int nameOffset = src.readInt();
        FieldValidator.checkOffset(nameOffset, arcSize);
        nameOffsets[a] = nameOffset;
        fm.writeInt(nameOffset);

        // 4 - Archive Directory Offset (offset to the NumberOfFiles field)
        int dirOffset = src.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);
        dirOffsets[a] = dirOffset;
        fm.writeInt(dirOffset);
      }

      // 4 - IND File Length
      fm.writeBytes(src.readBytes(4));

      for (int a = 0; a < numArchives; a++) {
        int nameOffset = nameOffsets[a];

        // catch up to the right place, just in case
        int dataMismatch = nameOffset - (int) src.getOffset();
        if (dataMismatch > 0) {
          fm.writeBytes(src.readBytes(dataMismatch));
        }

        // X - Archive Name (need to convert to lowercase, crop to 8 chars, and append ".arc" to get the matching filename)
        // 1 - null Name Terminator
        String archiveName = src.readNullString();
        fm.writeString(archiveName);
        fm.writeByte(0);
        String arcName = archiveName.toLowerCase();
        if (arcName.length() > 8) {
          arcName = arcName.substring(0, 8);
        }
        arcName += ".arc";

        boolean updatingThisArchive = false;
        if (arcName.equals(sourceFile.getName())) {
          // found the contents that we need to update
          updatingThisArchive = true;
        }

        // 0-3 - null Padding to a multiple of 4 bytes
        int dirOffset = dirOffsets[a];

        dataMismatch = dirOffset - (int) src.getOffset();
        if (dataMismatch > 0) {
          fm.writeBytes(src.readBytes(dataMismatch));
        }

        // 4 - Number of Files in the Archive
        int srcNumFiles = src.readInt();
        FieldValidator.checkNumFiles(srcNumFiles + 1); // allow empty archives
        fm.writeInt(srcNumFiles);

        if (!updatingThisArchive) {
          // just copy all the content exactly as it is

          // for each file in this archive
          //   4 - Hash

          // for each file in this archive
          //   4 - File Offset
          //   4 - File Length
          fm.writeBytes(src.readBytes(srcNumFiles * 12));

        }
        else {
          // this is the content we need to update

          // for each file in this archive
          //   4 - Hash
          fm.writeBytes(src.readBytes(srcNumFiles * 4));

          // for each file in this archive
          //   4 - File Offset
          //   4 - File Length
          for (int i = 0; i < srcNumFiles; i++) {
            fm.writeInt(offsets[i]);
            fm.writeInt(lengths[i]);
            src.skip(8);
          }
        }

      }

      fm.close();
      src.close();

    }
    catch (Throwable t) {
      logError(t);
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

    if (headerInt1 == 1279678548) {
      return "txfl";
    }
    else if (headerInt2 == 22050 || headerInt2 == 11025 || headerInt2 == 44100) {
      return "wav_raw";
    }
    else if (headerInt1 == 1179076930) {
      return "big";
    }

    // THE BELOW ARE FROM THE SIMS 2 [XBOX]
    else if (headerInt2 == 1396920148) {
      return "amb"; // TOCS in the Ambients files
    }
    else if (headerInt2 == 1095649613) {
      return "ani"; // MINA in the Animatio files
    }
    else if (headerInt2 == 1128813123) {
      return "chr"; // CRHC in the Characte files
    }
    else if (headerInt2 == 1146377044) {
      return "chr"; // TSTD in the Datasets files
    }
    else if (headerInt2 == 1162105928) {
      return "edt"; // HTDE in the Edithtre files
    }
    else if (headerInt2 == 1161909315) {
      return "efa"; // CTAE in the Effectsa files
    }
    else if (headerInt2 == 1162169684) {
      return "efe"; // TMEE in the Effectse files
    }
    else if (headerInt2 == 1346458196) {
      return "emt"; // TRAP in the Emitters files
    }
    else if (headerInt2 == 1179407176) {
      return "fls"; // HSLF in the Flashes files
    }
    else if (headerInt2 == 1179602516) {
      return "fnt"; // TNOF in the Fonts files
    }
    else if (headerInt2 == 1297040460) {
      return "mdl"; // LDOM in the Models files
    }
    else if (headerInt2 == 1397245010) {
      return "shd"; // RDHS in the Shaders files
    }
    else if (headerInt2 == 1397053012) {
      return "sev"; // TVES in the Soundeve files
    }
    else if (headerInt2 == 1398035012) {
      return "str"; // DRTS in the Soundtra files
    }
    else if (headerInt2 == 1415071308) {
      return "txfl"; // LFXT in the Textures files
    }

    return null;
  }

  /**
   **********************************************************************************************
   When replacing txfl images, if the fileToReplaceWith is a different format image (eg DDS, PNG, ...)
   it can be converted into a txfl image. All other files are replaced without conversion
   @param resourceBeingReplaced the Resource in the archive that is being replaced
   @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
          one that will be converted into a different format, if applicable.
   @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
   **********************************************************************************************
   **/
  @Override
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {

    String beingReplacedExtension = resourceBeingReplaced.getExtension();
    if (beingReplacedExtension.equalsIgnoreCase("txfl")) {
      // try to convert

      String toReplaceWithExtension = FilenameSplitter.getExtension(fileToReplaceWith);
      if (toReplaceWithExtension.equalsIgnoreCase("txfl")) {
        // if the fileToReplace already has a txfl extension, assume it's already a compatible dtx file and doesn't need to be converted
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
      // If we're here, we have a rendered image, so we want to convert it into TXFL
      //
      //
      Viewer_ARC_14_TXFL_TXFL converterPlugin = new Viewer_ARC_14_TXFL_TXFL();

      File destination = new File(fileToReplaceWith.getAbsolutePath() + "." + beingReplacedExtension);
      if (destination.exists()) {
        destination.delete();
      }

      FileManipulator fmOut = new FileManipulator(destination, true);
      converterPlugin.replace(resourceBeingReplaced, imagePreviewPanel, fmOut);
      fmOut.close();

      return destination;

    }
    else if (resourceBeingReplaced.getExtension().equalsIgnoreCase("wav")) {
      // try to convert

      if (!FilenameSplitter.getExtension(fileToReplaceWith).equalsIgnoreCase("wav")) {
        // if the fileToReplaceWith has a WAV extension, need to read it in to a Resource_RawAudio_WAV.
        // otherwise replace as raw
        return fileToReplaceWith;
      }

      if (!(resourceBeingReplaced instanceof Resource_WAV_RawAudio)) {
        // resource must already be in the right format
        return fileToReplaceWith;
      }

      Resource_WAV_RawAudio resourceWAV = (Resource_WAV_RawAudio) resourceBeingReplaced;

      //
      //
      // if we're here, we want to open the WAV file to read in the properties, then store it on the resourceBeingReplaced
      //
      //

      long arcSize = fileToReplaceWith.length();

      FileManipulator fm = new FileManipulator(fileToReplaceWith, false);

      // 4 - Header (RIFF)
      if (!fm.readString(4).equals("RIFF")) {
        return fileToReplaceWith;
      }

      // 4 - Length
      fm.skip(4);

      // 4 - Header 2 (WAVE)
      // 4 - Header 3 (fmt )
      if (!fm.readString(8).equals("WAVEfmt ")) {
        return fileToReplaceWith;
      }

      // 4 - Block Size (16)
      int blockSize = fm.readInt();

      // 2 - Format Tag (0x0001)
      short codec = fm.readShort();
      resourceWAV.setCodec(codec);

      // 2 - Channels
      short channels = fm.readShort();
      resourceWAV.setChannels(channels);

      // 4 - Samples per Second (Frequency)
      int frequency = fm.readInt();
      resourceWAV.setFrequency(frequency);

      // 4 - Average Bytes per Second ()
      fm.skip(4);

      // 2 - Block Alignment (bits/8 * channels)
      short blockAlign = fm.readShort();
      resourceWAV.setBlockAlign(blockAlign);

      // 2 - Bits Per Sample (bits)
      short bitrate = fm.readShort();
      resourceWAV.setBitrate(bitrate);

      // X - Extra Data
      int extraLength = blockSize - 16;
      byte[] extraData = fm.readBytes(extraLength);
      resourceWAV.setExtraData(extraData);

      // 4 - Header (fact) or (data)
      while (fm.getOffset() < arcSize) {
        String header = fm.readString(4);
        if (header.equals("fact")) {
          // 4 - Data Length 
          fm.skip(4);

          // 4 - Number of Samples 
          int samples = fm.readInt();

          resourceWAV.setSamples(samples);
        }
        else if (header.equals("data")) {
          // 4 - Data Length 
          int length = fm.readInt();

          // X - Raw Audio Data
          File destination = new File(fileToReplaceWith.getAbsolutePath() + "_ge_converted.raw");
          if (destination.exists()) {
            destination.delete();
          }
          FileManipulator outFM = new FileManipulator(destination, true);

          // Write out the header from the source file

          // 4 - Audio Length
          outFM.writeInt(length);

          // 4 - Frequency
          outFM.writeInt(frequency);

          // 2 - Codec (105) - ie XBox 4bit
          outFM.writeShort(105);

          // 2 - Channels? (1)
          outFM.writeShort(1);

          // 4 - Frequency
          outFM.writeInt(frequency);

          // 4 - Unknown (12384)
          outFM.writeInt(12384);

          // 2 - Unknown (36)
          outFM.writeShort(36);

          // 2 - Bitrate? (4)
          outFM.writeShort(4);

          // 2 - Unknown (2)
          outFM.writeShort(2);

          // 2 - Unknown (64)
          outFM.writeShort(64);

          // 1 - null
          outFM.writeByte(0);

          outFM.writeBytes(fm.readBytes(length));

          outFM.close();
          fm.close();
          return destination;
        }
        else {
          // 4 - Data Length 
          int length = fm.readInt();
          try {
            FieldValidator.checkLength(length, arcSize);
          }
          catch (Throwable t) {
            fm.close();
            return fileToReplaceWith;
          }

          // X - Unknown
          fm.skip(length);
        }
      }

      fm.close();

      return fileToReplaceWith;
    }
    else {
      return fileToReplaceWith;
    }
  }

}
