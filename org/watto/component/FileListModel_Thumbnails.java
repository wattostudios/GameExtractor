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

package org.watto.component;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.BlankImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockQuickBMSExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_QuickBMSWrapper;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_Decompression;
import org.watto.task.Task;
import org.watto.task.TaskProgressManager;
import org.watto.task.Task_CreateImageResource;
import org.watto.task.Task_LoadThumbnailLater;
import org.watto.task.Task_QuickBMSBulkExport;

public class FileListModel_Thumbnails extends AbstractTableModel implements FileListModel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  Resource[] resources;

  ArchivePlugin readPlugin;

  int columnCount = 0;

  /** the table that this model belongs to **/
  WSTable table;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListModel_Thumbnails(WSTable table, int columnCount) {

    /*
    try {
      throw new Exception("FileListModel_Thumbnail: Create New Model");
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    */

    this.table = table;
    this.columnCount = columnCount;
    //reload();
    reload(new Resource[0]);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void addTableModelListener(TableModelListener tml) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class getColumnClass(int column) {
    //return Icon.class;
    return Resource.class;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getColumnCount() {
    return columnCount;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getColumnName(int column) {
    return "";
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource(int row, int column) {
    int resourceNumber = row * columnCount + column;

    if (resourceNumber < resources.length) {
      return resources[resourceNumber];
    }
    else {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getRowCount() {
    int numResources = resources.length;

    if (numResources <= 0) {
      return 0;
    }

    int numRows = numResources / columnCount;
    if (numResources % columnCount > 0) {
      numRows++;
    }
    return numRows;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object getValueAt(int row, int column) {
    Resource resource = getResource(row, column);
    if (resource == null) {
      // probably a padding cell for the end of the table
      return null;
    }

    //System.out.println("Requesting thumbnail for " + resource.getName());

    if (resource.getImageResource() == null) {
      /*
      // Extract the file and load the thumbnail for it
      Task_CreateImageResource task = new Task_CreateImageResource(resource);
      task.redo();
      */

      // Generate a default thumbnail first...
      resource.setImageResource(new BlankImageResource(resource));

      // Add a job to the end of the Swing EventDispatchThread to generate a real thumbnail and display it,
      // which will be invoked *after* the whole table is drawn.

      // First, are we using the QuickBMS plugin? If so, we want to have a single task to extract all the required files first (in bulk),
      // and then trigger the thumbnails (the LoadThumbnailLater Tasks get added behind the Extract task in the queue)
      if (resource.getExportedPath() == null) {
        ExporterPlugin exporter = resource.getExporter();
        if (exporter instanceof Exporter_QuickBMSWrapper || exporter instanceof Exporter_QuickBMS_Decompression || exporter instanceof BlockQuickBMSExporterWrapper) {
          if (SingletonManager.has("QuickBMSBulkExportTask")) {
            Task_QuickBMSBulkExport task = (Task_QuickBMSBulkExport) SingletonManager.get("QuickBMSBulkExportTask");
            task.addResourceToExtract(resource);
          }
          else {
            Task_QuickBMSBulkExport task = new Task_QuickBMSBulkExport(resource);
            task.setDirection(Task.DIRECTION_REDO);
            SwingUtilities.invokeLater(task);

            SingletonManager.add("QuickBMSBulkExportTask", task);
          }
        }
      }

      // Otherwise, in all normal cases, just trigger the extract and thumbnail generation as part of GameExtractor
      Task_LoadThumbnailLater task = new Task_LoadThumbnailLater(resource, table, this, row, column);
      task.setDirection(Task.DIRECTION_REDO);
      SwingUtilities.invokeLater(task);

    }

    //Image thumbnail = resource.getThumbnail();
    //return thumbnail;
    return resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean isCellEditable(int row, int column) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload() {
    reload(Archive.getResources());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload(Resource[] resources) {

    /*
    try {
      throw new Exception("FileListModel_Thumbnail: Trigger reload");
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    */

    this.resources = resources;
    readPlugin = Archive.getReadPlugin();

    if (Settings.getBoolean("LoadAllThumbnailsWhenOpeningArchive")) {
      // If the user wants to load all the thumbnails straight up, allow it, but it's not the default, and takes a long time to load them all
      int resourceCount = resources.length;

      // Progress dialog

      TaskProgressManager.show(2, 0, Language.get("Progress_LoadingThumbnails")); // 2 progress bars
      TaskProgressManager.setIndeterminate(true, 0); // first 1 is indeterminate

      //try {
      //  SwingUtilities.invokeAndWait(new Task_ShowProgressDialog());
      //}
      //catch (Throwable t) {
      //  ErrorLogger.log(t);
      //}

      TaskProgressManager.setMaximum(resourceCount, 1); // second one shows how many files are done

      for (int i = 0; i < resourceCount; i++) {
        Resource resource = resources[i];
        //System.out.println(resource.getName());
        if (resource.getImageResource() == null) {
          // Extract the file and load the thumbnail for it
          Task_CreateImageResource task = new Task_CreateImageResource(resource);
          task.redo();
          TaskProgressManager.setValue(i, 1); // update the value of the second progress bar
        }
      }

      TaskProgressManager.setVisible(false);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void removeTableModelListener(TableModelListener tml) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setValueAt(Object value, int row, int column) {
  }

}