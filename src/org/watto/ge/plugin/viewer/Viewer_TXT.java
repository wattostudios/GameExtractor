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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Text;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TXT extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TXT() {
    super("TXT", "Plain Text Document");
    setExtensions("txt", "ini", "inf", "nfo", "cfg", "log", "java", "html", "htm", "xml", "lua", "js", "json");
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Text) {
      return true;
    }
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // also look for <?xml at the beginning
      if (fm.readString(5).equalsIgnoreCase("<?xml")) {
        rating += 25;
      }

      return rating;

    }
    catch (Throwable e) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      // Using the text viewer
      int length = (int) fm.getLength();
      //int sizeLimit = Settings.getInt("TextFileViewerFileSizeLimit");
      //if (length > sizeLimit){
      //  length = sizeLimit;
      //  }

      /*
       * String fileText = fm.readString(length);
       *
       * try { if (fileText.length() >= 8 && ((int)fileText.charAt(3)) < 28 &&
       * ((int)fileText.charAt(5)) < 28 && ((int)fileText.charAt(7)) < 28){ // probably unicode
       * text fileText = new String(fileText.getBytes(),"UTF-16LE"); } } catch (Exception e){
       * e.printStackTrace(); }
       */

      String fileText = "";

      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      boolean formatFound = false;
      if (byte1 == 255 && byte2 == 254) {
        // Unicode
        //fm.seek(0); // want to ignore the 2-byte unicode header
        fileText = fm.readUnicodeString((length - 2) / 2);
        formatFound = true;
      }
      else if (byte1 == 239 && byte2 == 187) {
        int byte3 = ByteConverter.unsign(fm.readByte());
        if (byte3 == 191) {
          // UTF8
          //fm.seek(0); // want to ignore the 3-byte UTF8 header
          fileText = fm.readString(length - 3);
          formatFound = true;
        }
      }

      if (!formatFound) {
        // ASCII
        fm.seek(0);
        fileText = fm.readString(length);
      }

      PreviewPanel_Text preview = new PreviewPanel_Text(fileText);

      return preview;

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
    try {

      if (preview instanceof PreviewPanel_Text) {
        fm.writeString(((PreviewPanel_Text) preview).getText());
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}