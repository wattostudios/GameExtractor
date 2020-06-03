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

import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class WSNoWrapViewFactory implements ViewFactory {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSNoWrapViewFactory() {
    super();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public View create(Element elem) {
    String kind = elem.getName();
    if (kind != null) {
      if (kind.equals(AbstractDocument.ContentElementName)) {
        return new LabelView(elem);
      }
      else if (kind.equals(AbstractDocument.ParagraphElementName)) {
        return new WSNoWrapParagraphView(elem);
      }
      else if (kind.equals(AbstractDocument.SectionElementName)) {
        return new BoxView(elem, View.Y_AXIS);
      }
      else if (kind.equals(StyleConstants.ComponentElementName)) {
        return new ComponentView(elem);
      }
      else if (kind.equals(StyleConstants.IconElementName)) {
        return new IconView(elem);
      }
    }

    // default to text display
    return new LabelView(elem);
  }

}
