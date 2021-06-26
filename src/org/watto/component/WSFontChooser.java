////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.JComponent;
import org.watto.Language;
import org.watto.Settings;
import org.watto.event.WSEnterableInterface;
import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.event.listener.WSEnterableListener;
import org.watto.event.listener.WSSelectableListener;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * A Font Chooser GUI <code>Component</code>
 ***********************************************************************************************/

public class WSFontChooser extends WSPanel implements WSSelectableInterface, WSEnterableInterface, WSEventableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The <code>WSList</code> that displays the <code>Font</code> names **/
  WSList fontNameChooser;
  /** The <code>WSList</code> that displays the <code>Font</code> sizes **/
  WSList fontSizeChooser;
  /** The <code>WSList</code> that displays the <code>Font</code> styles **/
  WSList fontStyleChooser;

  /** The <code>Font</code> name field **/
  WSTextField fontName;
  /** The <code>Font</code> size field **/
  WSTextField fontSize;
  /** The <code>Font</code> style field **/
  WSTextField fontStyle;

  /** The <code>Font</code> color **/
  WSSmallColorChooser fontColor;

  /** The <code>Font</code> preview **/
  WSLabel fontPreview;

  /** removes the reiteration when loading the interface **/
  boolean building = false;

  /** Whether a keyPress event is being done? **/
  boolean doingKeyPress = false;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSFontChooser() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
   ***********************************************************************************************/
  public WSFontChooser(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
   * Gets the <code>Font</code> for the selected values
   * @return the <code>Font</code>
   ***********************************************************************************************/
  @Override
  public Font getFont() {
    try {
      Font font = Font.decode(fontName.getText());
      float size = Float.parseFloat(fontSize.getText());
      int style = Font.PLAIN;

      String styleChosen = fontStyle.getText();
      if (styleChosen.equals(Language.get("WSFontChooser_FontStyle_Italic"))) {
        style = Font.ITALIC;
      }
      else if (styleChosen.equals(Language.get("WSFontChooser_FontStyle_Bold"))) {
        style = Font.BOLD;
      }
      else {
        style = Font.PLAIN;
      }

      if (styleChosen.equals(Language.get("WSFontChooser_FontStyle_BoldItalic"))) {
        // special - bold-italic needs bold and italic done together
        return font.deriveFont(Font.BOLD + Font.ITALIC).deriveFont(size);
      }

      return font.deriveFont(style).deriveFont(size);
    }
    catch (Throwable t) {
      return null;
    }
  }

  /***********************************************************************************************
   * Sets the global values after the <code>WSComponent</code>s have been constructed from the
   * XML. Also initialises listeners and sets default values.
   ***********************************************************************************************/
  @SuppressWarnings("unchecked")
  public void loadGlobals() {
    // referencing the components
    fontNameChooser = (WSList) ComponentRepository.get("FontNameChooser");
    fontSizeChooser = (WSList) ComponentRepository.get("FontSizeChooser");
    fontStyleChooser = (WSList) ComponentRepository.get("FontStyleChooser");

    fontName = (WSTextField) ComponentRepository.get("FontName");
    fontSize = (WSTextField) ComponentRepository.get("FontSize");
    fontStyle = (WSTextField) ComponentRepository.get("FontStyle");

    fontColor = (WSSmallColorChooser) ComponentRepository.get("SmallColorChooser");

    fontPreview = (WSLabel) ComponentRepository.get("FontPreview");

    // populate the lists
    String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    fontNameChooser.setListData(fontNames);

    String[] fontSizes = new String[] { "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72" };
    fontSizeChooser.setListData(fontSizes);

    String[] fontStyles = new String[] { "Plain", "Bold", "Italic", "BoldItalic" };
    for (int i = 0; i < fontStyles.length; i++) {
      fontStyles[i] = Language.get("WSFontChooser_FontStyle_" + fontStyles[i]);
    }
    fontStyleChooser.setListData(fontStyles);

    // populate the comboboxes
    //String[] fontColors = new String[]{"BLACK","BLUE","CYAN","DARK_GRAY","GRAY","GREEN","LIGHT_GRAY","MAGENTA","ORANGE","PINK","RED","WHITE","YELLOW"};
    //fontColor.setModel(new DefaultComboBoxModel(fontColors));

    // select the correct rows in the lists (based on the Settings)
    setFontName(Settings.get("WSFontChooser_FontName_Selected"));
    setFontSize(Settings.get("WSFontChooser_FontSize_Selected"));
    setFontStyle(Language.get("WSFontChooser_FontStyle_" + Settings.get("WSFontChooser_FontStyle_Selected")));

    // register listeners on the lists
    //fontNameChooser.addListSelectionListener(new WSSelectableListener(this));
    fontSizeChooser.addListSelectionListener(new WSSelectableListener(this));
    fontStyleChooser.addListSelectionListener(new WSSelectableListener(this));

    fontName.addKeyListener(new WSEnterableListener(this));
    fontSize.addKeyListener(new WSEnterableListener(this));
    fontStyle.addKeyListener(new WSEnterableListener(this));

    reloadPreview();
  }

  /***********************************************************************************************
   * Performs an action when a <i>deselect</i> event is triggered
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the event <code>Object</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onDeselect(JComponent source, Object event) {
    return false;
  }

  /***********************************************************************************************
   * Performs an action when the <code>Enter</code> key is pressed from a <code>KeyEvent</code>
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the <code>KeyEvent</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onEnter(JComponent source, KeyEvent event) {
    doingKeyPress = true;

    if (source == fontName) {
      // Font Name List
      setFontName(fontName.getText());
      reloadPreview();
    }
    else if (source == fontSize) {
      // Font Size List
      setFontSize(fontSize.getText());
      reloadPreview();
    }
    else if (source == fontStyle) {
      // Font Style List
      setFontStyle(fontStyle.getText());
      reloadPreview();
    }

    doingKeyPress = false;
    return true;
  }

  /***********************************************************************************************
   * Performs an action when a <code>WSEvent</code> event is triggered
   * @param source the <code>Object</code> that triggered the event
   * @param event the <code>WSEvent</code>
   * @param type the events type ID
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onEvent(Object source, WSEvent event, int type) {
    if (type == WSEvent.COLOR_CHANGED) {
      reloadPreview();
      return true;
    }
    return false;
  }

  /***********************************************************************************************
   * Performs an action when a <i>select</i> event is triggered
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the event <code>Object</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onSelect(JComponent source, Object event) {
    if (source instanceof WSList) {
      if (doingKeyPress) {
        // prevents duplicate reloading of the preview when the user types in a value AND
        // when the value is also in the list.
        return true;
      }

      if (source == fontNameChooser) {
        // Font Name List
        setFontName((String) fontNameChooser.getSelectedValue());
        reloadPreview();
      }
      else if (source == fontSizeChooser) {
        // Font Size List
        Object size = fontSizeChooser.getSelectedValue();
        if (size != null) {
          setFontSize((String) size);
          reloadPreview();
        }
      }
      else if (source == fontStyleChooser) {
        // Font Style List
        setFontStyle((String) fontStyleChooser.getSelectedValue());
        reloadPreview();
      }
      return true;
    }
    return false;
  }

  /***********************************************************************************************
   * Regenerates the preview window after a property of the <code>Font</code> has changed
   ***********************************************************************************************/
  public void reloadPreview() {
    try {
      if (building) {
        return;
      }

      Color color = fontColor.getColor();
      fontPreview.setForeground(color);

      fontPreview.setFont(getFont());
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
   * Sets the name of the <code>Font</code>
   * @param name the <code>Font</code> name
   ***********************************************************************************************/
  public void setFontName(String name) {
    if (building) {
      return;
    }
    building = true;

    int selIndex = fontNameChooser.getSelectedIndex();

    // select it in the list, if it is a listed name
    if (name != null) {
      fontNameChooser.setSelectedValue(name, true);
    }

    if (fontNameChooser.getSelectedIndex() == -1) {
      name = Settings.get("WSFontChooser_FontName_Selected");
      if (name == null || name.equals("")) {
        //fontNameChooser.setSelectedIndex(0);
        name = (String) fontNameChooser.getSelectedValue();
      }
    }

    if (selIndex == fontNameChooser.getSelectedIndex()) {
      name = (String) fontNameChooser.getSelectedValue();
    }

    if (name != null) {
      fontName.setText(name);
      Settings.set("WSFontChooser_FontName_Selected", name);
    }

    building = false;
  }

  /***********************************************************************************************
   * Sets the size of the <code>Font</code>
   * @param size the <code>Font</code> size
   ***********************************************************************************************/
  public void setFontSize(String size) {
    if (building) {
      return;
    }
    building = true;

    try {
      Integer.parseInt(size); // only allow numbers
    }
    catch (Throwable t) {
      size = Settings.get("WSFontChooser_FontSize_Selected");
      if (size == null || size.equals("")) {
        fontSizeChooser.setSelectedIndex(4);
        size = (String) fontSizeChooser.getSelectedValue();
      }
    }

    // select it in the list, if it is a listed number
    fontSizeChooser.setSelectedValue(size, true);

    Object selected = fontSizeChooser.getSelectedValue();
    if (selected != null && !((String) selected).equals(size)) {
      fontSizeChooser.clearSelection();
    }

    fontSize.setText(size);
    Settings.set("WSFontChooser_FontSize_Selected", size);

    building = false;
  }

  /***********************************************************************************************
   * Sets the style of the <code>Font</code>. This has to be handled differently, because
   * BOLDITALIC is implemented oddly, and the language should not be saved.
   * @param style the <code>Font</code> style
   ***********************************************************************************************/
  public void setFontStyle(String style) {
    if (building) {
      return;
    }
    building = true;

    int selIndex = fontStyleChooser.getSelectedIndex();

    // select it in the list, if it is a listed number
    fontStyleChooser.setSelectedValue(style, true);

    if (fontStyleChooser.getSelectedIndex() == -1) {
      style = Settings.get("WSFontChooser_FontStyle_Selected");
      if (style == null || style.equals("")) {
        fontStyleChooser.setSelectedIndex(0);
        style = (String) fontSizeChooser.getSelectedValue();
      }
    }

    if (selIndex == fontStyleChooser.getSelectedIndex()) {
      style = (String) fontStyleChooser.getSelectedValue();
    }

    fontStyle.setText(style);

    // set language-independent names for the setting
    if (style != null) {
      if (style.equals(Language.get("WSFontChooser_FontStyle_Italic"))) {
        style = "Italic";
      }
      else if (style.equals(Language.get("WSFontChooser_FontStyle_Bold"))) {
        style = "Bold";
      }
      else if (style.equals(Language.get("WSFontChooser_FontStyle_BoldItalic"))) {
        style = "BoldItalic";
      }
      else {
        style = "Plain";
      }

      Settings.set("WSFontChooser_FontStyle_Selected", style);
    }

    building = false;
  }

  /***********************************************************************************************
   * Builds this <code>WSComponent</code> from the properties of the <code>node</code>
   * @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to
   *        construct
   ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {

    XMLNode srcNode = XMLReader.read(new File(Settings.getString("WSFontChooserXML")));
    super.toComponent(srcNode);

    setOpaque(true);

    /*
     * // TODO CHECK IF THIS IS NEEDED, AS PER BELOW COMMENT setLayout(new BorderLayout());
     *
     * // Build an XMLNode tree containing all the elements on the screen TO DELETE!!! XMLNode
     * srcNode = XMLReader.read(new File(Settings.getString("WSFontChooserXML")));
     *
     * // Build the components from the XMLNode tree Component component =
     * WSHelper.toComponent(srcNode); add(component,BorderLayout.CENTER);
     *
     * // setting up this object in the repository setCode(((WSComponent)component).getCode());
     * //ComponentRepository.add(this);
     */

    loadGlobals();
  }

}