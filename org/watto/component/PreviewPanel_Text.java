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

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.watto.Settings;
import org.watto.event.WSSelectableInterface;
import org.watto.event.listener.WSSelectableListener;
import org.watto.xml.XMLReader;

public class PreviewPanel_Text extends PreviewPanel implements WSSelectableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  JTextArea preview = null;

  static Font defaultFont = null;

  static Font monoFont = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_Text(String text) {
    super();

    WSOptionCheckBox wordwrapCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_Text_WordWrap\" setting=\"PreviewPanel_Text_WordWrap\" />"));
    WSOptionCheckBox monospacedFontCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_Text_MonospacedFont\" setting=\"PreviewPanel_Text_MonospacedFont\" />"));

    //add a listener to the checkbox, so we can capture and process select/deselect
    WSSelectableListener selectableListener = new WSSelectableListener(this);
    wordwrapCheckbox.addItemListener(selectableListener);
    monospacedFontCheckbox.addItemListener(selectableListener);

    WSPanel topPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" layout=\"GridLayout\" rows=\"1\" columns=\"2\" />"));
    topPanel.add(wordwrapCheckbox);
    topPanel.add(monospacedFontCheckbox);

    add(topPanel, BorderLayout.NORTH);

    preview = new JTextArea(text);

    defaultFont = preview.getFont();
    monoFont = new Font("monospaced", Font.PLAIN, 12);

    if (Settings.getBoolean("PreviewPanel_Text_MonospacedFont")) {
      preview.setFont(monoFont);
    }
    else {
      preview.setFont(defaultFont);
    }

    preview.setEditable(false);
    preview.setLineWrap(Settings.getBoolean("PreviewPanel_Text_WordWrap"));
    preview.setWrapStyleWord(true);

    add(new JScrollPane(preview), BorderLayout.CENTER);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getText() {
    return preview.getText();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection
    preview = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_Text_WordWrap")) {
        // disable wordwrap
        preview.setLineWrap(false);
      }
      else if (code.equals("PreviewPanel_Text_MonospacedFont")) {
        // Set to a normal font
        preview.setFont(defaultFont);
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_Text_WordWrap")) {
        // disable wordwrap
        preview.setLineWrap(true);
      }
      else if (code.equals("PreviewPanel_Text_MonospacedFont")) {
        // Set to a normal font
        preview.setFont(monoFont);
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
  }

}