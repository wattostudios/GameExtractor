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

import java.io.File;
import java.util.Arrays;
import org.watto.Language;
import org.watto.SingletonManager;
import org.watto.datatype.BlankImageResource;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ExporterByteBuffer;

/**
**********************************************************************************************
When given a Resource, will extract the file to a BufferArrayManipulator using an ExtractPlugin,
then use a ViewerPlugin to generate the ImageResource (if it's an image file)
**********************************************************************************************
**/
public class Task_CreateImageResource extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Resource resource;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_CreateImageResource(Resource resource) {
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {

    /*
    // THIS WORKS, BUT EXPORTS THE WHOLE FILE TO MEMORY.
    // CHANGED TO BELOW INSTEAD, WHICH SHOULD ONLY READ A LITTLE IN TO MEMORY FOR FINDING THE PLUGINS,
    // AND IF IT FINDS ONE, IT WILL THEN LOAD THE WHOLE FILE TO MEMORY. OTHERWISE, SKIPS THE FILE AND
    // THE MEMORY IS RETURNED.
    
    // create the in-memory buffer for extracting the file to
    long bufferSize = resource.getDecompressedLength();
    ByteBuffer byteBuffer = new ByteBuffer((int) bufferSize);
    FileManipulator fm = new FileManipulator(byteBuffer);
    
    // Need to set a fake file, so that the ViewerPlugins can get the extension when running getMatchRating()
    fm.setFakeFile(new File(resource.getName()));
    
    // do the extract to memory
    resource.extract(fm);
    */

    SingletonManager.set("CurrentResource", resource); // so it can be detected by ViewerPlugins for Thumbnail Generation

    // Create a buffer that reads from the exporter
    ExporterByteBuffer byteBuffer = new ExporterByteBuffer(resource);
    FileManipulator fm = new FileManipulator(byteBuffer);
    // Need to set a fake file, so that the ViewerPlugins can get the extension when running getMatchRating()
    fm.setFakeFile(new File(resource.getName()));

    // now find a previewer for the file
    // preview the first selected file
    RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
    if (plugins == null || plugins.length == 0) {
      // no viewer plugins found that will accept this file
      resource.setImageResource(new BlankImageResource(resource));
      return;
    }

    Arrays.sort(plugins);

    // re-open the file - it was closed at the end of findPlugins();
    byteBuffer = new ExporterByteBuffer(resource);
    fm.open(byteBuffer);

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

        return;
      }

    }

    fm.close();

    // no plugins were able to open this file successfully
    resource.setImageResource(new BlankImageResource(resource));
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
