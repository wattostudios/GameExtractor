
package com.github.jpeg2000;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import jj2000.j2k.codestream.writer.FileCodestreamWriter;
import jj2000.j2k.codestream.writer.HeaderEncoder;
import jj2000.j2k.entropy.encoder.EntropyCoder;
import jj2000.j2k.entropy.encoder.PostCompRateAllocator;
import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.forwcomptransf.ForwCompTransf;
import jj2000.j2k.io.AbstractDataSource;
import jj2000.j2k.quantization.quantizer.Quantizer;
import jj2000.j2k.roi.encoder.ROIScaler;
import jj2000.j2k.util.FacilityManager;
import jj2000.j2k.util.MsgLogger;
import jj2000.j2k.wavelet.analysis.ForwardWT;

/**
 * A class to create a J2KFile. J2K compressed data may be
 * created from any source, although for convenience a
 * method is supplied to create from a BufferedImage. Compression
 * requires all the image data all the time, so compression from
 * a stream is not possible.
 * 
 * @author http://bfo.com
 */
public class J2KWriter implements MsgLogger {

  private ColorSpecificationBox colr;

  private PaletteBox pclr;

  private ResolutionBox resc, resd;

  private J2KWriteParam param;

  private float ratio;

  private boolean reversible;

  private BlkImgDataSrc src;

  /**
   * Create a new J2KWriter
   */
  public J2KWriter() {
  }

  /**
   * Create and return a {@link J2KFile} which has the compressed image data
   */
  public J2KFile create() throws IOException {
    return doCreate(null);
  }

  private J2KFile doCreate(OutputStream out) throws IOException {
    if (src == null) {
      throw new IllegalStateException("No source");
    }
    if (param == null) {
      param = new SimpleJ2KWriteParam(src.getNumComps(), src.getNumTiles());
      ((SimpleJ2KWriteParam) param).setProgressionName("res");
    }
    if (ratio != 0 && param instanceof SimpleJ2KWriteParam) {
      ((SimpleJ2KWriteParam) param).setCompression(ratio, reversible);
    }
    if (param.getNumComponents() != src.getNumComps() || param.getNumTiles() != src.getNumTiles()) {
      throw new IllegalStateException("Param and source do not match");
    }

    Thread registerThread = Thread.currentThread();

    J2KFile file = new J2KFile();
    HeaderBox jp2h = new HeaderBox();
    int bpc = src.getNomRangeBits(0);
    int totbpc = bpc;
    for (int i = 1; i < src.getNumComps(); i++) {
      totbpc += src.getNomRangeBits(i);
      if (src.getNomRangeBits(i) != bpc) {
        bpc = 255;
      }
    }
    jp2h.add(new ImageHeaderBox(src.getImgWidth(), src.getImgHeight(), src.getNumComps(), bpc, false, false));
    if (colr != null) {
      jp2h.add(colr);
    }
    if (pclr != null) {
      jp2h.add(pclr);
    }
    if (resc != null || resd != null) {
      ResolutionSuperBox res = new ResolutionSuperBox();
      if (resc != null) {
        res.add(resc);
      }
      if (resd != null) {
        res.add(resd);
      }
      jp2h.add(res);
    }
    if (bpc == 255) {
      byte[] b = new byte[src.getNumComps()];
      for (int i = 0; i < b.length; i++) {
        b[i] = (byte) src.getNomRangeBits(i);
      }
      jp2h.add(new BitsPerComponentBox(b));
    }
    file.add(new FileTypeBox());
    file.add(jp2h);

    OutputStream bout;
    if (out == null) {
      bout = new ByteArrayOutputStream();
    }
    else {
      file.add(new CodeStreamBox());
      file.write(out);
      bout = out;
    }

    try {
      FacilityManager.registerMsgLogger(registerThread, this);
      ForwCompTransf fctransf = new ForwCompTransf(src, param);
      ImgDataConverter converter = new ImgDataConverter(fctransf);
      ForwardWT dwt = ForwardWT.createInstance(converter, param);
      Quantizer quant = Quantizer.createInstance(dwt, param);
      ROIScaler rois = ROIScaler.createInstance(quant, param);
      EntropyCoder ecoder = EntropyCoder.createInstance(rois, param, param.getCodeBlockSize(), param.getPrecinctPartition(), param.getBypass(), param.getResetMQ(), param.getTerminateOnByte(), param.getCausalCXInfo(), param.getCodeSegSymbol(), param.getMethodForMQLengthCalc(), param.getMethodForMQTermination());

      FileCodestreamWriter bwriter = new FileCodestreamWriter(bout, Integer.MAX_VALUE);
      ratio = param.getCompressionRatio();
      float rate = ratio == 1 ? Float.POSITIVE_INFINITY : totbpc / ratio;
      PostCompRateAllocator ralloc = PostCompRateAllocator.createInstance(ecoder, rate, bwriter, param);
      HeaderEncoder headenc = new HeaderEncoder(src, new boolean[src.getNumComps()], dwt, src, param, rois, ralloc);
      ralloc.setHeaderEncoder(headenc);
      headenc.encodeMainHeader();
      ralloc.initialize();
      headenc.reset();
      headenc.encodeMainHeader();
      bwriter.commitBitstreamHeader(headenc);
      ralloc.runAndWrite();
      bwriter.close();

      if (out == null) {
        file.add(new CodeStreamBox(((ByteArrayOutputStream) bout).toByteArray()));
      }
      return file;
    }
    finally {
      FacilityManager.unregisterMsgLogger(registerThread);
    }
  }

