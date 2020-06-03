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

package org.watto.ge.helper;

import java.io.File;
import org.watto.Settings;
import org.watto.component.WSPluginGroup;
import org.watto.component.WSPluginManager;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.FileListExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.task.Task;
import org.watto.task.Task_ExportFileList;
import org.watto.task.Task_ExportFiles;
import org.watto.task.Task_ReadArchive;

public class CommandLineProcessor {

  /**
  **********************************************************************************************
  Processes arguments provided to the command line
  **********************************************************************************************
  **/
  public CommandLineProcessor() {
  }

  /**
  **********************************************************************************************
  Runs an Extract from the command line
  **********************************************************************************************
  **/
  public void commandLineExtract(String input, String output, String convert, String filter) {
    if (output == null || input == null) {
      return; // force terminate
    }

    boolean archiveOpened = commandLineReadArchive(input);
    if (!archiveOpened) {
      return; // force terminate
    }

    // Convert the "convert" into a Viewer plugin
    ViewerPlugin plugin = null;
    if (convert != null) {
      WSPluginGroup group = WSPluginManager.getGroup("Viewer");
      if (group == null) {
        System.out.println("Error: No converter plugins loaded");
        return;
      }
      if (convert.equalsIgnoreCase("BMP")) {
        plugin = (ViewerPlugin) group.getPlugin("BMP_BMP");
      }
      else if (convert.equalsIgnoreCase("DXT1")) {
        plugin = (ViewerPlugin) group.getPlugin("DDS_DDS_Writer_DXT1");
      }
      else if (convert.equalsIgnoreCase("DXT3")) {
        plugin = (ViewerPlugin) group.getPlugin("DDS_DDS_Writer_DXT3");
      }
      else if (convert.equalsIgnoreCase("DXT5")) {
        plugin = (ViewerPlugin) group.getPlugin("DDS_DDS_Writer_DXT5");
      }
      else if (convert.equalsIgnoreCase("GIF")) {
        plugin = (ViewerPlugin) group.getPlugin("GIF_GIF");
      }
      else if (convert.equalsIgnoreCase("JPG")) {
        plugin = (ViewerPlugin) group.getPlugin("JPEG_JFIF");
      }
      else if (convert.equalsIgnoreCase("PCX")) {
        plugin = (ViewerPlugin) group.getPlugin("PCX");
      }
      else if (convert.equalsIgnoreCase("PNG")) {
        plugin = (ViewerPlugin) group.getPlugin("PNG_PNG");
      }
      else if (convert.equalsIgnoreCase("TGA")) {
        plugin = (ViewerPlugin) group.getPlugin("TGA");
      }

      if (plugin == null) {
        System.out.println("Error: Could not find the converter plugin for format \"" + convert + "\"");
        return;
      }
    }

    // Filter the files based on "filter".
    Resource[] resources = Archive.getResources();
    if (filter != null) {
      int numResources = resources.length;
      Resource[] newResources = new Resource[numResources];
      int realNumResources = 0;

      for (int r = 0; r < numResources; r++) {
        Resource resource = resources[r];
        if (resource.getName().matches(filter)) {
          newResources[realNumResources] = resource;
          realNumResources++;
        }
      }

      if (realNumResources == numResources) {
        // no change - selected them all
      }
      else {
        resources = new Resource[realNumResources];
        System.arraycopy(newResources, 0, resources, 0, realNumResources);
      }
    }

    // Extract the files to disk (and set the converter, if one was chosen)
    Task_ExportFiles task = new Task_ExportFiles(new File(output), resources);
    task.setShowPopups(false);
    if (plugin != null) {
      task.setConverterPlugins(new ViewerPlugin[] { plugin });
    }
    task.redo();

    System.out.println("Finished extracting files");
  }

