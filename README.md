# GAME EXTRACTOR
Extensible Game Archive Editor
http://www.watto.org/extract

## About Game Extractor
Game Extractor is a program that reads and writes files used in games. The primary focus is to
open game archives and allow you to extract the files from them. Secondary to that, there are
also viewers and converters for many different images, including game-specific ones, and a number
of compression algorithms.

## GitHub Information

### What this is...
* This is the source, and complimentary files, for Game Extractor (Basic Version). 
* It is intended that this be used to assist programmers with reading and writing a whole range
  of files used in games, including archive and image formats
* It may also assist people to create their own Game Extractor plugins, which would be compatible
  with both the Basic and Full Versions

### What this is *not*...
* This is only the source for the *Basic Version*. The Full Version code is not included in this
  source repository, as the Full Version is a purchasable product and is not covered under the
  GPLv2 license.
* For most users, there should generally be no need to build these sources - if you want a working
  build of Game Extractor (Basic Version), get it from the website at http://www.watto.org/extract
  
## Installation and Build Prerequisites

Game Extractor requires you to have Java Runtime Environment 8.0 (or 1.8) or later installed on
your computer. This is a free download from http://www.java.com 

If you are using Java version 10 or newer, you will need to install the JavaFX libraries as well,
as they are no longer part of the JRE. These are available from https://openjfx.io/ and will need
to be on your classpath (or in the main Game Extractor directory).

We maintain the project using Eclipse. You should be able to download the repository, and import
it into Eclipse via the menu path File -- Open Projects from File System. The main entry point
is under src/org/watto/ge/GameExtractor.java . When importing into Eclipse, you will need to
choose Java 1.8 under Project --> Java Compiler, as well as choosing JDK 1.8 under Project -->
Java Build Path.

Eclipse will give errors relating to Java Media Framework. If you have it installed, make sure
you add the libraries to the project classpath. Otherwise, just delete the corresponding java
sources if you're not interested in those in particular.

## Operating System Support

Game Extractor is only officially supported on Windows. While Java can theoretically run on many
different operating systems, this doesn't necessarily mean that Game Extractor works correctly
or completely on those operating systems.

We have performed some very basic tests on Linux-based operating systems (does the program open,
and does it read an archive) - that's about it. We have provided some extremely basic scripts to
assist running in ksh/csh/bash shells, however you should evaluate the scripts and adjust them
appropriately for your system.

We would expect that most functions would operate correctly in non-Windows environments, but
offer no guarantees of this, and in some situations we may not be able to make it work in those
environments at all.

## Troubleshooting and Contact Information

If you have a question about the commands and tools in Game Extractor, open up the Help window
within the Game Extractor program. It also contains some tutorials documenting how to perform
specific tasks, such as "How to Open an Archive". These help files can also be opened in any web
browser - start with the file help/index.html

If you have any other Game Extractor questions, browse the Help/FAQ pages on the website -
http://www.watto.org/extract . These pages include help on general Game Extractor queries,
common errors and problems, and information on the full version.

There is a Contact Form on our website, if you need to contact us for any further purpose.

## Version History

* Version 3.14
  * [I] Support for more games, with a focus on adding previews (image and 3D meshes) and thumbnails
  * [+] Added support for encrypted UE4 files when a valid encryption key is supplied
  * [+] When exporting images with multiple frames, and converting them, all the frames are now
        converted and saved as separate images, rather than just the first frame.
  * [+] New Preview Panels to display and edit files as Tables or Trees
  * [A] Exporting meshes with multiple objects in them into OBJ/STL format now works correctly
  * [B] Splitting Unity3D archives where an individual file is over 2GB now works correctly

* Version 3.13
  * [I] Support for more games, with a focus on adding previews (image and 3D meshes) and thumbnails
  * [+] Added previews for Unity3D v20 Meshes that are stored in a separate resS file
  * [+] Added a Mesh Investigator, for previewing unknown files as 3D meshes
  * [+] The ImageInvestigator now does swizzling, and can read color palettes from the image file
  * [+] Added scripts for running Game Extractor from PowerShell and Linux ksh/csh/bash
  * [+] Added some tutorial PDF documents for performing common tasks in Game Extractor
  * [A] New cleaner images for file types, and for some buttons and menu items that were confusing
  * [A] If a plugin forces some file types to display as Text, the thumbnail icon will show as such
  * [A] The command-line argument -list will now write to stdout if -output is omitted
  * [A] Saving a preview where the image has multiple frames, will now save all frames at once
  * [A] When extracting by right-click for the first time, the user is asked to choose a directory
  * [B] Fixed some bugs related to the writing of archives to special Windows directories
  * [B] Fixed bugs where inline-edited text files from PreviewPanel_Text, or files changed by
        external programs (autodetected) would not write into archives properly when saving
  * [B] Minor code changes to address incompatibilities with Linux operating systems