  @Override
  public void flush() {
  }

  @Override
  public void println(String str, int flind, int ind) {
  }

  @Override
  public void printmsg(int sev, String msg) {
  }

  /**
   * Set the "capture" resolution that is written out.
   * @param horiz the horizontal resolution in dots-per-meter, or 0 for no box
   * @param vertical the horizontal resolution in dots-per-meter, or 0 for no box
   */
  public void setCaptureResolution(double horiz, double vertical) {
    resc = horiz > 0 && vertical > 0 ? new ResolutionBox(Box.fromString("resc"), (float) horiz, (float) vertical) : null;
  }

  /**
   * Set the ColorSpace that is written out. This is required if
   * a BufferedImage wasn't used as a source
   */
  public void setColorSpace(ColorSpace space) {
    colr = new ColorSpecificationBox(space);
    pclr = null;
  }

  /**
   * Set the ColorSpace that is written out, and use a palette
   * @param space the underlying ColorSpace
   * @param size the number of entries in the palette
   * @param numc the number of components in the palette index
   * @param palette the palette entries, which are accessed as <code>palette[entry * numc + component]</code>
   */
  public void setColorSpace(ColorSpace space, int size, int numc, byte[] palette) {
    colr = new ColorSpecificationBox(space);
    pclr = new PaletteBox(size, numc, palette);
  }

  /**
   * Set the compresison ratio.
   * @see SimpleJ2KWriteParam#setCompressionRatio
   */
  public void setCompressionRatio(float ratio, boolean reversible) {
    this.ratio = ratio;
    this.reversible = reversible;
  }

  /**
   * Set the "diisplay" resolution that is written out.
   * @param horiz the horizontal resolution in dots-per-meter, or 0 for no box
   * @param vertical the horizontal resolution in dots-per-meter, or 0 for no box
   */
  public void setDisplayResolution(double horiz, double vertical) {
    resd = horiz > 0 && vertical > 0 ? new ResolutionBox(Box.fromString("resd"), (float) horiz, (float) vertical) : null;
  }

  /**
   * Set the write parameters. Usually this isn't required, but
   * it may be called for fine control of the encoding
   */
  public void setParams(J2KWriteParam param) {
    this.param = param;
  }

  /**
   * Set the Source for the image. Any BlkImgDataSrc may be used, but
   * a {@link AbstractDataSource} would be the easiest
   */
  public void setSource(BlkImgDataSrc src) {
    this.src = src;
  }

  /**
   * Set the Source for the image to the BufferedImage.
   * @param img the image
   * @param tilesize the tilesize, 256 is recommended
   */
  public void setSource(BufferedImage img, int tilesize) {
    setSource(AbstractDataSource.newInstance(img, tilesize));
    ColorModel cm = img.getColorModel();
    if (cm instanceof IndexColorModel) {
      throw new UnsupportedOperationException("No indexed yet");
      // TODO
    }
    else {
      setColorSpace(cm.getColorSpace());
    }
  }

  /**
   * Create and write a {@link J2KFile} to the specified OutputStream. This
   * method does not need any intermediate buffers and can write the compressed
   * image straight to the OutputStream, unlike {@link #create} which has to
   * store the image in memory.
   */
  public void write(OutputStream out) throws IOException {
    doCreate(out);
  }
}
