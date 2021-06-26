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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.MultiFileBlockExporterWrapper;
import org.watto.ge.plugin.exporter.MultiFileBlockVariableExporterWrapper;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NoExt_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_2() {

    super("NoExt_2", "NoExt_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Helldivers",
        "Magicka 2",
        "Memphis",
        "Warhammer: End Times: Vermintide");
    setExtensions(""); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

  }

  int version = 4;

  /**
   **********************************************************************************************
   Decompressed an archive, where the whole archive is compressed.
   Reads the compressed block information first, then processes the compressed blocks themselves.
   Writes the output to a file with the same name, but with "_ge_decompressed" at the end of it.
   The decompressed file contains the same Unreal header as the compressed file, so you can open
   the decompressed file in GE directly, without needing to re-decompress anything.
   If the decompressed file already exists, we use that, we don't re-decompress.
   **********************************************************************************************
   **/
  public FileManipulator decompressArchive(FileManipulator fm) {
    try {
      // Build a new "_ge_decompressed" archive file in the current directory
      File origFile = fm.getFile();

      String pathOnly = FilenameSplitter.getDirectory(origFile);
      String filenameAndExtension = FilenameSplitter.getFilenameAndExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameAndExtension + "_ge_decompressed");
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);
      decompFM.getBuffer().setBufferSize(10485760); // 10MB output buffer for fast writing

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      //fm.getBuffer().setBufferSize(10485760); // 10MB input buffer as well, for fast reading
      //fm.flush();
      fm.seek(0);

      // 2 - Version (4/5)
      // 2 - Unknown ((bytes)0,240)
      version = fm.readByte();
      fm.skip(3);

      // 4 - Decompressed Archive Length?
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // 4 - null
      fm.skip(4);

      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      while (fm.getOffset() < arcSize) {
        // 4 - Compressed Block Length
        int compBlockLength = fm.readInt();
        FieldValidator.checkLength(compBlockLength);

        /*
        long decompBlockLength = fm.getRemainingLength();
        if (decompBlockLength > 65536) {
          decompBlockLength = 65536; // only the last block is smaller
        }
        */
        long decompBlockLength = 65536;

        long offset = fm.getOffset();

        // X - Compressed File Data (ZLib Compression)

        if (compBlockLength == decompBlockLength) {
          // not compressed - raw data block
          for (int b = 0; b < compBlockLength; b++) {
            decompFM.writeByte(fm.readByte());
          }
        }
        else {
          // compressed

          //System.out.println("Decompressing block at offset " + offset);
          exporter.openUnclosable(fm, compBlockLength, (int) decompBlockLength);

          while (exporter.available()) {
            decompFM.writeByte(exporter.read());
          }

          exporter.closeOnlyInflater();
        }

        // ensure we're at the correct offset
        //int tooSmall = (int) ((offset + compBlockLength) - fm.getOffset());
        //System.out.println("too small by " + tooSmall);
        fm.seek(offset + compBlockLength);

      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(currentOffset);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
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
      byte[] headerBytes = fm.readBytes(4);
      if ((headerBytes[0] == 4 || headerBytes[0] == 5) && headerBytes[1] == 0 && headerBytes[2] == 0 && ByteConverter.unsign(headerBytes[3]) == 240) {
        rating += 50;
      }

      fm.skip(8);

      // ZLib Compression Header
      if (fm.readString(1).equals("x")) {
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

      File origPath = path;

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - Version (4/5)
      // 2 - Unknown ((bytes)0,240)
      version = fm.readByte();
      fm.skip(3);

      // SEE IF WE NEED TO DECOMPRESS THE ARCHIVE
      FileManipulator decompFM = decompressArchive(fm);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      fm.getBuffer().setBufferSize(40); // for quick reading

      // 256 - Unknown
      // skip the directory
      int dirOffset = (numFiles * 16) + 256 + 4;
      if (version == 5) {
        dirOffset += (numFiles * 4);
      }
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int bankFileNumber = -1;
      for (int i = 0; i < numFiles; i++) {
        //System.out.println(fm.getOffset());
        // 8 - File Type Hash? (same as in the Directory)
        long fileType = fm.readLong();

        // 8 - File Name Hash? (same as in the Directory)
        fm.skip(8);

        // 4 - Number of Blocks (1)
        int numBlocks = fm.readInt();
        FieldValidator.checkNumFiles(numBlocks);

        // 4 - Unknown
        fm.skip(4);

        int length = 0;

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Unknown
          fm.skip(4);

          // 4 - File Length
          int blockLength = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown
          fm.skip(4);

          if (version == 4) {
            length += blockLength;
          }
          else if (version == 5) {
            if (fileType == -3656297521719370190l) {
              // nothing special
              length += blockLength;
            }
            else {
              // 4 - Extra Header Length? (8)
              int extraLength = fm.readInt();
              FieldValidator.checkRange(extraLength, 0, 512);

              // 4 - File Length (not including these version 5 fields - the File Data length only)
              int dataLength = fm.readInt();
              FieldValidator.checkLength(dataLength, arcSize);
              length += dataLength;

              // 8 - File Name Hash? (same as in the Directory) [OPTIONAL]
              extraLength = blockLength - dataLength - 8;
              FieldValidator.checkLength(extraLength, arcSize);
              fm.skip(extraLength);
            }
          }
        }

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(i);
        if (fileType == 1792059921637536489l) {
          filename += ".bone";
        }
        else if (fileType == -7845776955604022234l) {
          filename += ".animation";
        }
        else if (fileType == -7389443945178646108l) {
          // SPECIAL CASE - sets the filename to be the same as the source file, so we can use it to read the .stream files!
          //filename += ".timpani_bank";
          filename = origPath.getName() + ".timpani_bank";
          bankFileNumber = i; // so we can analyse it down further...
        }
        else if (fileType == -6591347889605831076l) {
          filename += ".state_machine";
        }
        else if (fileType == -6333977373143224988l) {
          filename += ".particle";
        }
        else if (fileType == -3656297521719370190l) {
          filename += ".dds";
        }
        else if (fileType == -2259526030728936129l) {
          filename += ".unit";
        }
        else if (fileType == -1531025310400979233l) {
          filename += ".material";
        }
        else if (fileType == -111525813760131931l) {
          filename += ".shading_environment";
        }
        else if (fileType == 3055991222284753605l) {
          filename += ".level";
        }
        else if (fileType == -6171789732059940328l) {
          filename += ".id"; // guess only - only found 3 files with this, and they were either 4 or 8 bytes in size, and contained only int fields with value "1"
        }
        else if (fileType == 2848016463268491676l) {
          filename += ".render_config";
        }
        else if (fileType == -7035681691298058571l) {
          filename += ".shader_library_group";
        }
        else if (fileType == -1878508312612070765l) {
          filename += ".shader_library";
        }
        else if (fileType == -6990137951956076416l) {
          filename += ".font";
        }
        else if (fileType == -6823360279786481694l) {
          filename += ".lua";
        }
        else if (fileType == -9051012362580577742l) {
          filename += ".config";
        }
        else if (fileType == 4260310590552720244l) {
          filename += ".network_config";
        }
        else if (fileType == -5936749679887390854l) {
          filename += ".package";
        }
        else if (fileType == -4674384319396922447l) {
          filename += ".physics_properties";
        }
        else if (fileType == -5968043961655577708l) {
          filename += ".surface_properties";
        }
        else if (fileType == 46134157573332076l) {
          filename += ".timpani_master";
        }
        else if (fileType == -7866682425036021491l) {
          filename += ".flow";
        }
        else if (fileType == -5586802062191616713l) {
          filename += ".mouse_cursor";
        }
        else if (fileType == 979299457696010195l) {
          filename += ".strings";
        }
        else if (fileType == 8823852514130469410l) {
          filename += ".ctdg";
        }
        else if (version == 5 && fileType == 6006249203084351385l) {
          filename += ".bnk";
        }
        else {
          filename += "." + fileType;
        }

        //System.out.println((offset - 36) + "\t" + filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);
        resources[i].forceNotAdded(true);

        TaskProgressManager.setValue(i);
      }

      // Now analyse the TIMPANI_BANK file
      if (bankFileNumber != -1) {
        Resource bankResource = resources[bankFileNumber];

        // See if there is a STREAM file with the same name
        String streamFilePath = origPath.getAbsolutePath() + ".stream";

        File streamFile = new File(streamFilePath);
        if (streamFile.exists()) {
          // yep, the stream file exists, so continue processing...

          fm.getBuffer().setBufferSize(2048); // normal buffer size

          long streamSize = streamFile.length();

          long bankOffset = bankResource.getOffset();
          fm.seek(bankOffset);

          // 8 - Number of Files
          int numBankFiles = fm.readInt();
          FieldValidator.checkNumFiles(numBankFiles);
          fm.skip(4);

          // add this many files to the archive
          int newSize = numFiles + numBankFiles;

          Resource[] temp = resources;
          resources = new Resource[newSize];
          System.arraycopy(temp, 0, resources, 0, numFiles);

          // read the bank directory and add them to the array
          long[] part1Offsets = new long[newSize];
          long[] part1Lengths = new long[newSize];
          long[] part2Offsets = new long[newSize];
          long[] part2Lengths = new long[newSize];

          for (int i = numFiles; i < newSize; i++) {
            // 8 - File Name Hash?
            fm.skip(8);

            // 4 - File Offset
            long part1Offset = fm.readInt() + bankOffset + 68; // +68 to skip the file header
            FieldValidator.checkOffset(part1Offset, arcSize);
            part1Offsets[i] = part1Offset;

            // 4 - File Length
            int part1Length = fm.readInt() - 68;
            FieldValidator.checkLength(part1Length, arcSize);
            part1Lengths[i] = part1Length;

            // 4 - Stream File Offset
            int part2Offset = fm.readInt();
            FieldValidator.checkOffset(part2Offset, streamSize + 1);
            part2Offsets[i] = part2Offset;

            // 4 - Stream File Length
            int part2Length = fm.readInt();
            FieldValidator.checkLength(part2Length, streamSize);
            part2Lengths[i] = part2Length;
          }

          fm.getBuffer().setBufferSize(8); // quick reading

          ExporterPlugin defaultExporter = Exporter_Default.getInstance();
          File[] multiFilesArray = new File[] { fm.getFile(), streamFile }; // for OGG files

          ExporterPlugin wavHeaderExporter = Exporter_Custom_WAV_RawAudio.getInstance();
          ExporterPlugin[] wavExporters = new ExporterPlugin[] { wavHeaderExporter, defaultExporter, defaultExporter };
          File[] multiFilesArrayWav = new File[] { fm.getFile(), fm.getFile(), streamFile }; // for WAV files

          for (int i = numFiles; i < newSize; i++) {
            // Need to see if it's an OGG or a WAV file. WAV files need the header prepended to it
            fm.seek(part1Offsets[i]);

            // 4 - Audio Header
            String audioHeader = fm.readString(4);

            if (audioHeader.equals("OggS")) {
              // OGG Audio
              String filename = Resource.generateFilename(i) + ".ogg";

              long[] offsets = new long[] { part1Offsets[i], part2Offsets[i] };
              long[] lengths = new long[] { part1Lengths[i], part2Lengths[i] };
              ExporterPlugin multiExporter = new MultiFileBlockExporterWrapper(defaultExporter, multiFilesArray, offsets, lengths, lengths);

              int combinedLength = (int) (part1Lengths[i] + part2Lengths[i]);

              // Add the audio file to the array
              resources[i] = new Resource(streamFile, filename, part1Offsets[i], combinedLength, combinedLength, multiExporter);
              resources[i].forceNotAdded(true);
            }
            else {
              // WAV Audio
              String filename = Resource.generateFilename(i) + ".wav";

              long[] offsets = new long[] { 0, part1Offsets[i], part2Offsets[i] };
              long[] lengths = new long[] { 0, part1Lengths[i], part2Lengths[i] };

              ExporterPlugin multiExporter = new MultiFileBlockVariableExporterWrapper(wavExporters, multiFilesArrayWav, offsets, lengths, lengths);

              int combinedLength = (int) (part1Lengths[i] + part2Lengths[i]);// + 44); // +44 for the WAV header

              // Add the audio file to the array
              Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(streamFile, filename, part1Offsets[i], combinedLength, combinedLength, multiExporter);
              resource.setAudioProperties(48000, (short) 16, (short) 1, combinedLength);
              resource.forceNotAdded(true);
              resources[i] = resource;
            }
          }

        }
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

}
