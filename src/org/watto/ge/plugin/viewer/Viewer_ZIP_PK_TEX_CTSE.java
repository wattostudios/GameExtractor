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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_TEX_CTSE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_TEX_CTSE() {
    super("ZIP_PK_TEX_CTSE", "ZIP_PK_TEX_CTSE Image");
    setExtensions("tex");

    setGames("The Talos Principle");
    setPlatforms("PC");
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
  public boolean canReplace(PreviewPanel panel) {
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_ZIP_PK) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readString(4).equals("CTSE")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      // 4 - Header 2
      if (fm.readString(4).equals("META")) {
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
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (CTSE)

      // 4 - Header (META)
      // 4 - Hash?
      // 4 - Unknown (11)
      fm.skip(16);

      // 4 - String Length
      int nameLength = fm.readInt();
      FieldValidator.checkLength(nameLength, 512); // guess maz length

      // X - Metadata String
      fm.skip(nameLength);

      int[] widths = null;
      int[] heights = null;
      int[] formats = null;
      int numImages = 0;
      ImageResource[] images = null;

      while (fm.getOffset() < arcSize) {
        // 4 - Header
        String header = fm.readString(4);

        // 4 - Number of Entries
        int numEntries = fm.readInt();

        if (header.equals("MSGS")) {
          if (numEntries > 0) {
            ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Don't know how to handle entries in block " + header);
            return null;
          }
        }
        else if (header.equals("INFO")) {
          //for (int i=0;i<numEntries;i++) {
          //  4 - null
          //  4 - Unknown (1)
          //  4 - Unknown (33)
          //  4 - Unknown (2)
          //}
          fm.skip(numEntries * 16);
        }
        else if (header.equals("RFIL")) {
          for (int i = 0; i < numEntries; i++) {
            // 4 - null
            // 4 - Unknown
            fm.skip(8);

            // 4 - Name Length
            nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Name
            fm.skip(nameLength);
          }
        }
        else if (header.equals("IDNT")) {
          //for (int i=0;i<numEntries;i++) {
          //  4 - null
          //  4 - null
          //}
          fm.skip(numEntries * 8);
        }
        else if (header.equals("EXTY")) {
          for (int i = 0; i < numEntries; i++) {
            // 4 - Entry ID?
            fm.skip(4);

            // 4 - Name Length
            nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Name
            fm.skip(nameLength);
          }
        }
        else if (header.equals("INTY")) {
          for (int i = 0; i < numEntries; i++) {
            // 4 - Header (DTTY)
            // 4 - Entry ID?
            fm.skip(8);

            // 4 - Name Length
            nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Name
            fm.skip(nameLength);

            // 4 - Unknown (1/15/...)
            fm.skip(4);

            // 4 - Unknown (0/5)
            int unknownFlag = fm.readInt();

            if (unknownFlag == 5) {
              // 4 - Unknown ID
              // 4 - Header (STMB)
              fm.skip(8);

              // 4 - Number of Entries
              int numSubEntries = fm.readInt();
              FieldValidator.checkNumFiles(numSubEntries + 1); // allow nulls

              // for each entry
              //   4 - Unknown ID
              //   4 - Unknown ID
              // }
              fm.skip(numSubEntries * 8);
            }
            else if (unknownFlag == 0) {
              // 4 - Unknown ID
              // 4 - Unknown ID
              fm.skip(8);
            }
            else if (unknownFlag == 14) {
              // 4 - Name Length
              nameLength = fm.readInt();
              FieldValidator.checkFilenameLength(nameLength);

              // X - Name
              fm.skip(nameLength);

              // 4 - Unknown ID
              fm.skip(4);
            }
            else {
              //ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Don't know how to handle unknown flag value " + unknownFlag + " in block " + header);
              //return null;
              // 4 - Unknown ID
              fm.skip(4);
            }

          }
        }
        else if (header.equals("EXOB")) {
          if (numEntries > 0) {
            ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Don't know how to handle entries in block " + header);
            return null;
          }
        }
        else if (header.equals("OBTY")) {
          //for (int i=0;i<numEntries;i++) {
          //  4 - Unknown ID
          //  4 - Unknown ID  
          //}
          fm.skip(numEntries * 8);
        }

        else if (header.equals("EDTY")) {
          if (numEntries > 0) {
            ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Don't know how to handle entries in block " + header);
            return null;
          }
        }
        else if (header.equals("OBJS")) {

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(12);

          // 4 - Name Length
          nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          fm.skip(nameLength);

          // 4 - Unknown ID
          // 4 - Unknown ID
          fm.skip(8);
        }
        else if (header.equals("DCON")) {
          numImages = numEntries;
          widths = new int[numEntries];
          heights = new int[numEntries];
          formats = new int[numEntries];

          for (int i = 0; i < numEntries; i++) {
            // 4 - Unknown (1)
            // 4 - null
            // 4 - Unknown (1)
            // 4 - Unknown (7)
            // 4 - null
            // 4 - Unknown (1)
            // 4 - null
            // 4 - Unknown (-1)
            fm.skip(32);

            // 4 - Image Width
            int width = fm.readInt();
            FieldValidator.checkWidth(width);
            widths[i] = width;

            // 4 - Image Height
            int height = fm.readInt();
            FieldValidator.checkHeight(height);
            heights[i] = height;

            // 4 - Unknown (1)
            fm.skip(4);

            // 4 - Image Format (9=DXT1, 10=DXT5)
            int format = fm.readInt();
            formats[i] = format;
          }
        }
        else if (header.equals("STAR")) {
          images = new ImageResource[numImages];

          if (widths == null || heights == null || formats == null) {
            ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Empty width/height/format");
            return null;
          }

          for (int i = 0; i < numImages; i++) {
            int width = widths[i];
            int height = heights[i];
            int imageFormat = formats[i];

            // X - Image Data (DTX5)
            ImageResource imageResource = null;
            if (imageFormat == 9) {
              imageResource = ImageFormatReader.readDXT1(fm, width, height);
            }
            else if (imageFormat == 10) {
              imageResource = ImageFormatReader.readDXT5(fm, width, height);
            }
            else if (imageFormat == 15) {
              imageResource = ImageFormatReader.readBC5(fm, width, height);
            }
            else if (imageFormat == 8) {
              imageResource = ImageFormatReader.readRGBA(fm, width, height);
            }
            else if (imageFormat == 3) {
              imageResource = ImageFormatReader.readBGRA(fm, width, height);
            }
            else if (imageFormat == 7) {
              imageResource = ImageFormatReader.readABGR(fm, width, height);
            }
            else {
              ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Unknown Image Format: " + imageFormat);
              return null;
            }

            images[i] = imageResource;

          }

          fm.close();

          return images[0];
        }
        else {
          ErrorLogger.log("[Viewer_ZIP_PK_TEX_CTSE] Unknown block " + header + " at offset " + (fm.getOffset() - 8));
          return null;
        }

      }

      fm.close();

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