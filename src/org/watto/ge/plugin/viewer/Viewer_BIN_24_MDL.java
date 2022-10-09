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
import org.watto.ge.plugin.archive.Plugin_BIN_24;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_24_MDL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_24_MDL() {
    super("BIN_24_MDL", "Beyond Good & Evil MDL Mesh Viewer");
    setExtensions("mdl");

    setGames("Beyond Good & Evil");
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
      if (plugin instanceof Plugin_BIN_24) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      int header = fm.readInt();
      if (header == -1 || header == 1895872846) {
        rating += 5;

        if (fm.readByte() == 5 && ByteConverter.unsign(fm.readByte()) == 128) {
          rating += 5;
        }
      }
      else if (header == 1) {
        // 4 - Number of Points
        if (FieldValidator.checkNumVertices(fm.readInt())) {
          rating += 5;
        }
        // 4 - null
        if (fm.readInt() == 0) {
          rating += 5;
        }
        // 4 - Number of Tex Coords?
        if (FieldValidator.checkNumVertices(fm.readInt())) {
          rating += 5;
        }
        // 4 - Number of Parts
        if (FieldValidator.checkNumVertices(fm.readInt())) {
          rating += 5;
        }

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
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = null; // we're using MeshView, as we're setting textures on the mesh

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      boolean rendered = false;

      // 4 - Header (-1)
      int header = fm.readInt();

      if (header == -1 || header == 1895872846) {
        // 
        // MODEL TYPE 1
        //

        // 1 - Unknown (5)
        // 1 - Unknown (128)
        fm.skip(2);

        //
        // VERTICES
        //

        // 4 - Number of Points
        int numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];
        normals = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          // Skip the texture mapping for now
          float xTexture = 0;
          float yTexture = 0;

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

        // 4 - Number of Unknown
        int numUnknown = fm.readInt();
        FieldValidator.checkNumVertices(numUnknown);

        fm.skip(numUnknown * 12);

        // 4 - Number of Parts
        int numParts = fm.readInt();
        FieldValidator.checkRange(numParts, 1, 200); // 200 guess

        meshView = new MeshView[numParts];

        for (int p = 0; p < numParts; p++) {

          //
          // FACES
          //

          // 2 - Number of Face Indexes in this Part
          int numFaces = ShortConverter.unsign(fm.readShort());
          FieldValidator.checkNumFaces(numFaces + 1); // allow zero faces

          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          fm.skip(6);

          int faceDataSize = 2;

          int numFaces3 = numFaces * 3;
          FieldValidator.checkNumFaces(numFaces3 + 1); // allow zero faces

          //numFaces = numFaces3 / 3;
          int numFaces6 = numFaces3 * 2;

          faces = new int[numFaces6]; // need to store front and back faces

          for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
            // 2 - Point Index 1
            // 2 - Point Index 2
            // 2 - Point Index 3
            int facePoint1 = (ShortConverter.unsign(fm.readShort()));
            int facePoint2 = (ShortConverter.unsign(fm.readShort()));
            int facePoint3 = (ShortConverter.unsign(fm.readShort()));

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
          // RENDER THE MESH
          //

          // we have a full mesh for a single object (including all parts adjusted) - add it to the model
          if (faces != null && points != null && normals != null && texCoords != null) {
            // Create the Mesh
            TriangleMesh triangleMesh = new TriangleMesh();
            triangleMesh.getTexCoords().addAll(texCoords);

            triangleMesh.getPoints().addAll(points);
            triangleMesh.getFaces().addAll(faces);
            //triangleMesh.getNormals().addAll(normals);

            faces = null;
            //points = null;
            //normals = null;
            //texCoords = null;

            // Create the MeshView
            MeshView view = new MeshView(triangleMesh);
            meshView[p] = view;

            rendered = true;
          }

        }

        if (!rendered) {
          // didn't find any meshes
          return null;
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

        // clean up memory
        points = null;
        normals = null;
        texCoords = null;

        // return the preview based on a MeshView
        PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);
        return preview;
      }
      else if (header == 1) {
        // 
        // MODEL TYPE 2
        //

        // 4 - Number of Points
        int numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Number of Unknown Ints
        int numUnknownInts = fm.readInt();
        FieldValidator.checkRange(numUnknownInts, 0, 10000); // 10000 guess

        // 4 - Number of Tex Coords?
        int numUnknown = fm.readInt();
        FieldValidator.checkNumVertices(numUnknown + 1); // allow no tex co-ords

        // 4 - Number of Parts
        int numParts = fm.readInt();
        FieldValidator.checkRange(numParts, 1, 200); // 200 guess

        meshView = new MeshView[numParts];

        // 4 - null
        fm.skip(4);

        // 4 - Has Unknowns Flag
        int unknownFlag = fm.readByte();
        byte[] unknownBytes = fm.readBytes(3);

        if (unknownFlag == 1) {
          // 
          // UNKNOWNS (TYPE 1)
          //

          // 4 - Number of Unknowns
          int numUnknowns = fm.readInt();
          FieldValidator.checkNumFaces(numUnknowns);

          // for each unknown
          for (int u = 0; u < numUnknowns; u++) {
            // 4 - Number of Blocks in this Unknown
            int blocks = fm.readInt();
            FieldValidator.checkRange(blocks, 1, 10);// 10 guess

            // 4 - Unknown Float
            // 4 - Unknown Float
            // 4 - Unknown Float
            // 4 - Unknown Float
            // 4 - Unknown Float
            // 4 - Unknown Float
            fm.skip(24);

            for (int b = 0; b < blocks; b++) {
              // 2 - Unknown (1)
              fm.skip(2);

              // 2 - Number of First Entries in this Block
              int blockCount = fm.readShort();

              // for each First Entry in this Block
              //   2 - Unknown ID
              fm.skip(blockCount * 2);

            }
          }
        }
        else if (unknownBytes[0] == 32 && ByteConverter.unsign(unknownBytes[1]) == 222 && ByteConverter.unsign(unknownBytes[2]) == 192) {
          // 
          // UNKNOWNS (TYPE 2)
          //

          // 2 - null
          fm.skip(2);

          // 2 - Unknown Count 1
          int unknownCount1 = fm.readShort();

          // for each unknown count 1
          for (int c = 0; c < unknownCount1; c++) {
            // 2 - Unknown (2)
            fm.skip(2);

            // 2 - Unknown Count 2
            int unknownCount2 = fm.readShort();

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - null

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - null

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - null

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown (6)
            fm.skip(68);

            // for each unknown count 2
            //  4 - Unknown
            fm.skip(unknownCount2 * 4);
          }

        }

        //
        // VERTICES
        //

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];
        normals = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          // Skip the texture mapping for now
          float xTexture = 0;
          float yTexture = 0;

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

        // UNKNOWN INTS
        fm.skip(numUnknownInts * 4);

        // TEXTURE CO-ORDS?
        fm.skip(numUnknown * 8);

        //
        // PARTS
        //
        int[] faceCount = new int[numParts];
        for (int p = 0; p < numParts; p++) {
          // 4 - Number of Faces in this Part
          int numFaces = fm.readInt();
          FieldValidator.checkNumFaces(numFaces + 1); // allow zeros 
          faceCount[p] = numFaces;

          // 4 - Part Number (incremental from 0)
          // 8 - null
          fm.skip(12);
        }

        for (int p = 0; p < numParts; p++) {

          //
          // FACES
          //

          int numFaces = faceCount[p];

          int faceDataSize = 2;

          int numFaces3 = numFaces * 3;
          FieldValidator.checkNumFaces(numFaces3 + 1); // allow zeros

          //numFaces = numFaces3 / 3;
          int numFaces6 = numFaces3 * 2;

          faces = new int[numFaces6]; // need to store front and back faces

          for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
            // 2 - Point Index 1
            // 2 - Point Index 2
            // 2 - Point Index 3
            int facePoint1 = (ShortConverter.unsign(fm.readShort()));
            int facePoint2 = (ShortConverter.unsign(fm.readShort()));
            int facePoint3 = (ShortConverter.unsign(fm.readShort()));

            // reverse face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint3;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint1;

            // forward face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint3;

            // 2 - Unknown Index 1
            // 2 - Unknown Index 2
            // 2 - Unknown Index 3

            // 8 - null
            fm.skip(14);
          }

          //
          // RENDER THE MESH
          //

          // we have a full mesh for a single object (including all parts adjusted) - add it to the model
          if (faces != null && points != null && normals != null && texCoords != null) {
            // Create the Mesh
            TriangleMesh triangleMesh = new TriangleMesh();
            triangleMesh.getTexCoords().addAll(texCoords);

            triangleMesh.getPoints().addAll(points);
            triangleMesh.getFaces().addAll(faces);
            //triangleMesh.getNormals().addAll(normals);

            faces = null;
            //points = null;
            //normals = null;
            //texCoords = null;

            // Create the MeshView
            MeshView view = new MeshView(triangleMesh);
            meshView[p] = view;

            rendered = true;
          }

        }

        if (!rendered) {
          // didn't find any meshes
          return null;
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

        // clean up memory
        points = null;
        normals = null;
        texCoords = null;

        // return the preview based on a MeshView
        PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);
        return preview;
      }
      else {
        // unknown type
        return null;
      }

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