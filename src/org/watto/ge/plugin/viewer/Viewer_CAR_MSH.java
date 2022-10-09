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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_CAR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_CAR_MSH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_CAR_MSH() {
    super("CAR_MSH", "Carnivores MSH Mesh");
    setExtensions("msh");

    setGames("Carnivores",
        "Carnivores 2",
        "Carnivores: Ice Age");
    setPlatforms("PC");
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_CAR) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (FieldValidator.checkNumVertices(fm.readInt())) {
        rating += 5;
      }

      if (FieldValidator.checkNumFaces(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  float minX = 20000f;

  float maxX = -20000f;

  float minY = 20000f;

  float maxY = -20000f;

  float minZ = 20000f;

  float maxZ = -20000f;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      boolean rendered = false;

      // 4 - Number Of Points
      int numVertices = fm.readInt();
      FieldValidator.checkNumVertices(numVertices);

      // 4 - Number Of Triangles
      int numFaces = fm.readInt();
      FieldValidator.checkNumVertices(numFaces);

      // 4 - Texture Length
      fm.skip(4);

      //
      // FACES
      //

      // in this file, numFaces is actually the number of triangles )not the number of face indexes), so need to do *3 to get the number of face indexes
      int numFaces3 = numFaces * 3;
      FieldValidator.checkNumFaces(numFaces3);
      int numFaces6 = numFaces3 * 2;

      //if (texture == null) {
      //// no texture - load front and back faces
      faces = new int[numFaces6]; // need to store front and back faces

      for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
        // 4 - Vertex 1
        // 4 - Vertex 2
        // 4 - Vertex 3

        int facePoint1 = fm.readInt();
        int facePoint2 = fm.readInt();
        int facePoint3 = fm.readInt();

        // 4 - Texture U 1
        // 4 - Texture U 2
        // 4 - Texture U 3
        // 4 - Texture V 1
        // 4 - Texture V 2
        // 4 - Texture V 3
        // 4 - Flags
        // 4 - Unknown
        // 4 - Parent Face Index
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(52);

        // reverse face first (so the light shines properly, for this model specifically)
        faces[j] = facePoint3;
        faces[j + 1] = facePoint2;
        faces[j + 2] = facePoint1;

        // forward face second
        faces[j + 3] = facePoint1;
        faces[j + 4] = facePoint2;
        faces[j + 5] = facePoint3;

      }

      //
      // VERTICES
      //

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      int numVertices2 = numVertices * 2;
      texCoords = new float[numVertices2];

      for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {
        // 4 - X Coordinate (Float)
        // 4 - Y Coordinate (Float)
        // 4 - Z Coordinate (Float)
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Attached Bone Index
        fm.skip(4);

        // Skip the texture mapping for now
        float xTexture = 0;
        float yTexture = 0;
        //float xTexture = floats[uvOffset];
        //float yTexture = floats[uvOffset + 1];

        texCoords[k] = xTexture;
        texCoords[k + 1] = yTexture;

        // Calculate the size of the object
        if (xPoint < minX) {
          minX = xPoint;
        }
        if (xPoint > maxX) {
          maxX = xPoint;
        }

        if (yPoint < minY) {
          minY = yPoint;
        }
        if (yPoint > maxY) {
          maxY = yPoint;
        }

        if (zPoint < minZ) {
          minZ = zPoint;
        }
        if (zPoint > maxZ) {
          maxZ = zPoint;
        }
      }

      //
      // RENDER THE MESH
      //

      // we have a full mesh for a single object (including all parts adjusted) - add it to the model
      if (faces != null && points != null && texCoords != null) {
        // Create the Mesh
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);

        faces = null;
        points = null;
        texCoords = null;

        // Create the MeshView

        // calculate the sizes and centers
        float diffX = (maxX - minX);
        float diffY = (maxY - minY);
        float diffZ = (maxZ - minZ);

        float centerX = minX + (diffX / 2);
        float centerY = minY + (diffY / 2);
        float centerZ = minZ + (diffZ / 2);

        Point3D sizes = new Point3D(diffX, diffY, diffZ);
        Point3D center = new Point3D(centerX, centerY, centerZ);

        // return the preview based on a MeshView
        PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);
        return preview;
      }

      return null;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image loadTextureImage(String textureFilename) {
    try {

      textureFilename = textureFilename.toLowerCase();

      // the filename here is .TGA, the filename in the archive is .PC TEXTURE, so we need to change it
      int dotPos = textureFilename.lastIndexOf('.');
      if (dotPos > 0) {
        textureFilename = textureFilename.substring(0, dotPos) + ".pc texture"; // note: lowercase
      }

      // now find the resource
      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().toLowerCase().equals(textureFilename)) {
          // found the right resource
          return loadTextureImage(resources[i]);
        }
      }

      // not found
      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image loadTextureImage(Resource imageResource) {
    try {

      // 1. Open the file
      ByteBuffer buffer = new ByteBuffer((int) imageResource.getLength());
      FileManipulator fm = new FileManipulator(buffer);
      imageResource.extract(fm);
      fm.setFakeFile(new File(imageResource.getName())); // set a fake file here, so that the ViewerPlugins can check the file extension

      // 2. Get all the ViewerPlugins that can read this file type
      RatedPlugin[] plugins = PluginFinder.findPlugins(fm, ViewerPlugin.class); // NOTE: This closes the fm pointer!!!
      if (plugins == null || plugins.length == 0) {
        // no viewer plugins found that will accept this file
        return null;
      }

      Arrays.sort(plugins);

      // re-open the file - it was closed at the end of findPlugins();
      fm = new FileManipulator(buffer);

      // 3. Try each plugin until we find one that can render the file as an ImageResource
      PreviewPanel imagePreviewPanel = null;
      for (int i = 0; i < plugins.length; i++) {
        fm.seek(0); // go back to the start of the file
        imagePreviewPanel = ((ViewerPlugin) plugins[i].getPlugin()).read(fm);

        if (imagePreviewPanel != null) {
          // 4. We have found a plugin that was able to render the image
          break;
        }
      }

      fm.close();

      if (imagePreviewPanel == null || !(imagePreviewPanel instanceof PreviewPanel_Image)) {
        // no plugins were able to open this file successfully
        return null;
      }

      //
      //
      // If we're here, we have a rendered image
      //
      //

      //java.awt.Image image = ((PreviewPanel_Image) imagePreviewPanel).getImage();
      ImageResource imageResourceObj = ((PreviewPanel_Image) imagePreviewPanel).getImageResource();
      imageResourceObj = ImageFormatReader.flipVertically(imageResourceObj); // the previewer flips the image for this format (so the preview displays properly), we need to flip it back
      java.awt.Image image = imageResourceObj.getImage();
      BufferedImage bufImage = null;
      if (image instanceof BufferedImage) {
        bufImage = (BufferedImage) image;
      }
      else {
        bufImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bufImage.createGraphics();
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();
      }

      return SwingFXUtils.toFXImage(bufImage, null);
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
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readThumbnail(FileManipulator source) {
    try {
      PreviewPanel preview = read(source);
      if (preview == null || !(preview instanceof PreviewPanel_3DModel)) {
        return null;
      }

      PreviewPanel_3DModel preview3D = (PreviewPanel_3DModel) preview;

      // generate a thumbnail-sized snapshot
      int thumbnailSize = 150; // bigger than ImageResource, so it is shrunk (and smoothed as a result)
      preview3D.generateSnapshot(thumbnailSize, thumbnailSize);

      java.awt.Image image = preview3D.getImage();
      if (image != null) {
        ImageResource resource = new ImageResource(image, preview3D.getImageWidth(), preview3D.getImageHeight());
        preview3D.onCloseRequest(); // cleanup memory
        return resource;
      }

      preview3D.onCloseRequest(); // cleanup memory

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}