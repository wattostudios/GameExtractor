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
import org.watto.ge.plugin.ExporterPlugin;

/**
**********************************************************************************************
For use in the FileListModel_Tree, where you need a node with a resource, but the resource
doesn't actually exist. Functions identically to a resource, just has a different class so it
can be identified as a fake.
**********************************************************************************************
**/
public class FakeResource extends Resource {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource() {
    super();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath) {
    super(sourcePath);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, String name) {
    super(sourcePath, name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(String name) {
    super();
    setName(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeResource(String name, long offset, long length) {
    super(name, offset, length);
  }

}