* Version 3.12
  * [I] Heaps of new features and improvements...
    * [I] Support for more games (archive formats, image/audio previews, and thumbnails)
    * [I] Analysis tools for mass-analyzing directories, extracting files, and converting formats
    * [I] Previews for 3D Models in certain games, including some Unity3D and Unreal Engine meshes
    * [I] Improved modification of files within GE, and detection of external modifications
  * [+] A Welcome Wizard is now shown when new users run Game Extractor for the first time
  * [+] The files in the File List can now be filtered using the Search panel
  * [+] Added directory analysis tools for finding game archives in a directory, analyzing the 
        contents of archives, and extracting/converting all the files en masse
  * [+] Created a Preview Panel that can be used by Viewers for rendering 3D Models, including the
        generation of screenshots in standard Image formats, and exporting to other 3D model formats
  * [+] If you extract or preview a file, and then modify the file in another program, you can now
        automatically import the changed files back into Game Extractor
  * [+] Text files can now be edited within Game Extractor, if the Archive supports replacing/writing
  * [+] You can now change color palettes for images that have multiple different palettes
  * [+] Added support for unswizzling of Nintendo Switch images
  * [A] RegEx and Wildcards will now be evaluated by the Search panel
  * [A] Changed the language of the Search panel to Search and Filter
  * [A] Moved some of the more advanced Directory List buttons to a separate hidable panel
  * [A] When extracting files where the name already exists, a number will be prepended to it
  * [A] Pressing the Enter key in the Search/Filter panel will do a filter rather than a search
  * [A] When extracting Unreal Engine 4 archives, related files (uexp, ubulk) are also extracted,
        and SoundWave files are also converted to OGG audio if possible
  * [B] Fixed an issue where decompressed Unreal Engine 1/2 archives would be deleted prematurely
  * [B] Fixed an issue where bulk QuickBMS extracts wouldn't work for filenames with spaces in them

* Version 3.11
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Better support for more Unreal Engine 1/2/3/4 games, including Sound and Texture previews
  * [+] The Information SidePanel now shows any resource-specific properties like Image Width/Height
  * [+] BC7 images can now be shown as previews or thumbnails
  * [B] Fixed a bug where some Unreal Engine 4 archives would always show a progress bar of 99.9%
  * [B] Fixed a bug where some thumbnails would display corrupted, particularly Unreal Engine images
  * [B] Fixed a bug where the Settings and Interface files would occasionally get corrupted on exit
  * [B] Fixed a bug where the ImageInvestigator would show a blank panel on double-click of a file
  * [B] Fixed a bug which prevented some Unreal Engine 1 and 2 Textures from being displayed
  * [B] Fixed several bugs with reading Unreal Engine 4 archives and processing uasset files in them
  * [B] Fixed a bug where files using QuickBMS compression wouldn't be extracted if there were more
        than ~100 files selected, or if you chose the Extract All button.
        
* Version 3.10
  * [I] Heaps of new features and improvements...
    * [I] Support for more games (archive formats, image/audio previews, and thumbnails)
    * [I] Wrappers for QuickBMS scripts, so you can use them to open more archives
    * [I] Included vgmstream for converting many audio formats to WAV files
    * [I] Exporters and previewers for FSB5 Audio, used in Unity games
    * [I] An Image Investigator, for previewing unknown files as images in a variety of formats
    * [I] Interface improvements and additional functions for many of the Side Panels
    * [I] Source code and complimentary files are now pushed to GitHub
  * [+] Image Previews can now show images with multiple frames stored in the same file
  * [+] Added support for opening, previewing, and extracting files using QuickBMS. This allows for
        the use of complex functions like non-Java compression algorithms and in-line code execution.
  * [+] QuickBMS can be downloaded automatically by Game Extractor if it doesn't exist
  * [+] Added an Image Investigator panel, which can display unknown files as images.
  * [+] Option to show the Image Investigator panel for unknown files, rather than the Hex Editor
  * [+] Double-clicking a BMS file will add it to Game Extractor if it looks like a BMS script
  * [+] The Directory Lists now show "special" folders like Documents, Downloads, This PC, etc.
  * [+] Word Wrap and Monospaced Fonts can now be toggled for Text Previews
  * [+] Shrink-to-Fit and Transparency Pattern can now be toggled for Image Previews
  * [+] Visual improvements for the Hex Viewer, and values now show as both little and big endian
  * [+] Added support for FSB3 audio archives, and fixed WAV output headers in FSB audio
  * [A] Changed the web renderer for the Help pages, and re-wrote all the help documents.
  * [A] Thumbnails will now show filenames on 2 lines, which is better suited for longer filenames.
  * [A] Unity3D archives now identify the majority of the file types with proper file extensions
  * [A] Unreal Engine 4 archives now identify uasset files with their proper file extensions, and
        any related "split" files (uexp, ubulk, etc) are no longer shown in the file list
  * [A] The right-click and drag-drop menus now have icons and more options
  * [A] The tables on the Plugin List are now sized appropriately to the header text
  * [B] Fixed a bug where image previews wouldn't save if the filename had funny characters in it
  * [B] Fixed an issue where PNG images wouldn't toggle the background color when clicked
  * [B] Fixed an issue where files wouldn't be extracted to the right place if the extract directory
        didn't exist, such as when manually entered.
  * [B] The Group Table view no longer removes the filter when replacing single files
  * [B] Fixed a bug where files would not be extracted when using the right-click Extract options
  * [B] Bug fixes for some overlapping and miscolored text in the About SidePanel
  
