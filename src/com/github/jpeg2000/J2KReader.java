
package com.github.jpeg2000;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.DataBlkInt;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.util.FacilityManager;
import jj2000.j2k.util.MsgLogger;
import jj2000.j2k.wavelet.synthesis.InverseWT;

/**
 * <p>
 * An InputStream giving access to the decoded image data. Tiles are decoded on demand, meaning
 * the entire image doesn't have to be decoded in memory. The image is converted to 8-bit, YCbCr
 * images are converted to RGB and component subsampling is removed, but otherwise the image data
 * is unchanged.
 * </p>
 * 
 * @author http://bfo.com
 */
public class J2KReader extends InputStream implements MsgLogger {

  private RandomAccessIO in;

  private Thread registerThread;

  private BlkImgDataSrc src;          // image data source

  private DecoderSpecs decSpec;

  private InverseWT invWT;

  private BitstreamReaderAgent breader;

  private int fulliw, fullih, numtx, numty, iw, ih, scanline, numc, fullscale, scale, ntw;//, nth;

  private int[] depth;

  private int[] channels;

  // variable
  private DataBlkInt db;

  private int pos, ty, length;

  private byte[] buf;

 // private boolean baseline = true;

 // private boolean seenapprox;

  private int[][] palette;

  private ColorSpace cs;

  private int cstype;

  private ResolutionBox resc, resd;

  /**
   * Create a new J2KReader from a raw codestream.
   * @param file the CodeStream to read from
   */
  public J2KReader(CodeStreamBox box) throws IOException {
    init(box.getRandomAccessIO());
  }

  /**
   * Create a new J2KReader from a "jp2" file
   * @param file the J2KFile to read from
   */
  public J2KReader(J2KFile file) throws IOException {
    for (Box box : file.getHeaderBox().getBoxes()) {
      addBox(box);
    }
    init(file.getCodeStreamBox().getRandomAccessIO());
  }

  protected void addBox(Box box) {
    if (box instanceof ImageHeaderBox) {
      ImageHeaderBox b = (ImageHeaderBox) box;
      channels = new int[b.getNumComponents()];
      for (int i = 0; i < channels.length; i++) {
        channels[i] = i;
      }
    }
    else if (box instanceof PaletteBox) {
      PaletteBox b = (PaletteBox) box;
      int indexsize = b.getNumEntries();
      int numc = b.getNumComp();
      palette = new int[indexsize][numc];
      for (int i = 0; i < indexsize; i++) {
        for (int c = 0; c < numc; c++) {
          palette[i][c] = b.getComponentValue(i, c);
        }
      }
    }
    else if (box instanceof ColorSpecificationBox) {
      ColorSpecificationBox b = (ColorSpecificationBox) box;
      int method = b.getMethod();
      if (method == 1) {
        cs = createColorSpace(b.getEnumeratedColorSpace(), null);
      }
      else if (method == 2 || method == 3) {
        cs = createColorSpace(0, b.getICCProfileData());
      }
    }
    else if (box instanceof ChannelDefinitionBox) {
      // ComponentDefinitionBox
      ChannelDefinitionBox b = (ChannelDefinitionBox) box;
      short[] c = b.getChannel();
      short[] a = b.getAssociation();
      for (int i = 0; i < c.length; i++) {
        channels[c[i]] = a[i] - 1;
      }
    }
    else if (box instanceof ResolutionBox) {
      if (Box.toString(box.getType()).equals("resc")) {
        resc = (ResolutionBox) box;
      }
      else if (Box.toString(box.getType()).equals("resd")) {
        resd = (ResolutionBox) box;
      }
    }
  }

  public int available() {
    return buf.length - pos;
  }

  public void close() throws IOException {
    free();
  }