  /**
  **********************************************************************************************
  Runs a List from the command line
  @param input The archive file to examine
  @param output A filename to save the list to
  @param format The format of the list data (CSV, EXCEL, HTML, TABBED, XML)
  @param fields The fields to include in the list (All, Compressed, CompressedKB, CompressionType, Decompressed, DecompressedKB, Description, Directory, Extension, FilePath, Filename, Offset, SourceFile)
  @param filter Only files that match the regex expression will be listed
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  public void commandLineList(String input, String output, String format, String[] fields, String filter) {
    if (output == null || input == null) {
      return; // force terminate
    }

    boolean archiveOpened = commandLineReadArchive(input);
    if (!archiveOpened) {
      return; // force terminate
    }

    // Convert the "format" into a FileListExporter plugin
    FileListExporterPlugin plugin = null;
    if (format != null) {
      WSPluginGroup group = WSPluginManager.getGroup("FileListExporter");
      if (group == null) {
        System.out.println("Error: No file list exporter plugins loaded");
        return;
      }
      if (format.equalsIgnoreCase("CSV")) {
        plugin = (FileListExporterPlugin) group.getPlugin("csv");
      }
      else if (format.equalsIgnoreCase("EXCEL")) {
        plugin = (FileListExporterPlugin) group.getPlugin("xls");
      }
      else if (format.equalsIgnoreCase("HTML")) {
        plugin = (FileListExporterPlugin) group.getPlugin("html");
      }
      else if (format.equalsIgnoreCase("JSON")) {
        plugin = (FileListExporterPlugin) group.getPlugin("json");
      }
      else if (format.equalsIgnoreCase("TABBED")) {
        plugin = (FileListExporterPlugin) group.getPlugin("txt");
      }
      else if (format.equalsIgnoreCase("XML")) {
        plugin = (FileListExporterPlugin) group.getPlugin("xml");
      }

      if (plugin == null) {
        System.out.println("Error: Could not find the file list exporter plugin for format \"" + format + "\"");
        return;
      }
    }

    // Convert the "fields" into WSTableColumn[] columns
    WSTableColumn[] allColumns = Archive.getColumns();
    int numColumns = allColumns.length;

    WSTableColumn[] columns = new WSTableColumn[numColumns];
    int realNumColumns = 0;
    for (int i = 0; i < numColumns; i++) {
      // exclude columns of type "Icon"
      Class type = allColumns[i].getType();
      if (type != String.class && type != Long.class && type != Boolean.class) {
        continue; // don't want this column
      }

      // See if this column is one of the columns chosen by the user
      if (fields == null) {
        // include all columns by default
        columns[realNumColumns] = allColumns[i];
        realNumColumns++;
        continue;
      }

      String columnCode = allColumns[i].getCode();
      int numFields = fields.length;
      for (int f = 0; f < numFields; f++) {
        String field = fields[f];
        if (field.equalsIgnoreCase("All")) {
          columns[realNumColumns] = allColumns[i];
          realNumColumns++;
          break;
        }
        else if (columnCode.equals("Compressed")) {
          if (field.equalsIgnoreCase("CompressedLength")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("CompressedLengthKB")) {
          if (field.equalsIgnoreCase("CompressedKB")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Compression")) {
          if (field.equalsIgnoreCase("CompressionType")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Decompressed")) {
          if (field.equalsIgnoreCase("DecompressedLength")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("DecompressedLengthKB")) {
          if (field.equalsIgnoreCase("DecompressedKB")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Description")) {
          if (field.equalsIgnoreCase("Description")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Directory")) {
          if (field.equalsIgnoreCase("Directory")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Extension")) {
          if (field.equalsIgnoreCase("Extension")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("FilePath")) {
          if (field.equalsIgnoreCase("FilePath")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Filename")) {
          if (field.equalsIgnoreCase("Filename")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Offset")) {
          if (field.equalsIgnoreCase("Offset")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
        else if (columnCode.equals("Source")) {
          if (field.equalsIgnoreCase("SourceFile")) {
            columns[realNumColumns] = allColumns[i];
            realNumColumns++;
            break;
          }
        }
      }

    }

    // shrink the columns array
    if (realNumColumns < numColumns) {
      allColumns = columns;
      columns = new WSTableColumn[realNumColumns];
      System.arraycopy(allColumns, 0, columns, 0, realNumColumns);
    }

    // Filter the files based on "filter". The next step reads directly from Archive.getResources(),
    // so we actually need to apply the filter and remove the non-matching Resources from Archive
    // before we run the export below.
    Resource[] resources = Archive.getResources();
    if (filter != null) {
      int numResources = resources.length;
      Resource[] newResources = new Resource[numResources];
      int realNumResources = 0;

      for (int r = 0; r < numResources; r++) {
        Resource resource = resources[r];
        if (resource.getName().matches(filter)) {
          newResources[realNumResources] = resource;
          realNumResources++;
        }
      }

      if (realNumResources == numResources) {
        // no change - selected them all
      }
      else {
        resources = new Resource[realNumResources];
        System.arraycopy(newResources, 0, resources, 0, realNumResources);
      }
    }

    if (resources.length <= 0) {
      System.out.println("Error: The \"-filter\" did not match any files in the archive.");
      return;
    }

    // Set the Resources back on the Archive
    Archive.setResources(resources);

    // Export the list to disk
    Task_ExportFileList task = new Task_ExportFileList(plugin, columns, new File(output));
    task.setShowPopups(false);
    task.redo();

    System.out.println("Finished listing files");
  }

  /**
  **********************************************************************************************
  NOT CALLED FROM THE COMMAND LINE DIRECTLY. This is a helper method to open an archive
  **********************************************************************************************
  **/
  public boolean commandLineReadArchive(String input) {

    File fileToOpen = new File(input);
    if (!fileToOpen.exists()) {
      System.out.println("Error: Could not open the file \"" + input);
      return false;
    }

    Task_ReadArchive task = new Task_ReadArchive(fileToOpen);
    task.setDirection(Task.DIRECTION_REDO);
    task.run();

    if (Archive.getNumFiles() >= 1) {
      return true;
    }

    System.out.println("Error: The archive could not be opened, or contained no files.");
    return false;
  }

