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

package org.watto.ge.plugin;

import java.io.File;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.WSObjectPlugin;
import org.watto.component.WSPluginException;
import org.watto.component.WSPluginManager;
import org.watto.component.WSTableColumn;
import org.watto.datatype.FileType;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.ReplaceDetails;
import org.watto.datatype.ReplaceDetails_File;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.FileTypeDetector;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.DirectoryBuilder;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************
The ArchivePlugin is one of the most important classes in Game Extractor. A class that extends
from ArchivePlugin is able to read, and possibly write, a particular format of archive.
<br><br>
This class contains many methods and globals that make it easy to write an extending class,
such as methods for reading and writing using different inputs, methods to control the data
displayed by the FileTablePanels and by the FileTableSorter, and globals for verification of
fields when reading an archive.
<br><br>
It also contains methods to allow automatic replacing support with only slight alteration to
the code in your plugin. Methods to assist Game Extractor in the automatic detection of a
compatable read plugin are also supplied.
**********************************************************************************************
**/
public abstract class ArchivePlugin extends WSObjectPlugin {

  /**
  **********************************************************************************************
  Gets a file with the same name, but different extension, to the <i>source</i>
  @param source the source file to use as the name and diretory base
  @param extension the new extension
  @return the file with the same name, different extension
  @throws WSPluginException if the file does not exist.
  **********************************************************************************************
  **/
  public static File getDirectoryFile(File source, String extension) throws WSPluginException {
    return getDirectoryFile(source, extension, true);
  }

  /**
  **********************************************************************************************
  Gets a file with the same name, but different extension, to the <i>source</i>
  @param source the source file to use as the name and diretory base
  @param extension the new extension
  @return the file with the same name, different extension
  @throws WSPluginException if the file does not exist.
  **********************************************************************************************
  **/
  public static File getDirectoryFile(File source, String extension, boolean checkExists) throws WSPluginException {
    String pathName = source.getPath();
    int dotPos = pathName.lastIndexOf(".");
    if (dotPos < 0) {
      throw new WSPluginException("Missing Directory File");
    }

    File path = new File(pathName.substring(0, dotPos) + "." + extension);
    if (checkExists && !path.exists()) {
      throw new WSPluginException("Missing Directory File");
    }

    return path;
  }

  /**
  **********************************************************************************************
  Records the error/exception stack trace in the log file. If debug is enabled, it will also
  write the error to the <i>System.out</i> command prompt
  @param t the <i>Throwable</i> error/exception
  **********************************************************************************************
  **/
  public static void logError(Throwable t) {
    ErrorLogger.log(t);
  }

  /**
  **********************************************************************************************
  Resizes the <i>resources</i> array to the new size, where <i>numResources</i> MUST be smaller
  than the current array length.
  @param resources the array to resize
  @param numResources the new size of the array
  @return the resized array
  **********************************************************************************************
  **/
  public static Resource[] resizeResources(Resource[] resources, int numResources) {
    Resource[] temp = resources;
    resources = new Resource[numResources];
    System.arraycopy(temp, 0, resources, 0, numResources);
    return resources;
  }