  /** 
   * Convert the enumerated colorspace or ICC profile data to a {@link ColorSpace}.
   * This method could be overriden by subclasses to increase the number
   * of supported ColorSpaces.
   * @param e the enumerated colorspace value, eg 16 for sRGB, or 0 if an ICC profile is specified.
   * @param iccprofile the raw data of the ICC profile of specified, or null if an enumerated colorspace is used.
   * @return the ColorSpace, or null if it is unsupported.
   */
  protected ColorSpace createColorSpace(int e, byte[] iccprofile) {
    if (iccprofile != null) {
      return new ICC_ColorSpace(ICC_Profile.getInstance(iccprofile));
    }
    else {
      switch (e) {
        case 16:
          return ColorSpace.getInstance(ColorSpace.CS_sRGB);
        case 17:
          return ColorSpace.getInstance(ColorSpace.CS_GRAY);
        default:
          cstype = e;
      }
    }
    return null;
  }

  public void flush() {
  }

  private void free() throws IOException {
    if (in != null) {
      FacilityManager.unregisterMsgLogger(registerThread);
      registerThread = null;
      in.close();
      in = null;
      src = null;
      decSpec = null;
      invWT = null;
      breader = null;
      db = null;
    }
  }

  //--------------------------------------------------------------
  // InputStream methods

  /**
   * Return the number of bits for each component - currently this is always 8.
   */
  public int getBitsPerComponent() {
    return 8;
  }

