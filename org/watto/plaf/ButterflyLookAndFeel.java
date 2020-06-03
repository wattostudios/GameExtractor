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

package org.watto.plaf;

import java.awt.Font;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;

/***********************************************************************************************
 * An <code>LookAndFeel</code> for GUI <code>Component</code>s
 ***********************************************************************************************/

public class ButterflyLookAndFeel extends LookAndFeel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The width of <code>Border</code>s for ordinary <code>Component</code>s **/
  int borderWidth = 6;
  /** The width of <code>Border</code>s for <code>Menu</code>s **/
  int menuBorderWidth = 2;
  /** How round the corners of the <code>Border</code>s are **/
  int roundness = 4;

  /***********************************************************************************************
   * Sets the properties of the <code>LookAndFeel</code> such as the <code>Font</code> and
   * <code>Color</code>s
   ***********************************************************************************************/
  public ButterflyLookAndFeel() {
    super("ButterflyLookAndFeel", "org.watto.plaf");
  }

  /***********************************************************************************************
   * Gets the <code>borderWidth</code>
   * @return the <code>borderWidth</code>
   ***********************************************************************************************/
  public int getBorderWidth() {
    return borderWidth;
  }

  /**
   * *********************************************************************************************
   *
   * *********************************************************************************************
   */
  @Override
  public UIDefaults getDefaults() {
    UIDefaults table = new UIDefaults();

    loadClassDefaults(table);
    loadSystemColorDefaults(table);
    loadComponentDefaults(table);

    return table;
  }

  /**
   * *********************************************************************************************
   *
   * *********************************************************************************************
   */
  @Override
  public String getDescription() {
    return "WATTO Studios - Butterfly Theme - http://www.watto.org";
  }

  /***********************************************************************************************
   * Gets the <code>menuBorderWidth</code>
   * @return the <code>menuBorderWidth</code>
   ***********************************************************************************************/
  public int getMenuBorderWidth() {
    return menuBorderWidth;
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>
   ***********************************************************************************************/
  @Override
  public Object getProperty(String property) {
    if (property.equals("BORDER_WIDTH")) {
      return borderWidth;
    }
    else if (property.equals("MENU_BORDER_WIDTH")) {
      return menuBorderWidth;
    }
    else if (property.equals("ROUNDNESS")) {
      return roundness;
    }
    else {
      return super.getProperty(property);
    }
  }

  /***********************************************************************************************
   * Gets the <code>roundness</code>
   * @return the <code>roundness</code>
   ***********************************************************************************************/
  public int getRoundness() {
    return roundness;
  }

  /***********************************************************************************************
   * Initialises the class defaults table
   * @param table the class defaults table
   ***********************************************************************************************/
  protected void loadClassDefaults(UIDefaults table) {
    super.initClassDefaults(table);

    String packageClassName = packageName + ".Butterfly";

    table.put("ButtonUI", packageClassName + "ButtonUI");
    table.put("ScrollBarUI", packageClassName + "ScrollBarUI");
    table.put("ScrollPaneUI", packageClassName + "ScrollPaneUI");
    table.put("LabelUI", packageClassName + "LabelUI");
    table.put("ProgressBarUI", packageClassName + "ProgressBarUI");
    table.put("ToolBarUI", packageClassName + "ToolBarUI");
    table.put("ToolBarSeparatorUI", packageClassName + "ToolBarSeparatorUI");
    table.put("MenuBarUI", packageClassName + "MenuBarUI");
    table.put("MenuUI", packageClassName + "MenuUI");
    table.put("MenuItemUI", packageClassName + "MenuItemUI");
    table.put("PopupMenuUI", packageClassName + "PopupMenuUI");
    table.put("PopupMenuSeparatorUI", packageClassName + "PopupMenuSeparatorUI");
    table.put("PanelUI", packageClassName + "PanelUI");
    table.put("ComboBoxUI", packageClassName + "ComboBoxUI");
    table.put("CheckBoxUI", packageClassName + "CheckBoxUI");
    table.put("ListUI", packageClassName + "ListUI");
    table.put("TextFieldUI", packageClassName + "TextFieldUI");
    table.put("TextAreaUI", packageClassName + "TextAreaUI");
    table.put("SliderUI", packageClassName + "SliderUI");
    table.put("TreeUI", packageClassName + "TreeUI");
    table.put("ToolTipUI", packageClassName + "ToolTipUI");
    table.put("RadioButtonUI", packageClassName + "RadioButtonUI");
    table.put("ToggleButtonUI", packageClassName + "ToggleButtonUI");
    table.put("TableUI", packageClassName + "TableUI");
    table.put("FileChooserUI", packageClassName + "FileChooserUI");
    table.put("SplitPaneUI", packageClassName + "SplitPaneUI");
    table.put("EditorPaneUI", packageClassName + "EditorPaneUI");

    //table.put("ViewportUI",packageClassName + "ViewportUI");
    //table.put("InternalFrameUI",packageClassName + "InternalFrameUI");
    //table.put("RootPaneUI",packageClassName + "RootPaneUI");
    table.put("TabbedPaneUI", packageClassName + "TabbedPaneUI");
    //table.put("PasswordFieldUI",packageClassName + "PasswordFieldUI");
    //table.put("TableHeaderUI",packageClassName + "HeaderUI");
    //table.put("ColorChooserUI",packageClassName + "ColorChooserUI");
    //table.put("FormattedTextFieldUI",packageClassName + "FormattedTextFieldUI");
    //table.put("CheckBoxMenuItemUI",packageClassName + "CheckBoxMenuItemUI");
    //table.put("RadioButtonMenuItemUI",packageClassName + "RadioButtonMenuItemUI");
    //table.put("SeparatorUI",packageClassName + "SeparatorUI");
    //table.put("SpinnerUI",packageClassName + "SpinnerUI");
    //table.put("ToolBarSeparatorUI",packageClassName + "ToolBarSeparatorUI");
    //table.put("TextPaneUI",packageClassName + "TextPaneUI");
    //table.put("EditorPaneUI",packageClassName + "EditorPaneUI");
    //table.put("TableHeaderUI",packageClassName + "TableHeaderUI");
    //table.put("DesktopPaneUI",packageClassName + "DesktopPaneUI");
    //table.put("DesktopIconUI",packageClassName + "DesktopIconUI");
    //table.put("OptionPaneUI",packageClassName + "OptionPaneUI");
  }

  /***********************************************************************************************
   * Initialises the component defaults table
   * @param table the component defaults table
   ***********************************************************************************************/
  protected void loadComponentDefaults(UIDefaults table) {
    super.initComponentDefaults(table);

    EmptyBorder noBorder = new EmptyBorder(0, 0, 0, 0);
    EmptyBorder oneBorder = new EmptyBorder(1, 1, 1, 1);
    EmptyBorder borBorder = new EmptyBorder(borderWidth, borderWidth, borderWidth, borderWidth);
    EmptyBorder menuBorder = new EmptyBorder(menuBorderWidth, menuBorderWidth, menuBorderWidth, menuBorderWidth);

    Font fontBold = font.deriveFont(Font.BOLD);

    table.put("Button.border", borBorder);
    table.put("Button.font", fontBold);
    table.put("CheckBox.font", fontBold);
    table.put("ComboBox.font", fontBold);
    table.put("EditorPane.border", noBorder);

    table.put("FormattedTextField.border", noBorder);
    table.put("FormattedTextField.margin", new Insets(0, 0, 0, 0));

    table.put("Label.font", fontBold);

    table.put("List.background", backgroundColor);
    table.put("List.border", oneBorder);
    table.put("List.focusCellHighlightBorder", oneBorder);
    table.put("List.font", font);
    table.put("List.selectionBackground", midColor);
    table.put("List.selectionForeground", darkColor);

    table.put("Menu.border", new EmptyBorder(menuBorderWidth + 1, menuBorderWidth, menuBorderWidth, menuBorderWidth + 2));
    table.put("Menu.font", fontBold);
    table.put("Menu.menuPopupOffsetX", 0);
    table.put("Menu.menuPopupOffsetY", 3);

    table.put("MenuBar.border", menuBorder);
    table.put("MenuBar.font", fontBold);

    table.put("MenuItem.acceleratorForeground", textColor);
    table.put("MenuItem.acceleratorSelectionForeground", textColor);
    table.put("MenuItem.border", new EmptyBorder(menuBorderWidth + 3, menuBorderWidth + 2, menuBorderWidth + 2, menuBorderWidth));
    table.put("MenuItem.font", fontBold);

    table.put("PopupMenu.border", menuBorder);
    table.put("PopupMenu.font", fontBold);

    table.put("ProgressBar.border", noBorder);
    table.put("ProgressBar.font", fontBold);

    table.put("RadioButton.font", fontBold);

    table.put("ScrollBar.border", noBorder);

    table.put("ScrollPane.viewportBorder", noBorder);
    table.put("ScrollPane.border", new EmptyBorder(menuBorderWidth + 1, menuBorderWidth + 1, menuBorderWidth + 1, menuBorderWidth + 1));

    table.put("Slider.font", fontBold);

    table.put("SplitPane.border", noBorder);

    table.put("TabbedPane.font", font);
    table.put("TabbedPane.tabAreaBackground", backgroundColor);
    table.put("TabbedPane.background", backgroundColor);
    table.put("TabbedPane.foreground", textColor);
    table.put("TabbedPane.light", lightColor);
    table.put("TabbedPane.shadow", darkColor);
    table.put("TabbedPane.darkShadow", darkColor);
    table.put("TabbedPane.focus", midColor);
    table.put("TabbedPane.selected", midColor);
    table.put("TabbedPane.selectHighlight", lightColor);
    table.put("TabbedPane.tabAreaInsets", new Insets(0, 0, 0, 0));
    table.put("TabbedPane.tabInsets", new Insets(1, 3, 1, 3));
    table.put("TabbedPane.tabsOverlapBorder", Boolean.FALSE);
    table.put("TabbedPane.selectedTabPadInsets", new Insets(1, 3, 1, 3));
    table.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
    table.put("TabbedPane.tabRunOverlay", new Integer(0));
    table.put("TabbedPane.tabsOpaque", Boolean.TRUE);
    table.put("TabbedPane.contentOpaque", Boolean.TRUE);

    table.put("Table.focusCellBackground", backgroundColor);
    table.put("Table.focusCellHighlightBorder", noBorder);
    table.put("Table.font", font);
    table.put("Table.gridColor", backgroundColor);

    table.put("TableHeader.background", lightColor);
    table.put("TableHeader.cellBorder", menuBorder);
    table.put("TableHeader.font", fontBold);

    table.put("TextArea.border", noBorder);
    table.put("TextArea.font", font);
    table.put("TextArea.margin", new Insets(0, 0, 0, 0));
    table.put("TextArea.selectionBackground", midColor);
    table.put("TextArea.selectionForeground", textColor);

    table.put("TextField.border", borBorder);
    table.put("TextField.font", font);
    table.put("TextField.margin", new Insets(0, 0, 0, 0));
    table.put("TextField.selectionBackground", midColor);
    table.put("TextField.selectionForeground", textColor);

    table.put("ToggleButton.border", borBorder);
    table.put("ToggleButton.font", fontBold);

    table.put("ToolBar.border", menuBorder);
    table.put("ToolBar.background", midColor);
    table.put("ToolBar.darkShadow", midColor);
    table.put("ToolBar.dockingBackground", midColor);
    table.put("ToolBar.dockingForeground", darkColor);
    table.put("ToolBar.floatingBackground", midColor);
    table.put("ToolBar.floatingForeground", darkColor);
    table.put("ToolBar.font", fontBold);
    table.put("ToolBar.foreground", darkColor);
    table.put("ToolBar.highlight", midColor);
    table.put("ToolBar.light", midColor);
    table.put("ToolBar.shadow", midColor);

    table.put("ToolTip.font", font);

    table.put("Tree.font", font);
    table.put("Tree.hash", darkColor); // legs
    table.put("Tree.line", darkColor); // horizontal lines
    table.put("Tree.selectionBackground", midColor);
    table.put("Tree.selectionBorderColor", darkColor);
    table.put("Tree.selectionForeground", textColor);
    table.put("Tree.textBackground", backgroundColor);
    table.put("Tree.textForeground", textColor);

    // Java 1.5 Tweaks
    table.put("MenuBar.gradient", Arrays.asList(new Object[] { new Float(0f), new Float(0f), new ColorUIResource(backgroundColor), new ColorUIResource(backgroundColor), new ColorUIResource(backgroundColor) }));

    table.put("SplitPane.dividerFocusColor", backgroundColor);
    table.put("SplitPane.background", backgroundColor);
    table.put("SplitPane.centerOneTouchButtons", Boolean.TRUE);

  }

  /***********************************************************************************************
   * Initialises the system colors defaults table
   * @param table the system colors defaults table
   ***********************************************************************************************/
  protected void loadSystemColorDefaults(UIDefaults table) {
    super.initSystemColorDefaults(table);

    ColorUIResource backgroundColorResource = new ColorUIResource(backgroundColor);
    ColorUIResource textColorResource = new ColorUIResource(textColor);
    ColorUIResource midColorResource = new ColorUIResource(midColor);
    ColorUIResource lightColorResource = new ColorUIResource(lightColor);
    ColorUIResource darkColorResource = new ColorUIResource(darkColor);

    table.put("desktop", backgroundColorResource);
    table.put("activeCaptionBorder", midColorResource);
    table.put("inactiveCaption", lightColorResource);
    table.put("inactiveCaptionText", midColorResource);
    table.put("inactiveCaptionBorder", midColorResource);
    table.put("menu", darkColorResource);
    table.put("text", midColorResource);
    table.put("textInactiveText", lightColorResource);
    table.put("control", backgroundColorResource);
    table.put("controlHighlight", midColorResource);
    table.put("controlShadow", lightColorResource);
    table.put("scrollbar", darkColorResource);
    table.put("info", darkColorResource);
    table.put("infoText", lightColorResource);

    table.put("activeCaption", textColorResource);
    table.put("activeCaptionText", backgroundColorResource);
    table.put("window", backgroundColorResource);
    table.put("windowBorder", textColorResource);
    table.put("windowText", textColorResource);
    table.put("menuText", textColorResource);
    table.put("textText", textColorResource);
    table.put("textHighlight", textColorResource);
    table.put("textHighlightText", backgroundColorResource);
    table.put("controlText", textColorResource);
    table.put("controlLtHighlight", backgroundColorResource);
    table.put("controlDkShadow", textColorResource);
  }

  /***********************************************************************************************
   * Sets the <code>borderWidth</code>
   * @param borderWidth the <code>borderWidth</code>
   ***********************************************************************************************/
  public void setBorderWidth(int borderWidth) {
    this.borderWidth = borderWidth;
  }

  /***********************************************************************************************
   * Sets the <code>menuBorderWidth</code>
   * @param the menuBorderWidth <code>menuBorderWidth</code>
   ***********************************************************************************************/
  public void setMenuBorderWidth(int menuBorderWidth) {
    this.menuBorderWidth = menuBorderWidth;
  }

  /***********************************************************************************************
   * Sets the <code>roundness</code>
   * @param roundness the <code>roundness</code>
   ***********************************************************************************************/
  public void setRoundness(int roundness) {
    this.roundness = roundness;
  }

}