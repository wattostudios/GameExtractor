
package org.watto.datatype;

/***********************************************************************************************
 * A singleton class that captures changes and can ask the user to save the changes they made,
 * via a <code>WSPopup</code>
 ***********************************************************************************************/
public class PluginPrefix {

  /** The prefix of the plugin **/
  String prefix = "";

  /** The type of plugin defined by the prefix **/
  String type = "";

  /***********************************************************************************************
   *
   ***********************************************************************************************/
  public PluginPrefix(String prefix, String type) {
    this.prefix = prefix;
    this.type = type;
  }

  public String getPrefix() {
    return prefix;
  }

  public String getType() {
    return type;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setType(String type) {
    this.type = type;
  }
}