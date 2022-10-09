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

import java.net.URL;
import javax.media.protocol.URLDataSource;

/**
**********************************************************************************************
Allows MP3 audio files to be played as previews when they don't have a *.mp3 extension.
Used by Viewer_JavaMediaFramework_Audio.java
**********************************************************************************************
**/
public class MpegAudioDataSource extends URLDataSource {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public MpegAudioDataSource(URL url) throws java.io.IOException {
    super(url);
    connect();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getContentType() {
    if (!connected) {
      return null;
    }
    return "audio/mpeg";
  }

}
