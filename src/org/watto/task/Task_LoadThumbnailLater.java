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

package org.watto.task;

import java.awt.Rectangle;
import java.io.File;
import java.util.Arrays;

import javax.swing.table.AbstractTableModel;

import org.watto.Language;
import org.watto.SingletonManager;
import org.watto.component.WSTable;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB5_ProcessWithinArchive;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ExporterByteBuffer;

/**
**********************************************************************************************
When given a Resource, will extract the file to a BufferArrayManipulator using an ExtractPlugin,
then use a ViewerPlugin to generate the ImageResource (if it's an image file). This task should
be added to the end of the Swing Event Dispatch Thread via SwingUtilities.invokeLater() so that
*after* the whole table is drawn, it will start loading the thumbnails that were requested and
will update the table thumbnails accordingly.
**********************************************************************************************
**/
public class Task_LoadThumbnailLater extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Resource resource;

  /** the table to inform once the thumbnail has been generated, so it can repaint that cell **/
  WSTable tableToUpdate = null;

  /** the TableModel to inform once the thumbnail has been generated, so it can repaint that cell **/
  AbstractTableModel tableModelToUpdate = null;

  /** The row that this table cell belongs to **/
  int tableCellRow = -1;

  /** The column that this table cell belongs to **/
  int tableCellColumn = -1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_LoadThumbnailLater(Resource resource, WSTable tableToUpdate, AbstractTableModel tableModelToUpdate, int tableCellRow, int tableCellColumn) {
    this.resource = resource;
    this.tableToUpdate = tableToUpdate;
    this.tableModelToUpdate = tableModelToUpdate;
    this.tableCellRow = tableCellRow;
    this.tableCellColumn = tableCellColumn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {

    if (resource.getLength() <= 0) {
      return; // can't view an empty file
    }

    SingletonManager.set("CurrentResource", resource); // so it can be detected by ViewerPlugins for Thumbnail Generation

    //if (resource.getName().equals("Unnamed File 000960")) {
    //  System.out.println("TASK_LOADTHUMBNAILLATER_BREAKPOINT");
    //}

    FileManipulator fm = null;
    // See if the Resource has been exported already - if it has, read from that file instead of the original archive.
    File exportedPath = resource.getExportedPath();
    if (exportedPath != null && exportedPath.exists()) {
      // already exported - read from disk
      fm = new FileManipulator(exportedPath, false);
      //System.out.println("Loading Thumbnail for " + resource.getName() + " (already exported)");
    }
    else {
      // Need to read the file from the archive
      //System.out.println("Loading Thumbnail for " + resource.getName() + " (NEEDS EXPORTING)");

      if (resource.getExporter() instanceof Exporter_Custom_FSB5_ProcessWithinArchive) {
        return; // SPECIAL CASE: this exporter is a bit intensive, and it doesn't generate thumbnails, so skip it early.
      }

      // Create a buffer that reads from the exporter
      /*
      long length = resource.getLength();
      long decompLength = resource.getDecompressedLength();
      
      int maxSize = 65536;
      if (length > maxSize) {
        resource.setLength(maxSize);
      }
      if (decompLength > maxSize) {
        resource.setDecompressedLength(maxSize);
      }
      
      ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
      
      resource.setLength(length);
      resource.setDecompressedLength(decompLength);
      */
      ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);

      fm = new FileManipulator(byteBuffer);
      // Need to set a fake file, so that the ViewerPlugins can get the extension when running getMatchRating()
      fm.setFakeFile(new File(resource.getName()));
    }

    //if (resource.getName().equalsIgnoreCase("CHARS\\CREATURES\\DUPLO_CHOMPERLEGO\\DUPLO_CHOMPERLEGO_UK.TXT")) {
    // System.out.println("LoadThumbnailLater: Starting load for " + resource.getName());
    //}

    // now find a previewer for the file
    // preview the first selected file

    //System.out.println("Finding Plugins");
    RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
    //System.out.println("    Done");
    if (plugins == null || plugins.length == 0) {
      // no viewer plugins found that will accept this file
      // leave the BlankResource here
      return;
    }

    Arrays.sort(plugins);

    // re-open the file - it was closed at the end of findPlugins();
    if (exportedPath != null) {
      // already exported - read from disk
      fm = new FileManipulator(exportedPath, false);
    }
    else {
      // Need to read the file from the archive
      ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
      fm.open(byteBuffer);
    }

    //System.out.println("Trying Plugins");

    // try to open the preview using each plugin and previewFile(File,Plugin)
    for (int i = 0; i < plugins.length; i++) {

      fm.seek(0); // go back to the start of the file
      ImageResource imageResource = ((ViewerPlugin) plugins[i].getPlugin()).readThumbnail(fm);

      if (imageResource != null) {
        // If the image is animated, remove the animations to clean up those memory areas.
        // We don't really want to consider animated thumbnail images, do we!?
        imageResource.setNextFrame(null);

        // if we don't want to retain the original image data after thumbnail generate, trigger a thumbnail generation now so
        // that we can clean up the memory instantly rather than after the whole archive is loaded.
        //if (Settings.getBoolean("RemoveImageAfterThumbnailGeneration")) {
        imageResource.shrinkToThumbnail();
        //}

        // a plugin opened the file successfully, so if it's an Image, generate and set an ImageResource for it.
        resource.setImageResource(imageResource);

        fm.close();

        // Now that we have the thumbnail, change the value in the table
        if (tableModelToUpdate != null) {
          // paint the cell immediately (as part of this call in the Swing Event Dispatch Thread)
          Rectangle cellRect = tableToUpdate.getCellRect(tableCellRow, tableCellColumn, false);
          tableToUpdate.paintImmediately(cellRect);

          // This is an alternative to the above, but triggers the repaint after *all* the thumbnails are loaded
          //tableModelToUpdate.fireTableCellUpdated(tableCellRow, tableCellColumn);
        }

        return;
      }

    }

    fm.close();

    // no plugins were able to open this file successfully
    // leave the BlankResource here
    return;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
