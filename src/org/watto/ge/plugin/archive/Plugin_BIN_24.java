/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import java.util.HashMap;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_24 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_24() {

    super("BIN_24", "BIN_24");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Beyond Good & Evil");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("pal", "Color Palette", FileType.TYPE_OTHER),
        new FileType("mdl", "Model Mesh", FileType.TYPE_MODEL));

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

      // decompressed block length
      if (fm.readInt() == 512000) {
        rating += 25;
      }

      // compressed block length
      if (FieldValidator.checkRange(fm.readInt(), 0, 512000)) {
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
      String filenameOnly = FilenameSplitter.getFilename(origFile);
      String extensionOnly = FilenameSplitter.getExtension(origFile);

      File decompFile = new File(pathOnly + File.separatorChar + filenameOnly + "_ge_decompressed" + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      long currentOffset = fm.getOffset();
      long arcSize = fm.getLength();

      fm.seek(0); // to fill the buffer from the start of the file, for efficient reading

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(arcSize); // progress bar
      TaskProgressManager.setIndeterminate(true);

      Exporter_LZO_SingleBlock exporter = Exporter_LZO_SingleBlock.getInstance();

      while (fm.getOffset() < arcSize) {

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        // X - Compressed File Data (LZO Compression)
        exporter.open(fm, compLength, decompLength);

        while (exporter.available()) {
          decompFM.writeByte(exporter.read());
        }

      }
      exporter.close();

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
      PaletteManager.clear(); // clear the color palettes before we load new ones into it.

      FileManipulator fm = new FileManipulator(path, false);

      FileManipulator decompFM = decompressArchive(fm);
      if (decompFM != null) {
        fm.close(); // close the original archive
        fm = decompFM; // now we're going to read from the decompressed file instead
        fm.seek(0); // go to the same point in the decompressed file as in the compressed file

        path = fm.getFile(); // So the resources are stored against the decompressed file
      }

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;

      HashMap<Long, Long> paletteLookup = new HashMap<Long, Long>();
      long currentPalette = 0;
      int currentMeta = 0;
      int currentTexture = 0;
      int[] paletteMapping = new int[numFiles];

      while (fm.getOffset() < arcSize) {
        boolean textureFile = false;

        // 4 - File Length
        int length = fm.readInt();
        try {
          FieldValidator.checkLength(length, arcSize);
        }
        catch (Throwable t) {
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
        }

        // X - File Data
        long offset = fm.getOffset();

        String filename = Resource.generateFilename(realNumFiles);

        if (length < 20) {
          fm.skip(length);
        }
        else {

          // 4 - File Header (looking for images)
          int fileHeader1 = fm.readInt();

          // 1 - File Header 2 (looking for images)
          int fileHeader2 = fm.readByte();

          // 3 - Unknown
          int fileHeader2a = fm.readByte();
          int fileHeader3 = fm.readByte();
          int fileHeader3a = fm.readByte();

          // 2 - Image Width
          int width = fm.readShort();

          // 2 - Image Height
          int height = fm.readShort();

          // 4 - Unknown
          int fileHeader4 = fm.readInt();

          // 4 - null
          int nullCheck = fm.readInt();

          if (fileHeader1 == 1868654382) {
            filename += ".gao";
          }
          else if (fileHeader1 == 2003793710) {
            filename += ".wow";
          }
          else if (fileHeader1 == 1414418246) {
            filename += ".font";
          }
          else {
            if (length == 48 || (length == 64 && nullCheck != 0) || length == 768 || length == 1024) {
              filename += ".pal";
            }
            else if (fileHeader1 == 1 && (width >= 0 && width < 10000) && height == 0 && fileHeader4 > 0 && fileHeader4 < 10000) { // 10000 guess
              filename += ".mdl"; // Model (Type 2)
            }
            else if (fileHeader1 == 1895872846) {
              if (length > 40) {
                filename += ".mdl"; // Model (Type 1)
              }
            }
            else if (fileHeader1 == -1) {
              //System.out.println(fileHeader3);
              if (fileHeader3 == 7) {
                filename += ".tex_meta"; // metadata

                long backOff = fm.getOffset();
                fm.skip(16);
                //System.out.println(fm.readShort());

                long palettePos = fm.readShort();

                if (paletteLookup.containsKey(palettePos)) {
                  //System.out.println("Palette Number already exists --> Mapping " + palettePos + " to " + paletteLookup.get(palettePos));
                  paletteMapping[currentMeta] = paletteLookup.get(palettePos).intValue();
                }
                else {
                  //System.out.println("New --> " + palettePos);
                  paletteMapping[currentMeta] = (int) currentPalette;

                  paletteLookup.put(palettePos, currentPalette);
                  currentPalette++;
                }
                currentMeta++;

                fm.relativeSeek(backOff);
              }
              else if (fileHeader2 == 5 && ByteConverter.unsign((byte) fileHeader2a) == 128) {
                filename += ".mdl"; // Model (Type 1)
              }
              else if (length == 32) {
                filename += ".tex_header"; // Paletted Image (Header Only)
              }
              else if (length >= 64 && (width != 0 && height != 0) && ((fileHeader3 != 1 && fileHeader3a != 16))) { // 4097 are images in RGBA color or something, not paletted
                filename += ".tex"; // Paletted Image
                textureFile = true;
                //System.out.println(fileHeader3 + "\ttex");

                //if (fileHeader2 == previous2 && fileHeader2a == previous2a && fileHeader3 == previous3 && fileHeader4 == previous4 && prevWidth == width && prevHeight == height && prevNull == nullCheck) {
                //  System.out.println("Repeat");
                //}
                //else {
                //System.out.println(fileHeader4);
                //}
                /*
                if (width * height + 32 == length) {
                  System.out.println("8bit" + "\t" + fileHeader3);
                }
                else if ((width * height / 2) + 32 == length) {
                  System.out.println("4bit" + "\t" + fileHeader3);
                }
                else {
                  System.out.println("unknown" + "\t" + fileHeader3);
                }
                */

              }
              else {
                //filename += ".tex_other"; // something else, like a mesh or something?
              }

            }
          }

          fm.skip(length - 20);
        }

        //System.out.println(offset - 4 + "\t" + length);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.forceNotAdded(true);
        resources[realNumFiles] = resource;
        realNumFiles++;

        if (textureFile) {
          resource.addProperty("PaletteID", paletteMapping[currentTexture]);
          currentTexture++;
        }

        TaskProgressManager.setValue(offset);
      }

      resources = resizeResources(resources, realNumFiles);

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
    long length = resource.getLength();
    
    if (headerInt1 == -1) {
      if (length == 32) {
        return "tex_header"; // Paletted Image (Header Only)
      }
      else if (length < 100) {
        return "tex_small"; // Paletted Image
      }
      else {
        return "tex"; // Paletted Image
      }
    }
    else if (headerInt1 == 1868654382) {
      return "gao";
    }
    else if (headerInt1 == 1414418246) {
      return "font";
    }
    
    if (length == 48 || length == 64 || length == 768 || length == 1024) {
      return "pal"; // Color Palette
    }
    */

    return null;
  }

}