  /**
  **********************************************************************************************
  Writes the <i>resource</i> into the <i>destination</i> archive, using the <i>exporter</i> for
  formatting the output.
  @param exporter the exporter that converts a file for writing
  @param resource the file to write
  @param destination the archive to write to.
  **********************************************************************************************
  **/
  public static long write(ExporterPlugin exporter, Resource resource, FileManipulator destination) {
    try {
      long offset = destination.getOffset();
      exporter.pack(resource, destination);
      long compLength = destination.getOffset() - offset;
      return compLength;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Writes the <i>resources</i> into the <i>destination</i> archive, using the <i>exporter</i> for
  formatting the output.
  @param exporter the exporter that converts a file for writing
  @param resources the files to write
  @param destination the archive to write to.
  **********************************************************************************************
  **/
  public static long[] write(ExporterPlugin exporter, Resource[] resources, FileManipulator destination) {
    long[] compLengths = new long[resources.length];
    for (int i = 0; i < resources.length; i++) {
      long length = write(exporter, resources[i], destination);
      TaskProgressManager.setValue(i);
      compLengths[i] = length;
    }
    return compLengths;
  }

  /**
  **********************************************************************************************
  Writes the <i>resource</i> into the <i>destination</i> archive
  @param resource the file to write
  @param destination the archive to write to.
  **********************************************************************************************
  **/
  public static void write(Resource resource, FileManipulator destination) {
    ExporterPlugin exporter = Exporter_Default.getInstance();
    write(exporter, resource, destination);
  }

  /**
  **********************************************************************************************
  Writes the <i>resources</i> into the <i>destination</i> archive
  @param resources the files to write
  @param destination the archive to write to.
  **********************************************************************************************
  **/
  public static void write(Resource[] resources, FileManipulator destination) {
    for (int i = 0; i < resources.length; i++) {
      write(resources[i], destination);
      TaskProgressManager.setValue(i);
    }
  }

  /** Can this plugin read an archive? **/
  protected boolean canRead = true;

  /** Can this plugin write an archive? **/
  protected boolean canWrite = false;

  /** Can this plugin rename files within the archive? **/
  protected boolean canRename = false;

  /** Can this plugin replace files within the archive? **/
  protected boolean canReplace = false;

  /** Can implicit replacing be performed on the resources of this archive? **/
  protected boolean allowImplicitReplacing = false;

  /** Do we allow the automatic file type scanner to interrogate each file in the archive, assuming there are no filenames stored within? **/
  protected boolean allowScanForFileTypes = false;

  /** When replacing files in an archive, does this plugin support converting files into a different format before doing the replace? eg converting from a PNG image to a proprietary format? **/
  protected boolean convertOnReplace = false;

  /** The games that use this archive format **/
  protected String[] games = new String[] { "" };

  /** The default extension of archives complying to this format **/
  protected String[] extensions = new String[] { "" };

  /** The platforms that this archive exists on (such as "PC", "XBox", or "PS2") **/
  protected String[] platforms = new String[] { "" };

  // Extension,Description,Type
  /** Allows you to specify a plugin-specific description for files of a given extension **/
  protected FileType[] fileTypes = new FileType[0];

  /** properties of the archive **/
  protected Resource_Property[] properties = new Resource_Property[0];

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ArchivePlugin() {
    setCode("ArchivePlugin");
    setName("Archive Plugin");
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ArchivePlugin(String code) {
    setCode(code);
    setName(code);
  }

  /**
  **********************************************************************************************
  Constructor
  **********************************************************************************************
  **/
  public ArchivePlugin(String code, String name) {
    setCode(code);
    setName(name);
  }

  /**
  **********************************************************************************************
  Adds the <i>fileTypes</i> into the FileTypeDetector, for use when determining the description
  for a file with a given extension.
  **********************************************************************************************
  **/
  public void addFileTypes() {
    FileTypeDetector.clearSpecificDescriptions();

    for (int i = 0; i < fileTypes.length; i++) {
      FileTypeDetector.addFileType(fileTypes[i]);
    }

  }

  /**
  **********************************************************************************************
  Calculates the size of each resource based on the offsets. This will only work if
  <i>resources</i> is sorted by offset.
  @param resources the resources to set the size of
  @param numFiles the number of files in the array
  @param endOffset the end of the last file in the archive
  @param arcSize the size of the archive, for validation purposes
  **********************************************************************************************
  **/
  public void calculateFileSizes(Resource[] resources, int numFiles, long endOffset, long arcSize) throws Exception {
    numFiles--;

    for (int i = 0; i < numFiles; i++) {
      long length = resources[i + 1].getOffset() - resources[i].getOffset();
      FieldValidator.checkLength(length, arcSize);
      resources[i].setLength(length);
      resources[i].setDecompressedLength(length);
    }

    long length = endOffset - resources[numFiles].getOffset();
    FieldValidator.checkLength(length, arcSize);
    resources[numFiles].setLength(length);
    resources[numFiles].setDecompressedLength(length);
  }

  /**
  **********************************************************************************************
  Calculates the size of each resource based on the offsets. This will only work if
  <i>resources</i> is sorted by offset.
  @param resources the resources to set the size of
  @param arcSize the size of the archive, which is also the end of the last file
  **********************************************************************************************
  **/
  public void calculateFileSizes(Resource[] resources, long arcSize) throws Exception {
    calculateFileSizes(resources, resources.length, arcSize, arcSize);
  }

  /**
  **********************************************************************************************
  Calculates the size of each resource based on the offsets. This will only work if
  <i>resources</i> is sorted by offset.
  @param resources the resources to set the size of
  @param endOffset the end of the last file in the archive
  @param arcSize the size of the archive, for validation purposes
  **********************************************************************************************
  **/
  public void calculateFileSizes(Resource[] resources, long endOffset, long arcSize) throws Exception {
    calculateFileSizes(resources, resources.length, endOffset, arcSize);
  }

  /**
  **********************************************************************************************
  Calculates the size of each file, using the same process as calculateFileSizes(), where the
  <i>resources</i> are sorted descending by offset.
  @param resources the resources to set the size of
  @param numFiles the number of files in the array
  @param endOffset the end of the last file in the archive
  @param arcSize the size of the archive, for validation purposes
  **********************************************************************************************
  **/
  public void calculateFileSizesReverse(Resource[] resources, int numFiles, long endOffset, long arcSize) throws Exception {
    long length = endOffset - resources[0].getOffset();
    FieldValidator.checkLength(length, arcSize);
    resources[0].setLength(length);
    resources[0].setDecompressedLength(length);

    for (int i = 1; i < numFiles; i++) {
      length = resources[i - 1].getOffset() - resources[i].getOffset();
      FieldValidator.checkLength(length, arcSize);
      resources[i].setLength(length);
      resources[i].setDecompressedLength(length);
    }

  }

  /**
  **********************************************************************************************
  Calculates the size of each file, using the same process as calculateFileSizes(), where the
  <i>resources</i> are sorted descending by offset.
  @param resources the resources to set the size of
  @param arcSize the size of the archive, which is also the end of the last file
  **********************************************************************************************
  **/
  public void calculateFileSizesReverse(Resource[] resources, long arcSize) throws Exception {
    calculateFileSizesReverse(resources, resources.length, arcSize, arcSize);
  }

  /**
  **********************************************************************************************
  Calculates the size of each file, using the same process as calculateFileSizes(), where the
  <i>resources</i> are sorted descending by offset.
  @param resources the resources to set the size of
  @param endOffset the end of the last file in the archive
  @param arcSize the size of the archive, for validation purposes
  **********************************************************************************************
  **/
  public void calculateFileSizesReverse(Resource[] resources, long endOffset, long arcSize) throws Exception {
    calculateFileSizesReverse(resources, resources.length, endOffset, arcSize);
  }

  /**
  **********************************************************************************************
  Calculates the size of the padding, when the <code>length</code> needs to be a multiple of
  <code>multiple</code>
  @return the padding amount
  **********************************************************************************************
  **/
  public static int calculatePadding(int length, int multiple) {
    int padding = length % multiple;
    if (padding == 0) {
      return 0;
    }

    return multiple - padding;
  }

  /**
  **********************************************************************************************
  Calculates the size of the padding, when the <code>length</code> needs to be a multiple of
  <code>multiple</code>
  @return the padding amount
  **********************************************************************************************
  **/
  public static int calculatePadding(long length, int multiple) {
    return calculatePadding((int) length, multiple);
  }

  public boolean canConvertOnReplace() {
    return convertOnReplace;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean canImplicitReplace() {
    return allowImplicitReplacing;
  }

  /**
  **********************************************************************************************
  Can this plugin read an archive?
  @return true if the plugin can read, false if it cannot.
  **********************************************************************************************
  **/
  public boolean canRead() {
    return canRead;
  }

  /**
  **********************************************************************************************
  Gets the value to be shown in the column <i>code</i> for the given <i>resource</i> (AS A STRING - for the icons!)
  AT THE MOMENT, ONLY USED FOR THE MESSAGE IN THE STATUSBAR ON HOVER IN THE FILELISTTABLE
  @param resource the resource to get the value for
  @param code the code of the column to show
  @return the value to display
  **********************************************************************************************
  **/
  /*
  public String getColumnValueString(Resource resource, char code) {
    if (code == 'i'){
      return "";
      }
    else if (code == 'a'){
      if (resource.isAdded()){
        return "true";
        }
      return "false";
      }
    else if (code == 'r'){
      if (resource.isRenamed()){
        return "true";
        }
      return "false";
      }
    else if (code == 'R'){
      if (resource.isReplaced()){
        return "true";
        }
      return "false";
      }
    else {
      return getColumnValue(resource,code).toString();
      }
    }
  */

  /**
  **********************************************************************************************
  Can this plugin rename files within an archive?
  @return true if files can be renamed, false if renaming is not allowed.
  **********************************************************************************************
  **/
  public boolean canRename() {
    return canRename;
  }

  /**
  **********************************************************************************************
  Can this plugin replace files within an archive?
  @return true if files can be replaced, false if replacing is not allowed.
  **********************************************************************************************
  **/
  public boolean canReplace() {
    return (canReplace || allowImplicitReplacing);
  }

  /**
  **********************************************************************************************
  Is the automatic file scanner allowed to run on this archive, assuming filenames are not stored?
  **********************************************************************************************
  **/
  public boolean canScanForFileTypes() {
    return allowScanForFileTypes;
  }

  /**
  **********************************************************************************************
  Can this plugin write archives?
  @return true is the plugin can write archives, false if it cannot.
  **********************************************************************************************
  **/
  public boolean canWrite() {
    return canWrite;
  }

  /**
  **********************************************************************************************
  If this plugin supports the conversion of files into a different format when "replacing", eg
  converting a PNG image into a propriety image format, this is where we do it.
  @param resourceBeingReplaced the Resource in the archive that is being replaced
  @param fileToReplaceWith the file on your PC that will replace the Resource. This file is the
         one that will be converted into a different format, if applicable.
  @return the converted file, if conversion was applicable/successful, else the original fileToReplaceWith
  **********************************************************************************************
  **/
  public File convertOnReplace(Resource resourceBeingReplaced, File fileToReplaceWith) {
    return fileToReplaceWith;
  }

  /**
  **********************************************************************************************
  Gets a list of the allowed functions
  @return the list
  **********************************************************************************************
  **/
  public String getAllowedFunctionsList() {
    String list = "";

    if (canRead) {
      list += Language.get("Description_ReadOperation");
    }
    if (canWrite) {
      if (list.length() > 0) {
        list += ", ";
      }
      list += Language.get("Description_WriteOperation");
    }
    if (canRename) {
      if (list.length() > 0) {
        list += ", ";
      }
      list += Language.get("Description_RenameOperation");
    }
    if (canReplace && !allowImplicitReplacing) {
      if (list.length() > 0) {
        list += ", ";
      }
      list += Language.get("Description_ReplaceOperation");
    }
    if (allowImplicitReplacing) {
      if (list.length() > 0) {
        list += ", ";
      }
      list += Language.get("Description_ImplicitReplaceOperation");
    }

    return list;
  }

  /**
  **********************************************************************************************
  Gets a blank resource of this type, for use when adding resources
  **********************************************************************************************
  **/
  public Resource getBlankResource(File file, String name) {
    return new Resource(file, name);
  }

  /**
  **********************************************************************************************
  Gets all the columns for displaying information in the FileTablePanel
  @return the columns
  **********************************************************************************************
  **/
  public WSTableColumn[] getColumns() {
    return getDefaultColumns();
  }

  /**
  **********************************************************************************************
  Gets the value to be shown in the column <i>code</i> for the given <i>resource</i>
  @param resource the resource to get the value for
  @param code the code of the column to show
  @return the value to display
  **********************************************************************************************
  **/
  public Object getColumnValue(Resource resource, char code) {
    if (resource instanceof Resource_Property) {
      if (code == 'P') {
        return ((Resource_Property) resource).getCode();
      }
      else if (code == 'V') {
        return ((Resource_Property) resource).getValue();
      }
    }

    if (code == 'P') {
      return resource.getName();
    }
    else if (code == 'c') {
      return new Long(resource.getLength());
    }
    else if (code == 'd') {
      return new Long(resource.getDecompressedLength());
    }
    else if (code == 'O') {
      return new Long(resource.getOffset());
    }
    else if (code == 'C') {
      return new Long(getLengthKB(resource.getLength()));
    }
    else if (code == 'D') {
      return new Long(getLengthKB(resource.getDecompressedLength()));
    }
    else if (code == 'i') {
      return resource.getIcon();
    }
    else if (code == 'a') {
      return resource.getAddedIcon();
    }
    else if (code == 'r') {
      return resource.getRenamedIcon();
    }
    else if (code == 'R') {
      return resource.getReplacedIcon();
    }
    else if (code == 'z') {
      return new Boolean(resource.isCompressed());
    }
    else if (code == 'S') {
      return resource.getSource().getAbsolutePath();
    }
    else if (code == 'Z') {
      return resource.getExporter().getName();
    }
    else if (code == 'I') {
      if (Settings.getBoolean("ShowSystemSpecificIcons")) {

        try {
          // checks the filename contains no funny characters.
          File file = FilenameChecker.correctFilename(new File(Settings.get("TempDirectory") + File.separator + resource.getName()));
          if (!file.exists()) {
            // makes the directory
            DirectoryBuilder.buildDirectory(file, false);
            // creates an empty temp file of this name
            file.createNewFile();
          }

          // gets the icon for this file
          return FileSystemView.getFileSystemView().getSystemTypeDescription(file);
        }
        catch (Throwable t) {
          ErrorLogger.log(t);
        }
      }

      return FileTypeDetector.getFileType(resource.getExtension()).getDescription();
    }
    else if (code == 'F') {
      return resource.getDirectory();
    }
    else if (code == 'N') {
      return resource.getFilename();
    }
    else if (code == 'E') {
      return resource.getExtension();
    }
    else {
      return null;
    }
  }

  /**
  **********************************************************************************************
  Gets all the columns for displaying information in the FileTablePanel
  @return the columns
  **********************************************************************************************
  **/
  public WSTableColumn[] getDefaultColumns() {
    WSTableColumn[] columns = new WSTableColumn[17];

    //code,languageCode,class,editable,sortable
    columns[0] = new WSTableColumn("Icon", 'i', Icon.class, false, false, 18, 18); //icon
    columns[1] = new WSTableColumn("AddedIcon", 'a', Icon.class, false, false, 18, 18); //added
    columns[2] = new WSTableColumn("RenamedIcon", 'r', Icon.class, false, false, 18, 18); //renamed
    columns[3] = new WSTableColumn("ReplacedIcon", 'R', Icon.class, false, false, 18, 18); //Replaced
    columns[4] = new WSTableColumn("FilePath", 'P', String.class, true, true); //Path
    columns[5] = new WSTableColumn("Directory", 'F', String.class, true, true); //Folder
    columns[6] = new WSTableColumn("Filename", 'N', String.class, true, true); //Name
    columns[7] = new WSTableColumn("Extension", 'E', String.class, true, true); //Extension
    columns[8] = new WSTableColumn("CompressedLength", 'c', Long.class, false, true); //compressed Length
    columns[9] = new WSTableColumn("CompressedLengthKB", 'C', Long.class, false, true); //Compressed Length (larger multiple)
    columns[10] = new WSTableColumn("DecompressedLength", 'd', Long.class, false, true); //decompressed Length
    columns[11] = new WSTableColumn("DecompressedLengthKB", 'D', Long.class, false, true); //Decompressed Length (larger multiple)
    columns[12] = new WSTableColumn("Offset", 'O', Long.class, false, true); //Offset
    columns[13] = new WSTableColumn("Compressed", 'z', Boolean.class, false, true); //zipped?
    columns[14] = new WSTableColumn("Compression", 'Z', String.class, false, true); //Zipped Type
    columns[15] = new WSTableColumn("Description", 'I', String.class, false, true); //Information
    columns[16] = new WSTableColumn("Source", 'S', String.class, false, true); //Source

    return columns;
  }

  /**
  **********************************************************************************************
  Gets all the columns for displaying information in the FileTablePanel, as well as appending
  the given columns to the end of the list
  @return the default columns plus the appended columns
  **********************************************************************************************
  **/
  public WSTableColumn[] getDefaultColumnsWithAppended(WSTableColumn... appendedColumns) {
    WSTableColumn[] defaultColumns = getDefaultColumns();

    int defaultColumnCount = defaultColumns.length;
    int appendedColumnCount = appendedColumns.length;

    WSTableColumn[] columns = new WSTableColumn[defaultColumnCount + appendedColumnCount];
    System.arraycopy(defaultColumns, 0, columns, 0, defaultColumnCount);
    System.arraycopy(appendedColumns, 0, columns, defaultColumnCount, appendedColumnCount);

    return columns;
  }

  /**
  **********************************************************************************************
  Gets the description of the plugin, such as the games and platforms that are supported, and the
  functions that can be performed.
  @return the description of this plugin
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_ArchivePlugin");

    if (games.length <= 0) {
      description += "\n\n" + Language.get("Description_NoDefaultGames");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultGames");

      for (int i = 0; i < games.length; i++) {
        description += "\n -" + games[i];
      }

    }

    if (platforms.length <= 0) {
      description += "\n\n" + Language.get("Description_NoDefaultPlatforms");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultPlatforms");

      for (int i = 0; i < platforms.length; i++) {
        description += "\n -" + platforms[i];
      }

    }

    if (extensions.length <= 0 || extensions[0].length() == 0) {
      description += "\n\n" + Language.get("Description_NoDefaultExtensions");
    }
    else {
      description += "\n\n" + Language.get("Description_DefaultExtensions") + "\n";

      for (int i = 0; i < extensions.length; i++) {
        if (i > 0) {
          description += " *." + extensions[i];
        }
        else {
          description += "*." + extensions[i];
        }
      }

    }

    description += "\n\n" + Language.get("Description_SupportedOperations");
    if (canRead) {
      description += "\n - " + Language.get("Description_ReadOperation");
    }
    if (canWrite) {
      description += "\n - " + Language.get("Description_WriteOperation");
    }
    if (canRename) {
      description += "\n - " + Language.get("Description_RenameOperation");
    }
    if (canReplace && !allowImplicitReplacing) {
      description += "\n - " + Language.get("Description_ReplaceOperation");
    }
    if (allowImplicitReplacing) {
      description += "\n - " + Language.get("Description_ImplicitReplaceOperation");
    }

    if (convertOnReplace) {
      description += "\n\n" + Language.get("Description_ConvertOnReplace");
    }

    if (allowScanForFileTypes) {
      description += "\n\n" + Language.get("Description_AllowScanForFileTypes");
    }

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;

  }

  /**
  **********************************************************************************************
  Gets the extension at position <i>num</i> of the array
  @param num the extension number
  @return the extension
  **********************************************************************************************
  **/
  public String getExtension(int num) {
    if (num < extensions.length) {
      return extensions[num];
    }
    else {
      return "unk";
    }
  }

  /**
  **********************************************************************************************
  Gets all the extensions
  @return the extensions
  **********************************************************************************************
  **/
  public String[] getExtensions() {
    return extensions;
  }

  /**
  **********************************************************************************************
  Gets a list of the extensions
  @return the list
  **********************************************************************************************
  **/
  public String getExtensionsList() {
    String list = "";

    for (int i = 0; i < extensions.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += "*." + extensions[i];
    }

    return list;
  }

  /**
  **********************************************************************************************
  Gets all the games
  @return the games
  **********************************************************************************************
  **/
  public String[] getGames() {
    return games;
  }

  /**
  **********************************************************************************************
  Gets a list of the games
  @return the list
  **********************************************************************************************
  **/
  public String getGamesList() {
    String list = "";

    for (int i = 0; i < games.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += games[i];
    }

    return list;
  }

  /**
  **********************************************************************************************
  Converts a length from bytes into kilobytes
  @param length the length in bytes
  @return the length in kilobytes
  **********************************************************************************************
  **/
  public long getLengthKB(long length) {
    if (length == 0) {
      return 0;
    }

    length = length / 1024;

    if (length == 0) {
      return 1;
    }
    else {
      return length;
    }
  }

  /**
  **********************************************************************************************
  Gets the percentage chance that this plugin can read the <i>file</i>
  @param file the file to analyse
  @return the percentage (0-100) chance
  **********************************************************************************************
  **/
  public int getMatchRating(File file) {
    try {
      FileManipulator fm = new FileManipulator(file, false);
      int rating = getMatchRating(fm);
      fm.close();
      return rating;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Gets the percentage chance that this plugin can read the file <i>fm</i>
  @param fm the file to analyse
  @return the percentage (0-100) chance
  **********************************************************************************************
  **/
  public abstract int getMatchRating(FileManipulator fm);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getNumProperties() {
    return properties.length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String[] getPlatforms() {
    return platforms;
  }

  /**
  **********************************************************************************************
  Gets a list of the platforms
  @return the list
  **********************************************************************************************
  **/
  public String getPlatformsList() {
    String list = "";

    for (int i = 0; i < platforms.length; i++) {
      if (i > 0) {
        list += ", ";
      }
      list += platforms[i];
    }

    return list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property[] getProperties() {
    return properties;
  }

  /**
  **********************************************************************************************
  Gets only the columns that are being shown in the FileListPanel (ie not the columns that have
  been hidden in the Settings.
  @return the columns that will be shown
  **********************************************************************************************
  **/
  public WSTableColumn getViewingColumn(int column) {
    WSTableColumn[] columns = getColumns();

    int numCols = 0;
    for (int i = 0; i < columns.length; i++) {
      if (Settings.getBoolean("ShowTableColumn_" + columns[i].getCode())) {
        if (numCols == column) {
          return columns[i];
        }
        numCols++;
      }
    }

    return null;
  }

  /**
  **********************************************************************************************
  Gets only the columns that are being shown in the FileListPanel (ie not the columns that have
  been hidden in the Settings.
  @return the columns that will be shown
  **********************************************************************************************
  **/
  public WSTableColumn[] getViewingColumns() {
    WSTableColumn[] columns = getColumns();

    boolean showCustomColumns = Settings.getBoolean("ShowCustomColumns");

    int numColumns = columns.length;
    if (!showCustomColumns) {
      numColumns = 17;
    }

    int numTexts = 0;
    for (int i = 0; i < numColumns; i++) {
      if (Settings.getBoolean("ShowTableColumn_" + columns[i].getCode())) {
        columns[numTexts] = columns[i];
        numTexts++;
      }
      else if (i >= 17 && showCustomColumns) {
        // want to show the custom columns
        columns[numTexts] = columns[i];
        numTexts++;
      }
    }

    if (numTexts < columns.length) {
      WSTableColumn[] temp = columns;
      columns = new WSTableColumn[numTexts];
      System.arraycopy(temp, 0, columns, 0, numTexts);
    }

    return columns;
  }

  /**
  **********************************************************************************************
  Gets only the columns that are being shown in the FileListPanel (ie not the columns that have
  been hidden in the Settings.)
  @return the columns that will be shown
  **********************************************************************************************
  **/
  public WSTableColumn getViewingPropColumn(int column) {
    WSTableColumn[] columns = getViewingPropColumns();

    int numCols = 0;
    for (int i = 0; i < columns.length; i++) {
      if (numCols == column) {
        return columns[i];
      }
      numCols++;
    }

    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSTableColumn[] getViewingPropColumns() {

    WSTableColumn[] columns = new WSTableColumn[3];

    //code,languageCode,class,editable,sortable
    columns[0] = new WSTableColumn("RenamedIcon", 'r', Icon.class, false, false, 18, 18); //renamed
    columns[1] = new WSTableColumn("Property", 'P', String.class, false, true);
    columns[2] = new WSTableColumn("Value", 'V', String.class, true, true);

    int numTexts = 0;
    for (int i = 0; i < columns.length; i++) {
      if (Settings.getBoolean("ShowTableColumn_" + columns[i].getCode())) {
        columns[numTexts] = columns[i];
        numTexts++;
      }
    }

    if (numTexts < columns.length) {
      WSTableColumn[] temp = columns;
      columns = new WSTableColumn[numTexts];
      System.arraycopy(temp, 0, columns, 0, numTexts);
    }

    return columns;
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {
    return null;
  }

  /** A list of file extensions, that will be viewed as TXT files **/
  String[] textPreviewExtensions = null; // LOWER CASE

  /**
  **********************************************************************************************
  Gets the file extensions that will be viewed as TXT files
  **********************************************************************************************
  **/
  public String[] getTextPreviewExtensions() {
    return textPreviewExtensions;
  }

  /**
  **********************************************************************************************
  Sets the file extensions that will be viewed as TXT files
  **********************************************************************************************
  **/
  public void setTextPreviewExtensions(String... textPreviewExtensions) {
    this.textPreviewExtensions = textPreviewExtensions;

    // also load these into the file types, so they render thumbnails as TXT images
    FileType[] oldTypes = fileTypes;
    int numOldTypes = oldTypes.length;
    int numNewTypes = textPreviewExtensions.length;
    int totalTypes = numOldTypes + numNewTypes;

    fileTypes = new FileType[totalTypes];
    System.arraycopy(oldTypes, 0, fileTypes, 0, numOldTypes);

    int writePos = numOldTypes;
    for (int i = 0; i < numNewTypes; i++) {
      String extension = textPreviewExtensions[i];

      // ODD - can't get this to work, even though the debugger shows that they match
      /*
      // check if it's already been added as a file type
      for (int k = 0; k < numOldTypes; k++) {
        String oldExtension = fileTypes[k].getExtension();
        if (extension.equals(oldExtension)) {
          // already exists, don't add it
          continue;
        }
      }
      */

      fileTypes[writePos] = new FileType(extension, extension + " File", FileType.TYPE_DOCUMENT);
      writePos++;
    }

    if (writePos < totalTypes) {
      // didn't add at least 1 extension (probably already existed), so shrink the array
      oldTypes = fileTypes;
      fileTypes = new FileType[writePos];
      System.arraycopy(oldTypes, 0, fileTypes, 0, writePos);
    }

  }

  /**
  **********************************************************************************************
  The Previewer comes here ONLY IF it has tried all Viewer Plugins and didn't find a match. In
  this case, we analyse the Resource and determine how best to view it. Useful for specifying
  certain resources to be force-displayed in Viewer_TXT instead of the Hex Viewer.
  **********************************************************************************************
  **/
  public ViewerPlugin previewHint(Resource resource) {
    // Should be overwritten if the ArchivePlugin wants to force certain Resources to a ViewerPlugin.
    // Otherwise, if you're only setting TXT preview hints, you can just set the extensions in 
    // txtPreviewExtensions[] and they will be shown as TXT by default
    if (textPreviewExtensions != null) {
      String extension = resource.getExtension().toLowerCase();

      int numExtensions = textPreviewExtensions.length;
      for (int i = 0; i < numExtensions; i++) {
        if (extension.equals(textPreviewExtensions[i])) {
          return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
        }
      }
    }
    return null;
  }

  /**
  **********************************************************************************************
  Reads the archive <i>source</i>
  @param source the archive file
  @return the resources in the archive
  **********************************************************************************************
  **/
  public abstract Resource[] read(File source);

  /**
  **********************************************************************************************
  Writes the <i>resources</i> to the archive <i>destination</i>, where the archive was opened and
  modified (as opposed to write() which writes an archive from scratch). This directs to
  write(resources,destination) if the method is not overwritten
  @param resources the files to write
  @param destination the place to store the archive
  **********************************************************************************************
  **/
  public void replace(Resource[] resources, File destination) {
    write(resources, destination);
  }

  public void setCanConvertOnReplace(boolean convertOnReplace) {
    this.convertOnReplace = convertOnReplace;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin supports implicit replacing
  @param canReplace is implicit replacing allowed?
  **********************************************************************************************
  **/
  public void setCanImplicitReplace(boolean canReplace) {
    this.allowImplicitReplacing = canReplace;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin can read archives or not
  @param canRead is reading allowed?
  **********************************************************************************************
  **/
  public void setCanRead(boolean canRead) {
    this.canRead = canRead;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin can rename files in an archive
  @param canRename is renaming allowed?
  **********************************************************************************************
  **/
  public void setCanRename(boolean canRename) {
    this.canRename = canRename;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin can replace files in an archive
  @param canReplace is replacing allowed?
  **********************************************************************************************
  **/
  public void setCanReplace(boolean canReplace) {
    this.canReplace = canReplace;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin allows the automatic file scanner to try determining the type of file
  if there are no filenames stored in this archive
  @param canScan is the file scanner allowed to run?
  **********************************************************************************************
  **/
  public void setCanScanForFileTypes(boolean canScan) {
    this.allowScanForFileTypes = canScan;
  }

  /**
  **********************************************************************************************
  Sets whether this plugin can write archives
  @param canWrite is writing allowed?
  **********************************************************************************************
  **/
  public void setCanWrite(boolean canWrite) {
    this.canWrite = canWrite;
  }

  /**
  **********************************************************************************************
  Gets the value of the <i>resource</i> corresponding to the column <i>code</i>
  @param resource the resource to change the value of
  @param code the code of the column corresponding to the data being changed
  @param value the new value for the field.
  **********************************************************************************************
  **/
  public void setColumnValue(Resource resource, char code, Object value) {
    if (resource instanceof Resource_Property) {
      if (code == 'P') {
        ((Resource_Property) resource).setCode(value.toString());
      }
      else if (code == 'V') {
        ((Resource_Property) resource).setValue(value.toString());
      }
    }

    if (code == 'P') {
      resource.setName((String) value);
    }
    else if (code == 'c') {
      resource.setLength(((Long) value).longValue());
    }
    else if (code == 'd') {
      resource.setDecompressedLength(((Long) value).longValue());
    }
    else if (code == 'O') {
      resource.setOffset(((Long) value).longValue());
    }
    else if (code == 'F') {
      resource.setDirectory((String) value);
    }
    else if (code == 'N') {
      resource.setFilename((String) value);
    }
    else if (code == 'E') {
      resource.setExtension((String) value);
    }
  }

  /**
  **********************************************************************************************
  Sets up the default properties
  **********************************************************************************************
  **/
  public void setDefaultProperties(boolean force) {
    if (force || properties.length == 0) {
      properties = new Resource_Property[0];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExtensions(String... extensions) {
    this.extensions = extensions;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFileTypes(FileType... types) {
    fileTypes = types;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFileTypes(String... types) {
    int numTypes = types.length / 2;

    fileTypes = new FileType[numTypes];

    for (int i = 0, j = 0; i < types.length; i += 2, j++) {
      fileTypes[j] = new FileType(types[i], types[i + 1]);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setGames(String... games) {
    this.games = games;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPlatforms(String... platforms) {
    this.platforms = platforms;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setProperties(boolean canRead, boolean canWrite, boolean canReplace, boolean canRename) {
    this.canRead = canRead;
    this.canWrite = canWrite;
    this.canReplace = canReplace;
    this.canRename = canRename;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setProperties(Resource_Property[] properties) {
    this.properties = properties;
  }

  /**
  **********************************************************************************************
  Writes the <i>resources</i> to the archive <i>destination</i>, where the archive was constructed
  from scratch (as opposed to replace() which writes an archive that was already opened). If
  <i>allowImplicitReplacing</i> is enabled, it will write the archive without the need for
  overwriting this method.
  @param resources the files to write
  @param destination the place to store the archive
  **********************************************************************************************
  **/
  public void write(Resource[] resources, File destination) {
    // Should be overwritten by the plugin

    // Allows implicit replacing
    try {
      if (allowImplicitReplacing) {
        int numFiles = resources.length;

        // build the list of fields to replace
        ReplaceDetails[] fields = new ReplaceDetails[numFiles * 6];

        int copyPos = 0;
        for (int i = 0; i < numFiles; i++) {
          ReplacableResource resource = (ReplacableResource) resources[i];
          ReplaceDetails[] resourceFields = resource.getImplicitReplaceFields();

          if (resourceFields != null) {
            System.arraycopy(resourceFields, 0, fields, copyPos, resourceFields.length);
            copyPos += resourceFields.length;
          }
        }

        if (copyPos == 0) {
          // no files were replaced
          return;
        }

        // sort the fields list by offset
        int numFields = copyPos;

        java.util.Arrays.sort(fields, 0, numFields);

        // Create a holder for all the new offsets, so when we write the files, we know where they are in the archive
        long[] newOffsets = new long[numFields];

        // rebuild the archive
        FileManipulator fm = new FileManipulator(destination, true);
        FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

        long arcSize = src.getLength();
        TaskProgressManager.setMaximum(arcSize);

        for (int i = 0; i < numFields; i++) {
          ReplaceDetails field = fields[i];

          long nextOffset = field.getOffset();

          for (long p = src.getOffset(); p < nextOffset; p++) {
            // copy all the bytes until reaching the next offset that will have replaced data
            fm.writeByte(src.readByte());
          }

          TaskProgressManager.setValue(nextOffset);

          // now we are at the offset to replace some data
          long length = field.getLength();

          if (field instanceof ReplaceDetails_File) {
            // replacing a files' data
            //if (i == 279) {
            //  System.out.println("Writing file at: " + fm.getOffset());
            //}
            ReplacableResource resource = ((ReplaceDetails_File) field).getResource();
            //resource.setImplicitReplacedOffset(fm.getOffset());
            newOffsets[i] = fm.getOffset();
            write(resource.getExporter(), resource, fm);
          }
          else {
            // replacing a directory field like offset or length
            newOffsets[i] = -1; // set a dummy value
            long value = field.getValueWithEndian(); // changes it to big endian if required

            if (field.getName().equals("Offset")) {
              // Remember the location of this field, so we can store the actual value later.
              field.setOffset(fm.getOffset());
              for (long k = 0; k < length; k++) {
                fm.writeByte(0);
              }
            }
            else {
              //System.out.println("Writing length at: " + fm.getOffset());
              if (length == 2) {
                fm.writeShort((short) value);
              }
              else if (length == 4) {
                fm.writeInt((int) value);
              }
              else if (length == 8) {
                fm.writeLong(value);
              }
              else if (length == 0) {
              }
              else {
                throw new WSPluginException("Can not replace file - bad field length");
              }
            }
          }

          // skip over the amount of data being replaced in the old file
          src.skip(length);

        }

        // write the remaining bytes of the archive, if any.
        for (long p = src.getOffset(); p < arcSize; p++) {
          fm.writeByte(src.readByte());
        }

        // Now go through and set the new offsets on the files, so we can write them in the next loop.
        // This needs to be done in a separate loop, for archives where the files are stored in the
        // directory in a DIFFERENT order as in the file data area
        for (int i = 0; i < numFields; i++) {
          long newOffset = newOffsets[i];
          if (newOffset != -1) {
            // This is a file - set the offset to the one in the new archive
            ReplaceDetails field = fields[i];
            ReplacableResource resource = ((ReplaceDetails_File) field).getResource();

            long oldValue = resource.getOffset();
            resource.setOffset(newOffset);
            newOffsets[i] = oldValue; // so we can revert the offset back after we're finished
          }
        }

        // NOW GO BACK AND WRITE THE CORRECT FILE OFFSETS
        for (int i = 0; i < numFields; i++) {
          ReplaceDetails field = fields[i];

          if (field.getName().equals("Offset")) {
            fm.seek(field.getOffset());

            long value = field.getValueWithEndian(); // changes it to big endian if required
            long length = field.getLength();

            //System.out.println("Writing offset \"" + value + "\" at: " + field.getOffset() + " vs actual " + fm.getOffset());

            if (length == 2) {
              fm.writeShort((short) value);
            }
            else if (length == 4) {
              fm.writeInt((int) value);
            }
            else if (length == 8) {
              fm.writeLong(value);
            }
            else {
              throw new WSPluginException("Can not replace file - bad field length");
            }

          }
        }

        // Now go through and set the original offsets on the files
        for (int i = 0; i < numFields; i++) {
          long newOffset = newOffsets[i];
          if (newOffset != -1) {
            // This is a file - set the offset to the original one (which we put in the newOffset array in the first loop)
            ReplaceDetails field = fields[i];
            ReplacableResource resource = ((ReplaceDetails_File) field).getResource();

            resource.setOffset(newOffset);
          }
        }

        fm.close();
        src.close();
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}