* Version 3.09
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Added preview and thumbnail support for DXT Crunched textures used in the Unity engine
  * [+] Added LZMA and Explode decompression support
  * [+] Added the option to Open Archive when dragging a file into Game Extractor
  * [+] Added (un)swizzle and (un)splitter functions for handling PS2 images and color palettes
  * [A] Unity v17 compressed archives will now be fully decompressed, split, and read as whole archives
  * [A] Importing alpha images and converting to paletted will now retain the alpha values better
  * [B] Fixed a bug where the decompression of Unity v17 archives would sometimes be cut short
  * [B] Fixed a bug where previewing a file would show the original file instead of the replaced
        file, if you did a preview on the file before replacing it.
  * [B] Fixed a bug where archives would become corrupt when trying to overwrite the current archive
  * [B] Fixed an open file pointer leak in Plugin_ZBD.read() and reduced the match rating

* Version 3.08
  * [I] Support for hundreds of additional games, including preview and thumbnail support
  * [+] Archives without stored filenames can be scanned to guess the file types for unknown files

* Version 3.07
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Added support for LZ4-compressed UnityFS asset files
  * [+] Malformed ZIP files can now be read and contents displayed/exported
  * [B] Fixed bugs in the WAVE audio scanner that were truncating the end of the file

* Version 3.06
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Unreal Engine 4 *.uasset Texture2D images can now be viewed (in a PAK archive) when the 
        content is split into separate *.uexp and *.ubulk files
  * [+] Added some support for newer DX10 DDS image formats
  * [+] Added support for more Unity Engine versions, including split archives used on mobile
        devices, and UnityFS file system formats in newer Unity Engine versions
  * [B] Fixed bugs when displaying thumbnails for older Unreal Engine games
  * [B] Fixed bugs where file data wouldn't be saved properly when editing implicit replacable archives
  * [A] The temp directory is now emptied when opening archives
  * [A] DXT5 images are now rendered with alpha channels

* Version 3.05
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Added better support for reading RAR archives, including Solid archives
  * [B] Thumbnails will now generate properly for files that have been exported to disk
  * [B] Fixed a StackOverflow bug when reading the language XML file in the latest Java 8 patch levels

* Version 3.04
  * [I] Support for more games, with a focus on adding previews (image and audio) and thumbnails
  * [+] Paletted TGA images can now be previewed and viewed as thumbnails
  * [B] Fixed bugs where images wouldn't be converted to a different format when extracting
  * [B] Fixed a bug where the image converter list wouldn't have any values in it

* Version 3.03
  * [I] Support for more games, including previews and thumbnails
  * [+] Added support for exporting and previewing some audio files from FSB archives
  * [+] Plugins can now use QuickBMS to perform file decompression for unusual compression formats
  * [+] Preview and thumbnail support for Unreal Engine 3 "Texture2D" images (some games only)
  * [+] Added a LZO decompression Exporter, which is implemented in some Viewer plugins (including UE3)
  * [B] Fixed a bug where the BMP and JPEG thumbnail generators could overrun the length of the data
  * [B] Fixed a bug where the PCX thumbnail generator would only show the first few rows of the image
  * [B] Fixed a bug where files would not be extracted when extracting via the right-click menu

