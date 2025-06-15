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

import java.io.File;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.ComponentRepository;
import org.watto.component.SidePanel_DirectoryList;
import org.watto.component.WSTableColumn;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.DirectoryBuilder;
import org.watto.io.FilenameChecker;
import org.watto.plaf.LookAndFeelManager;

/**
 **********************************************************************************************
 * The Archive class manages details about the loaded archive, such as the <i>resources</i>, the
 * <i>columns</i> to be shown in the current FileListPanel, and the plugin used to read the
 * archive. It also contains methods for the management and manipulation of Resources, such as
 * adding and removing resources. <br>
 * <br>
 * This class is entirely static. You will need to call the constructor once in order to set up a
 * few globals and such, but from this point onwards you would simply call the methods directly
 * such as by "Archive.runMethod()".
 **********************************************************************************************
 **/
public class Archive {

  /** The file that was read into this archive **/
  static File basePath = null;

  /** The columns to be shown in the current FileListPanel **/
  static WSTableColumn[] columns = new WSTableColumn[0];

  /** The plugin used to read the <i>basePath</i> archive **/
  static ArchivePlugin readPlugin = new AllFilesPlugin();

  /** The resources stored in this archive **/
  static Resource[] resources = new Resource[0];

  static Icon fileIcon;

  static Icon renamedIcon;

  static Icon unrenamedIcon;

  static Icon replacedIcon;

  static Icon unreplacedIcon;

  static Icon addedIcon;

  static Icon unaddedIcon;