  /**
   **********************************************************************************************
   Run things via the command line instead of the GUI
   **********************************************************************************************
   **/
  public void processCommandLine(String[] args) {
    String action = null;
    String input = null;
    String output = null;
    String convert = null;
    String filter = null;
    String format = null;
    String[] fields = new String[0];

    System.out.println("Game Extractor " + Settings.getDouble("Version"));
    System.out.println("===========================================================");

    int numArgs = args.length;
    for (int i = 0; i < numArgs; i++) {
      String arg = args[i];
      if (arg.equals("-help")) {
        System.out.println("-extract               Extract files from an archive");
        System.out.println("  -input <file>        The archive file to extract");
        System.out.println("  -output <directory>  The directory to store the extracted files");
        System.out.println("  [-convert <format>]  If images are found, convert them to this format");
        System.out.println("                       (BMP, DXT1, DXT3, DXT5, GIF, JPG, PCX, PNG, TGA)");
        System.out.println("  [-filter <regex>]    Only files that match the regex expression will be");
        System.out.println("                       exported");
        System.out.println("");
        System.out.println("-list                  List the contents of an archive without extracting them");
        System.out.println("  -input <file>        The archive file to examine");
        System.out.println("  -output <file>       A filename to save the list to");
        System.out.println("  [-format <format>]   The format of the list data");
        System.out.println("                       (CSV, EXCEL, HTML, JSON, TABBED, XML)");
        System.out.println("  [-fields <f1,f2,*>]  The fields to include in the list");
        System.out.println("                       (All, Compressed, CompressedKB, CompressionType,");
        System.out.println("                       Decompressed, DecompressedKB, Description, Directory,");
        System.out.println("                       Extension, FilePath, Filename, Offset, SourceFile)");
        System.out.println("  [-filter <regex>]    Only files that match the regex expression will be");
        System.out.println("                       listeded");
        return;
      }
      else if (arg.equals("-extract")) {
        action = "extract";
      }
      else if (arg.equals("-list")) {
        action = "list";
      }
      else if (arg.equals("-input")) {
        // check that the next arg contains an existing file
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-input\" must be followed by the name of an input file");
          return; // force terminate
        }

        input = args[i + 1];
        i++;

        File inputFileObject = new File(input);
        if (!inputFileObject.exists()) {
          System.out.println("Error: Could not find the file specified by \"-input\"");
          return; // force terminate
        }
      }
      else if (arg.equals("-output")) {
        // check that the next arg is a folder, or doesn't exist
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-output\" must be followed by the name of an output folder");
          return; // force terminate
        }

        output = args[i + 1];
        i++;

        File outputFileObject = new File(output);
        if (outputFileObject.exists() && !outputFileObject.isDirectory()) {
          System.out.println("Error: The \"-output\" location exists, but is not a directory.");
          return; // force terminate
        }
      }
      else if (arg.equals("-convert")) {
        // check that the next arg is one of the converter formats
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-convert\" must be followed by the name of a conversion format");
          return; // force terminate
        }

        convert = args[i + 1];
        i++;

        String[] converters = new String[] { "BMP", "DXT1", "DXT3", "DXT5", "GIF", "JPG", "PCX", "PNG", "TGA" };
        boolean validConverter = false;
        for (int c = 0; c < converters.length; c++) {
          if (convert.equalsIgnoreCase(converters[c])) {
            validConverter = true;
            break;
          }
        }
        if (!validConverter) {
          System.out.println("Error: The \"-convert\" value \"" + convert + "\" is not valid");
          return; // force terminate
        }
      }
      else if (arg.equals("-filter")) {
        // check that the next arg exists
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-filter\" must be followed by a regular expression");
          return; // force terminate
        }

        filter = args[i + 1];
        i++;
      }
      else if (arg.equals("-format")) {
        // check that the next arg is one of the format formats
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-format\" must be followed by the name of a list format");
          return; // force terminate
        }

        format = args[i + 1];
        i++;

        String[] formats = new String[] { "CSV", "EXCEL", "HTML", "JSON", "TABBED", "XML" };
        boolean validFormat = false;
        for (int c = 0; c < formats.length; c++) {
          if (format.equalsIgnoreCase(formats[c])) {
            validFormat = true;
            break;
          }
        }
        if (!validFormat) {
          System.out.println("Error: The \"-format\" value \"" + format + "\" is not valid");
          return; // force terminate
        }
      }
      else if (arg.equals("-fields")) {
        // check that the next arg contains a list of fields
        if (i + 1 >= numArgs) {
          System.out.println("Error: \"-fields\" must be followed by a list of fields");
          return; // force terminate
        }

        String fieldsValue = args[i + 1];
        i++;

        fields = fieldsValue.split(",");

        String[] values = new String[] { "All", "Compressed", "CompressedKB", "CompressionType", "Decompressed", "DecompressedKB", "Description", "Directory", "Extension", "FilePath", "Filename", "Offset", "SourceFile" };
        for (int c = 0; c < fields.length; c++) {
          boolean validField = false;
          String field = fields[c];

          for (int f = 0; f < values.length; f++) {
            if (field.equalsIgnoreCase(values[f])) {
              validField = true;
              break;
            }
          }
          if (!validField) {
            System.out.println("Error: The \"-fields\" value \"" + field + "\" is not valid");
            return; // force terminate
          }
        }
      }
    }

    // Now we have finished processing all the args. So, if we have a valid action, lets process it.
    if (action.equals("extract")) {
      // check that we have all the mandatory fields
      if (input == null) {
        System.out.println("Error: \"-extract\" is missing the mandatory field \"-input\"");
        return; // force terminate
      }
      if (output == null) {
        System.out.println("Error: \"-extract\" is missing the mandatory field \"-output\"");
        return; // force terminate
      }

      // now run the extract
      commandLineExtract(input, output, convert, filter);
    }
    else if (action.equals("list")) {
      // check that we have all the mandatory fields
      if (input == null) {
        System.out.println("Error: \"-list\" is missing the mandatory field \"-input\"");
        return; // force terminate
      }
      if (output == null) {
        System.out.println("Error: \"-list\" is missing the mandatory field \"-output\"");
        return; // force terminate
      }

      // Check that, if the output file is specified, that they also specify a format
      if (output != null && format == null) {
        System.out.println("Error: \"-list\" with \"-output\" requires you to specify a \"-format\"");
        return; // force terminate
      }

      // now run the extract
      commandLineList(input, output, format, fields, filter);
    }

  }

}
