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

package org.watto.io;

import java.io.IOException;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.DataSource;

/**
**********************************************************************************************
  An InputStream that contains an overwriting offset and length. The InputStream works as
  usual - reading from wherever it is currently positioned in the file, but it will show an
  EOF whenever the fakeLength has been reached, regardless of the EOF of the underlying File.
  Useful for code that read until EOF, however you want it to stop prematurely, such as when
  the file is stored in an archive - so as to stop at the end of a single file rather than the
  end of the whole archive.
**********************************************************************************************
**/

public class FakeDataSource extends DataSource {

  DataSource realSource = null;
  String contentType = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FakeDataSource(DataSource realSource, String contentType) {
    this.realSource = realSource;
    this.contentType = contentType;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void connect() throws IOException {
    realSource.connect();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void disconnect() {
    realSource.disconnect();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getContentType() {
    if (contentType != null) {
      return contentType;
    }
    return realSource.getContentType();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object getControl(String type) {
    return realSource.getControl(type);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object[] getControls() {
    return realSource.getControls();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Time getDuration() {
    return realSource.getDuration();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public MediaLocator getLocator() {
    return realSource.getLocator();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void initCheck() {
    //realSource.initCheck();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setLocator(MediaLocator source) {
    realSource.setLocator(source);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void start() throws IOException {
    realSource.start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void stop() throws IOException {
    realSource.stop();
  }

}