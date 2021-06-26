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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ZIP_PK;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZIP_PK_SCM_MODL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZIP_PK_SCM_MODL() {
    super("ZIP_PK_SCM_MODL", "Supreme Commander 2 SCM 3D Model");
    setExtensions("scm");

    setGames("Supreme Commander 2");
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
      if (fm.readString(4).equals("MODL")) {
        rating += 50;
      }

      return rating;

    }
    catch (

    Throwable t) {
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      float minX = 20000f;
      float maxX = -20000f;
      float minY = 20000f;
      float maxY = -20000f;
      float minZ = 20000f;
      float maxZ = -20000f;

      // 4 - Header (MODL)
      // 4 - Unknown (5)
      // 4 - Skeleton Offset [-4]
      // 4 - Number of Skeletons
      fm.skip(16);

      // 4 - Vertex List Offset [-4]
      int vertexOffset = fm.readInt();
      FieldValidator.checkOffset(vertexOffset, arcSize);

      // 4 - null
      fm.skip(4);

      // 4 - Number of Vertices
      int numPoints = fm.readInt();
      FieldValidator.checkNumVertices(numPoints);

      // 4 - Triangles Offset [-4]
      int faceOffset = fm.readInt();
      FieldValidator.checkOffset(faceOffset, arcSize);

      // 4 - Number of Face Indexes (Number Of Triangles = value / 3)
      int numFaces = fm.readInt();
      FieldValidator.checkNumFaces(numFaces);

      // 4 - Information Block Offset [-4]
      // 4 - Information Block Length [+4]
      // 4 - Unknown (2)
      // 12 - Padding to a multiple of 16 bytes, then remove 4 (all (byte) 197)

      //
      //
      // VERTICES
      //
      //
      fm.seek(vertexOffset);

      int numPoints3 = numPoints * 3;
      points = new float[numPoints3];
      normals = new float[numPoints3];

      int numPoints2 = numPoints * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numPoints; i++, j += 3, k += 2) {
        // 4 - Vertex X
        // 4 - Vertex Y
        // 4 - Vertex Z
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Normal X
        // 4 - Normal Y
        // 4 - Normal Z
        float xNormal = fm.readFloat();
        float yNormal = fm.readFloat();
        float zNormal = fm.readFloat();

        normals[j] = xNormal;
        normals[j + 1] = yNormal;
        normals[j + 2] = zNormal;

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(24);

        // 4 - Texture Coordinate X
        // 4 - Texture Coordinate Y
        float xTexture = fm.readFloat();
        float yTexture = fm.readFloat();

        texCoords[k] = xTexture;
        texCoords[k + 1] = yTexture;

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(12);

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
      //
      // FACES
      //
      //
      fm.seek(faceOffset);

      // 4 - Number of Face Indexes
      int numFaces3 = numFaces;

      numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;
      faces = new int[numFaces6]; // need to store front and back faces

      for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
        // 2 - Face Point 1
        // 2 - Face Point 2
        // 2 - Face Point 3
        short facePoint1 = (short) (fm.readShort());
        short facePoint2 = (short) (fm.readShort());
        short facePoint3 = (short) (fm.readShort());

        // reverse face first (so the light shines properly, for this model specifically)
        faces[j] = facePoint3;
        faces[j + 1] = facePoint2;
        faces[j + 2] = facePoint1;

        // forward face second
        faces[j + 3] = facePoint1;
        faces[j + 4] = facePoint2;
        faces[j + 5] = facePoint3;
      }

      if (faces != null && points != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        //triangleMesh.getNormals().addAll(normals);

        faces = null;
        points = null;
        normals = null;
        texCoords = null;
      }

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      Point3D sizes = new Point3D(diffX, diffY, diffZ);
      Point3D center = new Point3D(centerX, centerY, centerZ);

      // Create the MeshView for holding the model and texture
      MeshView meshView = new MeshView(triangleMesh);

      Resource resource = (Resource) SingletonManager.get("CurrentResource");
      String currentResourceName = resource.getName();
      Image image = loadTextureImage(currentResourceName);
      if (image != null) {
        Material material = new PhongMaterial(Color.WHITE, image, (Image) null, (Image) null, (Image) null);
        meshView.setMaterial(material);
      }

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

      return preview;
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
  public Image loadTextureImage(String modelFilename) {
    try {

      // find the texture filename for the model
      String textureFilename = modelFilename.toLowerCase();

      int underscorePos = textureFilename.lastIndexOf('_');
      if (underscorePos > 0) {
        textureFilename = textureFilename.substring(0, underscorePos) + "_albedo.dds"; // NOTE: lowercase!
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

      //
      // THIS WORKS, BUT TAKES A REALLY LONG TIME TO RENDER, SO DON'T BOTHER WITH IMAGES FOR THESE MESHES
      //
      /*
      // try looking for "Team" instead of "Albedo"
      textureFilename = modelFilename.toLowerCase();
      
      if (underscorePos > 0) {
        textureFilename = textureFilename.substring(0, underscorePos) + "_team.win.dds"; // NOTE: lowercase!
      }
      
      // now find the resource
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().toLowerCase().equals(textureFilename)) {
          // found the right resource
          return loadTextureImage(resources[i]);
        }
      }
      */

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