  /**
   **********************************************************************************************
   * Adds all files in the <i>directory</i> to the archive. If there are any sub-directories,
   * they are also analysed and added.
   * @param directory the directory that contains the files to add
   * @param directoryName the prefix name to use for the files in the archive, rather than using
   *        the absolute directory path.
   **********************************************************************************************
   **/
  public static void addDirectory(File directory, String directoryName) {
    try {

      File[] files = directory.listFiles();

      int numFiles = resources.length;
      int newNumFiles = numFiles + files.length;
      resizeResources(newNumFiles);
      for (int i = numFiles, j = 0; i < newNumFiles && j < files.length; i++, j++) {
        if (files[j].isDirectory()) {
          addDirectory(files[j], directoryName + files[j].getName() + File.separator);
        }
        else {
          resources[i] = readPlugin.getBlankResource(files[j], directoryName + files[j].getName());
        }
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Adds the <i>files</i> to the archive. If any of the files are a directory, the contents of
   * the directory are added via addDirectory(File,String).
   * @param files the files to add to the archive.
   **********************************************************************************************
   **/
  public static void addFiles(File[] files) {
    try {

      int numFiles = resources.length;
      int newNumFiles = numFiles + files.length;
      resizeResources(newNumFiles);
      for (int i = numFiles, j = 0; i < newNumFiles && j < files.length; i++, j++) {
        if (files[j].isDirectory()) {
          addDirectory(files[j], files[j].getName() + File.separator);
        }
        else {
          resources[i] = readPlugin.getBlankResource(files[j], files[j].getName());
        }
      }

      // when a directory is added, it doesn't appear as a file in the list,
      // rather the contents of the directory are added instead. This call
      // will ensure that any nulls created by adding a directory will be
      // removed from the array.
      removeNullResources();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Adds a resource to the archive
   * @param file the resource to add
   **********************************************************************************************
   **/
  public static void addResource(Resource file) {
    int numResources = resources.length;
    resizeResources(numResources + 1);
    resources[numResources] = file;
  }

  /**
   **********************************************************************************************
   * Adds a number of files to the archive
   * @param files the files to add
   **********************************************************************************************
   **/
  public static void addResources(Resource[] files) {

    int numFiles = resources.length;
    int newNumFiles = numFiles + files.length;
    resizeResources(newNumFiles);

    for (int i = numFiles, j = 0; i < newNumFiles && j < files.length; i++, j++) {
      resources[i] = files[j];
    }

  }

  /**
   **********************************************************************************************
   * Extracts all the resources from this archive to the <i>directory</i>
   * @param directory the directory to export the files to.
   **********************************************************************************************
   **/
  public static void extractAllResources(File directory) {
    for (int i = 0; i < resources.length; i++) {
      resources[i].extract(directory);
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Icon getAddedIcon(boolean added) {
    if (added) {
      return addedIcon;
    }
    else {
      return unaddedIcon;
    }
  }

  /**
   **********************************************************************************************
   * Gets the name of the archive
   * @return the name of the opened archive, or "newArchive" if the archive was started from
   *         scratch
   **********************************************************************************************
   **/
  public static String getArchiveName() {
    if (basePath == null) {
      return "newArchive";
    }
    else {
      return basePath.getName();
    }
  }

  /**
   **********************************************************************************************
   * Gets the file that was loaded
   * @return the archive file
   **********************************************************************************************
   **/
  public static File getBasePath() {
    return basePath;
  }

  /**
   **********************************************************************************************
   * Gets the column with the <i>columnCode</i>
   * @param columnCode the code of the column
   * @return the column
   **********************************************************************************************
   **/
  public static WSTableColumn getColumn(char columnCode) {
    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getCharCode() == columnCode) {
        return columns[i];
      }
    }
    return null;
  }

  /**
   **********************************************************************************************
   * Gets the column from position <i>column</i> of the array
   * @param column the column number
   * @return the column
   **********************************************************************************************
   **/
  public static WSTableColumn getColumn(int column) {
    if (column < columns.length) {
      return columns[column];
    }
    else {
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Gets all the columns
   * @return the columns
   **********************************************************************************************
   **/
  public static WSTableColumn[] getColumns() {
    if (columns == null) {
      columns = getDefaultColumns();
    }
    return columns;
  }

  /**
   **********************************************************************************************
   * Gets the default columns for an archive
   * @return the default columns
   **********************************************************************************************
   **/
  public static WSTableColumn[] getDefaultColumns() {
    // We can't get it from ArchivePlugin directly because it is an abstract class.
    // So, get it from a dummy ArchivePlugin instead.
    return new AllFilesPlugin().getColumns();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Icon getIcon(String name) {
    if (Settings.getBoolean("ShowSystemSpecificIcons")) {

      try {
        // checks the filename contains no funny characters.
        File file = FilenameChecker.correctFilename(new File(Settings.get("TempDirectory") + File.separator + name));
        if (!file.exists()) {
          // makes the directory
          DirectoryBuilder.buildDirectory(file, false);
          // creates an empty temp file of this name
          file.createNewFile();
        }

        // gets the icon for this file
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        return icon;
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    return fileIcon;
  }

  /**
   **********************************************************************************************
   * Gets the maximum number of files, from setting "MaxNumberOfFiles4"
   * @return the maximum number of files
   **********************************************************************************************
   **/
  public static int getMaxFiles() {
    return getMaxFiles(4);
  }

  /**
   **********************************************************************************************
   * Gets the maximum number of files for a field with <i>size</i> number of bytes
   * @param size the number of bytes assigned to the NumberOfFiles field of an archive
   * @return the maximum number of files
   **********************************************************************************************
   **/
  public static int getMaxFiles(int size) {
    if (size == 2) {
      return Settings.getInt("MaxNumberOfFiles2");
    }
    else {
      return Settings.getInt("MaxNumberOfFiles4");
    }
  }

  /**
   **********************************************************************************************
   * Gets the number of columns
   * @return the number of columns
   **********************************************************************************************
   **/
  public static int getNumColumns() {
    return columns.length;
  }

  /**
   **********************************************************************************************
   * Gets the number of resources in the archive
   * @return the number of files
   **********************************************************************************************
   **/
  public static int getNumFiles() {
    if (resources == null) {
      return 0;
    }
    return resources.length;
  }

  /**
   **********************************************************************************************
   * Gets the plugin used to read the archive
   * @return the plugin
   **********************************************************************************************
   **/
  public static ArchivePlugin getReadPlugin() {
    return readPlugin;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Icon getRenamedIcon(boolean renamed) {
    if (renamed) {
      return renamedIcon;
    }
    else {
      return unrenamedIcon;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Icon getReplacedIcon(boolean replaced) {
    if (replaced) {
      return replacedIcon;
    }
    else {
      return unreplacedIcon;
    }
  }

  /**
   **********************************************************************************************
   * Gets the resource from index <i>num</i> of the array
   * @param num the resource number
   * @return the resource.
   **********************************************************************************************
   **/
  public static Resource getResource(int num) {
    return resources[num];
  }

  /**
   **********************************************************************************************
   * Gets all the resources in the archive
   * @return the resources
   **********************************************************************************************
   **/
  public static Resource[] getResources() {
    return resources;
  }

  /**
   **********************************************************************************************
   * Gets <i>numOfResources</i>, starting from the <i>startResource</i>
   * @return the resources
   **********************************************************************************************
   **/
  public static Resource[] getResources(int startResource, int numOfResources) {
    Resource[] range = new Resource[numOfResources];
    System.arraycopy(resources, startResource, range, 0, numOfResources);
    return range;
  }

  /**
   **********************************************************************************************
   * Gets the column with the <i>columnCode</i>
   * @param columnCode the code of the column
   * @return the column
   **********************************************************************************************
   **/
  public static WSTableColumn[] getSearchableColumns() {
    WSTableColumn[] columns = getColumns();

    WSTableColumn[] outColumns = new WSTableColumn[columns.length];
    int numColumns = 0;

    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getType() != Icon.class) {
        outColumns[numColumns] = columns[i];
        numColumns++;
      }
    }

    if (numColumns != outColumns.length) {
      columns = outColumns;
      outColumns = new WSTableColumn[numColumns];
      System.arraycopy(columns, 0, outColumns, 0, numColumns);
    }

    return outColumns;
  }

  /**
   **********************************************************************************************
   * Records an error to the log file
   * @param t the error that occurred.
   **********************************************************************************************
   **/
  public static void logError(Throwable t) {
    ErrorLogger.log(t);
  }

  /**
   **********************************************************************************************
   * Makes a new archive. Effectively resets the globals to their initial values. If there is an
   * archive already opened, and if the archive has been modified, it will ask the user to save
   * first.
   **********************************************************************************************
   **/
  public static void makeNewArchive() {

    resources = new Resource[0];
    readPlugin = new AllFilesPlugin();
    basePath = null;

    columns = getDefaultColumns();

    setBasePath(null);

    SidePanel_DirectoryList panel = (SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList");
    panel.checkInvalidControls();
  }

  /**
   **********************************************************************************************
   * Removes all resources from the archive
   **********************************************************************************************
   **/
  public static void removeAllResources() {
    resources = new Resource[0];
  }

  /**
   **********************************************************************************************
   * Removes all null resources from the array, which may be caused when removing files in batch,
   * or when adding directories of files.
   **********************************************************************************************
   **/
  public static void removeNullResources() {
    try {

      // find the 2 pointers
      int nullPos = -1;
      int nextFile = -1;
      for (int i = 0; i < resources.length; i++) {
        if (nullPos == -1 && resources[i] == null) {
          nullPos = i;
        }
        if (nullPos > -1 && resources[i] != null) {
          nextFile = i;
          i = resources.length;
        }
      }

      // re-shuffle the resource array to the top
      if (nullPos > -1 && nextFile > -1) {
        for (int i = nextFile; i < resources.length; i++) {
          if (resources[i] != null) {
            resources[nullPos] = resources[i];
            nullPos++;
          }
        }
      }

      // resize the resources array
      if (nullPos > -1) {
        resizeResources(nullPos);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Removes the resource <i>num</i> from the archive
   * @param num the resource to remove
   **********************************************************************************************
   **/
  public static void removeResource(int num) {
    removeResource(resources[num]);
  }

  /**
   **********************************************************************************************
   * Removes the <i>file</i> from the archive
   * @param file the resource to remove
   **********************************************************************************************
   **/
  public static void removeResource(Resource file) {
    removeResources(new Resource[] { file });
  }

  /**
   **********************************************************************************************
   * Removes the <i>files</i> from the archive
   * @param files the resources to remove.
   **********************************************************************************************
   **/
  public static void removeResources(Resource[] files) {
    try {

      // null out the resources to remove
      int filesPos = 0;
      while (filesPos < files.length) {
        for (int i = 0; i < resources.length; i++) {
          if (files[filesPos] == resources[i]) {
            resources[i] = null;
            filesPos++;
            if (filesPos >= files.length) {
              i = resources.length;
            }
          }
        }
      }

      removeNullResources();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
   * Changes the size of the <i>resources</i> array
   * @param numResources the new size of the array
   **********************************************************************************************
   **/
  public static void resizeResources(int numResources) {
    Resource[] temp = resources;
    resources = new Resource[numResources];

    if (numResources < temp.length) {
      System.arraycopy(temp, 0, resources, 0, numResources);
    }
    else {
      System.arraycopy(temp, 0, resources, 0, temp.length);
    }
  }

  /**
   **********************************************************************************************
   * Sets the <i>basePath</i> of the opened file
   * @param basePathNew the new path
   **********************************************************************************************
   **/
  public static void setBasePath(File basePathNew) {
    basePath = basePathNew;


      if (basePath == null) {
        GameExtractor.getInstance().setTitle(Language.get("ProgramName_Free") + " " + Settings.get("Version") + " - http://www.watto.org");
      }
      else {
        GameExtractor.getInstance().setTitle(Language.get("ProgramName_Free") + " " + Settings.get("Version") + " [" + basePath.getName() + "]");
      }
  }

  /**
   **********************************************************************************************
   * Sets the columns to be shown by the current FileListPanel to the default
   **********************************************************************************************
   **/
  public static void setColumns() {
    columns = getDefaultColumns();
  }

  /**
   **********************************************************************************************
   * Sets the columns to be shown by the current FileListPanel
   * @param columnsNew the new columns
   **********************************************************************************************
   **/
  public static void setColumns(WSTableColumn[] columnsNew) {
    columns = columnsNew;
  }

  /**
   **********************************************************************************************
   * Sets the number of resources in the archive. Used for undo() in Task_AddFiles();
   * @param numFiles the new number of files
   **********************************************************************************************
   **/
  public static void setNumFiles(int numFiles) {
    resizeResources(numFiles);
  }

  /**
   **********************************************************************************************
   * Sets the plugin used to read the archive
   * @param pluginNew the new plugin
   **********************************************************************************************
   **/
  public static void setReadPlugin(ArchivePlugin pluginNew) {
    readPlugin = pluginNew;

    SidePanel_DirectoryList panel = (SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList");
    panel.checkInvalidControls();
  }

  /**
   **********************************************************************************************
   * Sets the resources in the archive
   * @param resourcesNew the new resources
   **********************************************************************************************
   **/
  public static void setResources(Resource[] resourcesNew) {
    resources = resourcesNew;
  }

  /**
   **********************************************************************************************
   * Constructor. Should only be called once.
   **********************************************************************************************
   **/
  public Archive() {
    columns = getDefaultColumns();
    try {
      //fileIcon = new ImageIcon(getClass().getResource("images/WSTable/GenericFile.png"));
      //renamedIcon = new ImageIcon(getClass().getResource("images/WSTable/Renamed.png"));
      //unrenamedIcon = new ImageIcon(getClass().getResource("images/WSTable/Unrenamed.png"));
      //replacedIcon = new ImageIcon(getClass().getResource("images/WSTable/Replaced.png"));
      //unreplacedIcon = new ImageIcon(getClass().getResource("images/WSTable/Unreplaced.png"));
      //addedIcon = new ImageIcon(getClass().getResource("images/WSTable/Added.png"));
      //unaddedIcon = new ImageIcon(getClass().getResource("images/WSTable/Unadded.png"));

      fileIcon = LookAndFeelManager.getImageIcon("images/WSTable/GenericFile.png");
      renamedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Renamed.png");
      unrenamedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Unrenamed.png");
      replacedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Replaced.png");
      unreplacedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Unreplaced.png");
      addedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Added.png");
      unaddedIcon = LookAndFeelManager.getImageIcon("images/WSTable/Unadded.png");

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}