  /**
   * Read the content of this J2KReader and return it as a BufferedImage object
   * The ColorSpace must be supported by Java, which means - without extensions
   * to this API - only RGB, GrayScale and indexed-RGB are supported.
   * The InputStream this obejct represents will be read fully and closed.
   * @throws IOException if an IOException is encountered during read
   * @throws IllegalStateExeption if the ColorSpace specified by this file is unsupported.
   */
  public BufferedImage getBufferedImage() throws IOException {
    int width = getWidth();
    int height = getHeight();
    ColorSpace colorSpace = getColorSpace();
    if (colorSpace == null) {
      throw new IllegalStateException("Can't create image: unsupported ColorSpace type " + cstype);
    }
    int bpc = getBitsPerComponent();
   // int numc = getNumComponents();
    ColorModel colorModel;

    if (isIndexed() && colorSpace.isCS_sRGB()) {
      // IndexColorModel is always sRGB in Java.
      int indexSize = getIndexSize();
      byte[] palette = new byte[3 * indexSize];
      for (int i = 0; i < indexSize; i++) {
        palette[i * 3] = (byte) getIndexComponent(i, 0);
        palette[i * 3 + 1] = (byte) getIndexComponent(i, 1);
        palette[i * 3 + 2] = (byte) getIndexComponent(i, 2);
      }
      colorModel = new IndexColorModel(bpc, indexSize, palette, 0, false);
    }
    else {
      boolean opaque = getNumComponents() == colorSpace.getNumComponents();
      if (opaque) {
        colorModel = new ComponentColorModel(colorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
      }
      else {
        colorModel = new ComponentColorModel(colorSpace, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
      }
    }
    SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
    DataBufferByte buffer = (DataBufferByte) sampleModel.createDataBuffer();
    byte[] buf = buffer.getData();
    int off = 0, l;
    while (off < buf.length && (l = read(buf, off, buf.length - off)) >= 0) {
      off += l;
    }
    close();
    WritableRaster raster = Raster.createWritableRaster(sampleModel, buffer, null);
    return new BufferedImage(colorModel, raster, false, null);
  }

  /**
   * Return the "capture" resolution specified in the file in dots-per-meter,
   * with the horizontal and vertical resolution stored as the X and Y
   * values of the returned point. If no resolution is specified,
   * return null
   */
  public Point2D getCaptureResolution() {
    return resc == null ? null : new Point2D.Double(resc.getHorizontalResolution(), resc.getVerticalResolution());
  }

  /**
   * Return the ColorSpace for the image, which may be null if this
   * implementation has no support for the encoded space (eg. Lab or CMYK)
   */
  public ColorSpace getColorSpace() {
    return cs;
  }

  /**
   * Return the "display" resolution specified in the file in dots-per-meter,
   * with the horizontal and vertical resolution stored as the X and Y
   * values of the returned point. If no resolution is specified,
   * return null
   */
  public Point2D getDisplayResolution() {
    return resd == null ? null : new Point2D.Double(resd.getHorizontalResolution(), resd.getVerticalResolution());
  }

  /**
   * Return the overall height of the image, in pixels
   */
  public int getHeight() {
    return ih;
  }

  /**
   * Return the specified component from the image palette
   * @param color the color, from 0..getIndexSize()
   * @param component the component, from 0..getColorSpace().getNumComponents();
   */
  public int getIndexComponent(int color, int component) {
    return palette[color][component];
  }

  /**
   * Return the number of entries in the index
   */
  public int getIndexSize() {
    return palette != null ? palette.length : -1;
  }

  //--------------------------------------------------------------
  // Image methods

  /**
   * Return the number of components in the image data,
   * which will be 1 for indexed images, otherwise the number
   * of components in the image ColorSpace, plus one if the
   * image has an alpha channel
   */
  public int getNumComponents() {
    return numc;
  }

  /**
   * Return the original bit depth for the specified component from the source image.
   */
  public int getOriginalBitsPerComponent(int comp) {
    return depth[comp];
  }

  /**
   * Return the number of bytes in each scanline of the image
   */
  public int getRowSpan() {
    return getNumComponents() * getWidth();
  }

  /**
   * Return the overwall width of the image, in pixels
   */
  public int getWidth() {
    return iw;
  }

  private void init(RandomAccessIO in) throws IOException {
    this.in = in;
    registerThread = Thread.currentThread();
    FacilityManager.registerMsgLogger(registerThread, this);

    HeaderInfo hi = new HeaderInfo();
    J2KReadParam param = new SimpleJ2KReadParam();
    HeaderDecoder hd = new HeaderDecoder(in, param, hi);
    depth = new int[hd.getNumComps()];
    for (int i = 0; i < depth.length; i++) {
      depth[i] = hd.getOriginalBitDepth(i);
    }
    decSpec = hd.getDecoderSpecs();
    breader = BitstreamReaderAgent.createInstance(in, hd, param, decSpec, false, hi);
    if (isInterrupted()) {
      throw new InterruptedIOException();
    }
    EntropyDecoder entdec = hd.createEntropyDecoder(breader, param);
    if (isInterrupted()) {
      throw new InterruptedIOException();
    }
    ROIDeScaler roids = hd.createROIDeScaler(entdec, param, decSpec);
    if (isInterrupted()) {
      throw new InterruptedIOException();
    }
    Dequantizer deq = hd.createDequantizer(roids, depth, decSpec);
    if (isInterrupted()) {
      throw new InterruptedIOException();
    }
    invWT = InverseWT.createInstance(deq, decSpec);
    if (isInterrupted()) {
      throw new InterruptedIOException();
    }

    fullscale = breader.getImgRes();
    fulliw = breader.getImgWidth(fullscale);
    fullih = breader.getImgHeight(fullscale);
    setTargetSize(fulliw, fullih);
  }

  /**
   * Return true if the image is indexed with a palette
   */
  public boolean isIndexed() {
    return palette != null;
  }

  /**
   * Return true if the reading process should throw an InterruptedIOException.
   * By default this just checks if Thread.isInterrupted, but different Thread
   * interruption mechanisms can be used by overriding this method.
   */
  protected boolean isInterrupted() {
    return Thread.currentThread().isInterrupted();
  }

  private boolean nextRow(boolean skip) throws IOException {
    try {
      rowCallback();
      //        System.out.println("IN: ty="+ty+"/"+numty+" numtx="+numtx+" numc="+numc+" skip="+skip+" pos="+pos+" length="+length);
      if (ty == numty) {
        return false;
      }
      if (!skip) {
        for (int tx = 0; tx < numtx; tx++) {
          src.setTile(tx, ty);
          final int tileix = src.getTileIdx();
          // Determine tile width/height - this is not as simple as
          // calling src.getTileWidth when using less than full res.
          int tw = 0;
          int th = 0;
          for (int iz = 0; iz < numc; iz++) {
            tw = Math.max(tw, src.getTileCompWidth(tileix, iz));
            th = Math.max(th, src.getTileCompHeight(tileix, iz));
          }
          if (db == null) {
            // First pass
            buf = new byte[scanline * th];
            db = new DataBlkInt(0, 0, tw, th);
            ntw = tw;
           // nth = th;
          }
          db.w = tw;
          db.h = th;
          length = scanline * th;
          final int itx = tx * ntw;
          final int ity = 0;
          for (int iz = 0; iz < numc; iz++) {
            int riz = channels == null ? iz : channels[iz];     // output channel, could differ from input channel
            if (riz < 0) {
              // This is the OPACITY channel. Well technically
              // it's something that applies to all channels, but
              // given the limitations of what we can do with that
              // we'll assume opacity, as that's the only example
              // seen to date.
              riz = numc - 1;
            }
            final int depth = src.getNomRangeBits(iz);
            final int mid = 1 << (depth - 1);
            final int csx = src.getCompSubsX(iz);
            final int csy = src.getCompSubsY(iz);
            final int fb = src.getFixedPoint(iz);
            // System.out.println("iwh="+iw+"x"+ih+" txy="+tx+"x"+ty+" of "+numtx+","+numty+" itxy="+src.getTilePartULX()+"x"+src.getTilePartULY()+" tcwh="+tw+"x"+th+" twh="+src.getTileWidth()+"x"+src.getTileHeight()+" ntwh="+src.getNomTileWidth()+"x"+src.getNomTileHeight()+" iz="+iz+"="+riz+" ss="+csx+"x"+csy+" d="+depth+" mid="+mid+" fb="+fb+" sl="+scanline+" buf="+buf.length+" channels="+java.util.Arrays.toString(channels));
            int[] shift = null;
            if (depth < 8) {
              shift = new int[1 << depth];
              for (int i = 0; i < shift.length; i++) {
                shift[i] = Math.round(i * 255f / ((1 << depth) - 1));
              }
            }
            do {
              db = (DataBlkInt) src.getInternCompData(db, iz);
            }
            while (db.progressive);
            // Main loop: retrieve value, scaled to 8 bits and adjust midpoint
            for (int iy = 0; iy < th; iy++) {
              if (isInterrupted()) {
                throw new InterruptedIOException();
              }
              for (int ix = 0; ix < tw; ix++) {
                int val = (db.data[db.offset + iy * tw + ix] >> fb) + mid;
                if (depth == 8) {
                  val = Math.max(0, Math.min(255, val));
                }
                else if (depth > 8) {
                  val = Math.max(0, Math.min(255, val >> (depth - 8)));
                }
                else {
                  val = shift[val < 0 ? 0 : val >= shift.length ? shift.length - 1 : val];
                }
                buf[((ity + (iy * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] = (byte) val;
              }
            }
            if (csx != 1 || csy != 1) {
              // Component is subsampled; use bilinear interpolation to fill the gaps. Quick and dirty,
              // tested with limited test data
              for (int iy = 0; iy < th; iy++) {
                if (isInterrupted()) {
                  throw new InterruptedIOException();
                }
                for (int ix = 0; ix < tw; ix++) {
                  // Values on each of the four corners of our space
                  int v00 = buf[((ity + (iy * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] & 0xFF;
                  int v01 = ix + 1 == tw ? v00 : buf[((ity + (iy * csy)) * scanline) + ((itx + ((ix + 1) * csx)) * numc) + riz] & 0xFF;
                  int v10 = iy + 1 == th ? v00 : buf[((ity + ((iy + 1) * csy)) * scanline) + ((itx + (ix * csx)) * numc) + riz] & 0xFF;
                  int v11 = iy + 1 == th ? (ix + 1 == tw ? v00 : v10) : (ix + 1 == tw ? v10 : buf[((ity + ((iy + 1) * csy)) * scanline) + ((itx + ((ix + 1) * csx)) * numc) + riz] & 0xFF);
                  for (int jy = 0; jy < csy; jy++) {
                    for (int jx = 0; jx < csx; jx++) {
                      if (jx + jy != 0 && ix + jx < tw && iy + jy < th) {
                        // q = interpolated(v00, v01, v10, v11)
                        int q0 = v00 + ((v10 - v00) * jx / (csx - 1));
                        int q1 = v01 + ((v11 - v01) * jx / (csx - 1));
                        int q = q0 + ((q1 - q0) * jy / (csy - 1));
                        buf[((ity + (iy * csy) + jy) * scanline) + ((itx + (ix * csx) + jx) * numc) + riz] = (byte) q;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      ty++;
      if (ty == numty) {
        free();
      }
      pos = 0;
      return true;
    }
    catch (RuntimeException e) {
      throw new IOException("Decode failed at " + pos, e);
    }
  }

  public void println(String str, int flind, int ind) {
    //        System.out.println(str);
  }

  public void printmsg(int sev, String msg) {
    //        System.out.println(msg);
  }

  public int read() throws IOException {
    if (pos == length) {
      if (!nextRow(false)) {
        return -1;
      }
    }
    return buf[pos++] & 0xFF;
  }

  public int read(byte[] out) throws IOException {
    return read(out, 0, out.length);
  }

  public int read(byte[] out, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
    int origlen = len;
    while (len > 0) {
      if (pos == length) {
        if (!nextRow(false)) {
          break;
        }
      }
      int avail = Math.min(len, length - pos);
      System.arraycopy(buf, pos, out, off, avail);
      len -= avail;
      off += avail;
      pos += avail;
    }
    return len == origlen ? -1 : origlen - len;
  }

  /**
   * Called when a row of tiles is loaded. For benchmarking, the default is a no-op
   */
  protected void rowCallback() throws IOException {
  }

  /**
   * Set the target size for the output image. The default size is
   * the full size of the image, but it's possible to access lower
   * resolution versions of the image by calling this method with
   * the desired size. While the file image size may not match
   * exactly, it will be as close as usefully possible.
   * @param targetwidth the desired target width of the image
   * @param targetheight the desired target height of the image
   */
  public void setTargetSize(int targetwidth, int targetheight) {
    // Find the best scale so that final width/height are >= 1 and < 2
    // times the desired width.
    int newscale = fullscale;
    for (int i = fullscale; i >= 1; i--) {
      if (targetwidth > breader.getImgWidth(i) || targetheight > breader.getImgHeight(i)) {
        break;
      }
      newscale = i;
    }
    if (newscale != scale) {
      scale = newscale;
      invWT.setImgResLevel(scale);
      ImgDataConverter converter = new ImgDataConverter(invWT, 0);
      src = new InvCompTransf(converter, decSpec, depth);
      iw = src.getImgWidth();
      ih = src.getImgHeight();
      numtx = src.getNumTiles(null).x;
      numty = src.getNumTiles(null).y;
      numc = src.getNumComps();
      scanline = iw * numc;
    }
  }

  public long skip(long len) throws IOException {
    long origlen = len;
    if (buf == null && len > 0) {
      // Ensure read buffer is initialized before we try and skip
      int v = read();
      if (v < 0) {
        return 0;
      }
      len--;
    }
    while (len > 0) {
      int avail = Math.min((int) len, length - pos);
      len -= avail;
      pos += avail;
      if (pos == length) {
        if (!nextRow(len > buf.length)) {
          break;
        }
      }
    }
    return origlen - len;
  }

  public String toString() {
    return "{JPX: w=" + getWidth() + " h=" + getHeight() + " numc=" + getNumComponents() + " bpc=" + getBitsPerComponent() + (isIndexed() ? " ix" + getIndexSize() : "") + " rs=" + getRowSpan() + " hash=0x" + Integer.toHexString(System.identityHashCode(this)) + "}";
  }

}
