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

package org.watto.datatype;

import java.awt.Image;
import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.plaf.LookAndFeelManager;

public class FileType {

  public static int TYPE_ARCHIVE = 0;

  public static int TYPE_AUDIO = 1;

  public static int TYPE_DOCUMENT = 2;

  public static int TYPE_IMAGE = 3;

  public static int TYPE_PROGRAM = 4;

  public static int TYPE_VIDEO = 5;

  public static int TYPE_MODEL = 6;

  public static int TYPE_OTHER = -1;

  public static Image IMAGE_ARCHIVE;

  public static Image IMAGE_AUDIO;

  public static Image IMAGE_DOCUMENT;

  public static Image IMAGE_IMAGE;

  public static Image IMAGE_PROGRAM;

  public static Image IMAGE_VIDEO;

  public static Image IMAGE_MODEL;

  public static Image IMAGE_OTHER;

  /** The file extension **/
  String extension;

  /** GE-set description **/
  String description;

  /** GE-set type **/
  int type;

  /** System-specific Icon **/
  Image systemIcon = null;

  /** System-specific Description **/
  String systemDescription = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileType() {
    //IMAGE_ARCHIVE = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_AUDIO = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_DOCUMENT = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_IMAGE = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_PROGRAM = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_VIDEO = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();
    //IMAGE_OTHER = new ImageIcon(getClass().getResource("images/filetype_archive.gif")).getImage();

    IMAGE_ARCHIVE = LookAndFeelManager.getImageIcon("images/FileTypes/Archive.png").getImage();
    IMAGE_AUDIO = LookAndFeelManager.getImageIcon("images/FileTypes/Audio.png").getImage();
    IMAGE_DOCUMENT = LookAndFeelManager.getImageIcon("images/FileTypes/Document.png").getImage();
    IMAGE_IMAGE = LookAndFeelManager.getImageIcon("images/FileTypes/Image.png").getImage();
    IMAGE_PROGRAM = LookAndFeelManager.getImageIcon("images/FileTypes/Program.png").getImage();
    IMAGE_VIDEO = LookAndFeelManager.getImageIcon("images/FileTypes/Video.png").getImage();
    IMAGE_MODEL = LookAndFeelManager.getImageIcon("images/FileTypes/Model.png").getImage();
    IMAGE_OTHER = LookAndFeelManager.getImageIcon("images/FileTypes/Other.png").getImage();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileType(String extension, String description) {
    this(extension, description, TYPE_OTHER);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileType(String extension, String description, int type) {
    this.extension = extension;
    this.description = description;
    this.type = type;

    loadSystemSpecificDetails();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileType(String extension, String description, String typeName) {
    this.extension = extension;
    this.description = description;

    if (typeName.equals("Archive")) {
      type = TYPE_ARCHIVE;
    }
    else if (typeName.equals("Audio")) {
      type = TYPE_AUDIO;
    }
    else if (typeName.equals("Document")) {
      type = TYPE_DOCUMENT;
    }
    else if (typeName.equals("Image")) {
      type = TYPE_IMAGE;
    }
    else if (typeName.equals("Program")) {
      type = TYPE_PROGRAM;
    }
    else if (typeName.equals("Video")) {
      type = TYPE_VIDEO;
    }
    else {
      type = TYPE_OTHER;
    }

    loadSystemSpecificDetails();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getDescription() {
    if (Settings.getBoolean("ShowSystemSpecificIcons")) {
      // show system icons for these files
      if (systemDescription == null) {
        loadSystemSpecificDetails();
      }

      if (systemDescription != null && !systemDescription.equals("")) {
        return systemDescription;
      }
    }

    return description;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getExtension() {
    return extension;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image getImage() {

    if (Settings.getBoolean("ShowSystemSpecificIcons")) {
      // show system icons for these files
      if (systemIcon == null) {
        loadSystemSpecificDetails();
      }

      if (systemIcon != null) {
        return systemIcon;
      }
    }

    // show GE icons based on the file type
    if (type == TYPE_ARCHIVE) {
      return IMAGE_ARCHIVE;
    }
    else if (type == TYPE_AUDIO) {
      return IMAGE_AUDIO;
    }
    else if (type == TYPE_DOCUMENT) {
      return IMAGE_DOCUMENT;
    }
    else if (type == TYPE_IMAGE) {
      return IMAGE_IMAGE;
    }
    else if (type == TYPE_PROGRAM) {
      return IMAGE_PROGRAM;
    }
    else if (type == TYPE_VIDEO) {
      return IMAGE_VIDEO;
    }
    else if (type == TYPE_MODEL) {
      return IMAGE_MODEL;
    }
    else {
      return IMAGE_OTHER;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getType() {
    return type;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadSystemSpecificDetails() {

    if (Settings.getBoolean("ShowSystemSpecificIcons")) {
      // show system icons and descriptions for these files
      try {
        File tempFile = new File(Settings.get("TempDirectory") + File.separator + "ge_filetype." + extension);
        if (!tempFile.exists()) {
          tempFile.createNewFile();
        }
        if (tempFile.exists()) {
          sun.awt.shell.ShellFolder shellFolder = sun.awt.shell.ShellFolder.getShellFolder(tempFile);
          systemIcon = shellFolder.getIcon(true);
          systemDescription = shellFolder.getFolderType();
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setDescription(String description) {
    this.description = description;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExtension(String extension) {
    this.extension = extension;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setType(int type) {
    this.type = type;
  }

}