* Version 3.02
  * [I] Support for more games, including previews and thumbnails
  * [+] Thumbnails are now only loaded for the tiles currently in the display - others are loaded
        as you scroll through the list. Added an option to do full thumbnail loads on archive open.
        This makes it much faster to load archives in Thumbnail View, and more seamless.
  * [+] All popups now show as an overlay to the main window, instead of as separate modal popups.
        Makes the interface cleaner, and removes issues with popups hiding behind the main window.
  * [+] Added a command line interface for extracting or listing files in an archive. Run the command
        "java -jar GameExtractor.jar -help" for details, or see the help pages in the program
  * [+] File Lists can now be exported in JSON format
  * [+] Preview and thumbnail support for Unreal Engine 4 "Texture2D" images (some games only)
  * [A] When exporting files, optionally only display standard converters in the conversion list
  * [A] If an image is smaller than the thumbnail, added an option to show it as the actual size
        instead of enlarging it to fill the thumbnail tile.
  * [A] When you click in the current directory field of the DirectoryList, all the text is selected
  * [A] Errors are no longer written to the error log for normal startup, unless DebugMode is enabled
  * [B] Fixed further bugs that prevented image previews from displaying until you move the mouse
  * [B] Fixed a bug where replacing files in an archive wouldn't update the thumbnail image

* Version 3.01
  * [I] Some new features, some improvements, bug fixes, and support for new games
  * [+] When exporting files, you can optionally have Game Extractor try to convert images to a
        different format. For example, all images in an archive can be converted to PNG or JPEG.
  * [+] Added support for several recent Unity3D archive formats, and TEX image viewers
  * [+] Added support for Valve archives and VTF image viewers, in a variety of different pixel formats
  * [+] Added many additional image formats that are readable/writable by Game Imager
  * [+] You can now preview images with animations attached to them
  * [+] Added support for InstallShield CAB and Microsoft Cabinet CAB archives, including finding
        these archives within self-extracting EXE files
  * [A] The DirectoryList now shows the directory contents as a long vertically-scrollable list
  * [A] You can now type a directory name or filename into the DirectoryList and it will change to
        the directory (and load the archive if you entered a filename)
  * [A] Typing a key in a table will now scroll to the next matching file starting with that letter
  * [A] If a preview can't be converted to a different format, the "Save Preview As" options are hidden
  * [A] If you enable the "Show System-Specific Icons" seting, thumbnails for files without previewers 
        will display as their system-specific icons
  * [A] New splash screen images, including a separate one for the Basic Version
  * [A] Clicking the image preview window will now toggle between a black/white background, to allow
        you to better view images that are mostly white or have low alpha values
  * [B] Fixed several bugs where memory would not be released after generating thumbnail images
  * [B] Fixed a bug where opening a preview when the SidePanel_Preview was already open would not
        show the preview until you move the mouse
  * [B] When changing to the Thumbnail File List for the first time, when an archive is opened, the
        "Loading Thumbnails" popup will now be displayed
  * [B] Table Columns will now retain their sizes better, instead of slowly shrinking left over time
  * [B] Fixed a bug where thumbnails wouldn't generate correctly if performing large skip operations
  * [B] Fixed a bug where the interface wasn't loaded from the default on first startup
  * [-] The hover popup for Icon-only buttons has been removed, shows a tooltip in the statusbar instead

* Version 3.0
  * [I] New major release with the following significant changes...
    * [I] Built on Java 8.0, implementation of Java Packages and other code enhancements
    * [I] Development using Eclipse, build and deployment using Apache Ant
    * [I] WSProgram 4.0 now used as the program base
    * [I] New theme ButterflyLookAndFeel implemented by default
    * [I] Using launch4J for building the Windows executable, NSIS 3.0 for the installer
  * [+] Implemented additional image reading/writing formats from Game Imager (TGA, PCX)
  * [+] Implemented a Thumbnail File List, which will use ViewerPlugins to show thumbnails of any
        compatible format for all images in an archive (also set Java Initial Memory to 1GB)
  * [+] Implemented JLayer for playing MP3 audio files (removing a dependency on Java Media Framework)
  * [+] Implemented FFMPEG command line wrapping for converting XWMA audio files to WAV for previewing
  * [+] Reading support for several additional DDS image formats outside of the DXT1/3/5 formats
  * [+] Added a Renamer Plugin for using regex (regular expressions) as the search term
  * [+] Icon-only buttons now show the Name of the button when hovering over them for 2 seconds
  * [B] Statusbar messages for image columns (icon, Added, Renamed, etc) now show real values
  * [B] Clicking a drive letter in the OpenArchive screen now takes you to the drive correctly
  * [B] Fixed several display issues due to Java 8.0, including drawing issues hanging over from 2.x
  * [B] Custom table columns will now be shown if the setting is enabled
  * [A] General interface improvements, such as alternate row shading for tables, and more field labels
  * [A] Improved detection and handling of Unicode and UTF8 Strings for Viewer_TXT previews
  * [A] ArchivePlugins can now force Resources to preview in a specific ViewerPlugin, eg Viewer_TXT
        for text documents where the file extension isn't *.txt

For notes on Versions prior to 3.0, refer to the readme file for Game Extractor Version 2.x

* Legend
  * [+] Added something
  * [-] Removed something
  * [B] Bug fix
  * [A] Alteration
  * [T] Testing
  * [I] General Information
