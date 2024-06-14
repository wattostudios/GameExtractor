
package com.github.jpeg2000;

import jj2000.j2k.IntegerSpec;
import jj2000.j2k.ModuleSpec;
import jj2000.j2k.StringSpec;
import jj2000.j2k.entropy.CBlkSizeSpec;
import jj2000.j2k.entropy.PrecinctSizeSpec;
import jj2000.j2k.entropy.ProgressionSpec;
import jj2000.j2k.entropy.encoder.LayersInfo;
import jj2000.j2k.image.forwcomptransf.ForwCompTransfSpec;
import jj2000.j2k.quantization.GuardBitsSpec;
import jj2000.j2k.quantization.QuantStepSizeSpec;
import jj2000.j2k.quantization.QuantTypeSpec;
import jj2000.j2k.roi.MaxShiftSpec;
import jj2000.j2k.wavelet.analysis.AnWTFilterSpec;

/**
 * A minimal instance of the J2KWriteParam interface.
 *
 * @author http://bfo.com
 */
public class SimpleJ2KWriteParam implements J2KWriteParam {

  private final int numc, numtiles;

  private IntegerSpec decompositionLevel;

  private PrecinctSizeSpec precinctPartition;

  private ProgressionSpec progressionType;

  private GuardBitsSpec guardBits;

  private AnWTFilterSpec filters;

  private ForwCompTransfSpec componentTransformation;

  private QuantStepSizeSpec quantizationStep;

  private QuantTypeSpec quantizationType;

  private CBlkSizeSpec codeBlockSize;

  private StringSpec methodForMQTermination;

  private StringSpec methodForMQLengthCalc;

  private int startLevelROI;

  private float ratio;

  private String layers;

  private String progressionName;

  private boolean alignROI;

  /*
  private boolean sop;

  private boolean eph;

  private boolean bypass;

  private boolean resetMQ;

  private boolean terminateOnByte;

  private boolean causalCXInfo;

  private boolean codeSegSymbol;
*/
  private boolean lossless;

  private MaxShiftSpec rois;

  //private final StringSpec stringtrue, stringfalse;

  /**
   * Create a new SimpleJ2KWriteParam
   * @param numc the number of components
   * @param numtiles the number of tiles
   */
  public SimpleJ2KWriteParam(int numc, int numtiles) {
    this.numc = numc;
    this.numtiles = numtiles;
    //stringtrue = new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, "true");
    //stringfalse = new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, null);

    setLayers("0.015 +20 2.0 +10");
    setProgressionName("layer");
    setDecompositionLevel(5);
    setGuardBits(2);
    setQuantizationStep(Double.NaN);
    setCodeBlockSize(64, 64);
    setFilters(true, true);
    setROIs(-1, false, null);
    setMQ(null, null);
    setCompression(1, true);
  }

  public boolean getAlignROI() {
    return alignROI;
  }

  public StringSpec getBypass() {
    //        return bypass ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, "false");
  }

  public StringSpec getCausalCXInfo() {
    //        return causalCXInfo ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, null);
  }

  public CBlkSizeSpec getCodeBlockSize() {
    return codeBlockSize;
  }

  public StringSpec getCodeSegSymbol() {
    //        return codeSegSymbol ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, null);
  }

  public ForwCompTransfSpec getComponentTransformation() {
    return componentTransformation;
  }

  public float getCompressionRatio() {
    return ratio;
  }

  public IntegerSpec getDecompositionLevel() {
    return decompositionLevel;
  }

  public StringSpec getEPH() {
    //        return eph ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, "false");
  }

  public AnWTFilterSpec getFilters() {
    return filters;
  }

  public GuardBitsSpec getGuardBits() {
    return guardBits;
  }

  public String getLayers() {
    return layers;
  }

  public boolean getLossless() {
    return lossless;
  }

  public StringSpec getMethodForMQLengthCalc() {
    return methodForMQLengthCalc;
  }

  public StringSpec getMethodForMQTermination() {
    return methodForMQTermination;
  }

  public int getNumComponents() {
    return numc;
  }

  public int getNumTiles() {
    return numtiles;
  }

  public PrecinctSizeSpec getPrecinctPartition() {
    return precinctPartition;
  }

  public String getProgressionName() {
    return progressionName;
  }

  public ProgressionSpec getProgressionType() {
    return progressionType;
  }

  public QuantStepSizeSpec getQuantizationStep() {
    return quantizationStep;
  }

  public QuantTypeSpec getQuantizationType() {
    return quantizationType;
  }

  public StringSpec getResetMQ() {
    //        return resetMQ ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, null);
  }

  public MaxShiftSpec getROIs() {
    return rois;
  }

  public StringSpec getSOP() {
    //        return sop ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, "false");
  }

  public int getStartLevelROI() {
    return startLevelROI;
  }

  public StringSpec getTerminateOnByte() {
    //        return terminateOnByte ? stringtrue : stringfalse;
    return new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "false", new String[] { "true", "false" }, null, null);
  }

  public void setCodeBlockSize(int w, int h) {
    codeBlockSize = new CBlkSizeSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, w + " " + h);
  }

  /**
   * Set the desired Compression Ratio. A value of
   * 1 implies lossless, higher ratios involve loss
   * @param ratio the compression ratio
   * @param reversible if true, the reversible filter is used.  The irreversible one tends to give slightly better results but may use more memory
   */
  public void setCompression(float ratio, boolean reversible) {
    this.ratio = Math.max(1, ratio);
    lossless = ratio == 1;
    setFilters(lossless || reversible, true);
  }

  public void setDecompositionLevel(int level) {
    decompositionLevel = new IntegerSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, Integer.toString(level), "5");
    precinctPartition = new PrecinctSizeSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, null, getDecompositionLevel(), this, null);
  }

  public void setFilters(boolean filter53, boolean transform) {
    if (filter53) {
      quantizationType = new QuantTypeSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, "reversible");
      filters = new AnWTFilterSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, getQuantizationType(), this, "w5x3");
    }
    else {
      quantizationType = new QuantTypeSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, "expounded");
      filters = new AnWTFilterSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, getQuantizationType(), this, "w9x7");
    }
    componentTransformation = new ForwCompTransfSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE, getFilters(), this, Boolean.toString(transform));
  }

  public void setGuardBits(int bits) {
    if (bits <= 0) {
      throw new IllegalArgumentException();
    }
    guardBits = new GuardBitsSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, Integer.toString(bits));
  }

  private void setLayers(String layers) {
    this.layers = layers;
  }

  public void setMQ(String lengthcalc, String termination) {
    if (lengthcalc == null) {
      lengthcalc = "near_opt";
    }
    if (termination == null) {
      termination = "near_opt";
    }
    methodForMQLengthCalc = new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "near_opt", new String[] { "near_opt", "lazy_good", "lazy" }, this, lengthcalc);
    methodForMQTermination = new StringSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, "near_opt", new String[] { "near_opt", "easy", "predict", "full" }, this, termination);
  }

  public void setProgressionName(String name) {
    if ("res".equals(name) || "layer".equals(name) || "res-pos".equals(name) || "pos-comp".equals(name) || "comp-pos".equals(name)) {
      progressionName = name;
    }
    else {
      throw new IllegalArgumentException(name);
    }
  }

  public void setProgressionType(LayersInfo lyrs, String values) {
    progressionType = new ProgressionSpec(getNumTiles(), getNumComponents(), lyrs.getTotNumLayers(), getDecompositionLevel(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, values);
  }

  public void setQuantizationStep(double val) {
    quantizationStep = new QuantStepSizeSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, this, val == val ? Double.toString(val) : null);
  }

  public void setROIs(int start, boolean align, String rois) {
    this.startLevelROI = start;
    this.alignROI = align;
    this.rois = new MaxShiftSpec(getNumTiles(), getNumComponents(), ModuleSpec.SPEC_TYPE_TILE_COMP, rois);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ lossless:" + getLossless());
    sb.append(", numc:" + getNumComponents());
    sb.append(", numt:" + getNumTiles());
    sb.append(", layers:'" + getLayers() + "'");
    sb.append(", decomp:" + getDecompositionLevel());
    sb.append(", precinct:" + getPrecinctPartition());
    sb.append(", prog:" + getProgressionType());
    sb.append(", progn:'" + getProgressionName() + "'");
    sb.append(", sop:" + getSOP());
    sb.append(", eph:" + getEPH());
    sb.append(", tran:" + getComponentTransformation());
    sb.append(", cbsize:" + getCodeBlockSize());
    sb.append(", bypass:" + getBypass());
    sb.append(", resetmq:" + getResetMQ());
    sb.append(", termonbyte:" + getTerminateOnByte());
    sb.append(", causalcxinfo:" + getCausalCXInfo());
    sb.append(", mqlengthcalc:" + getMethodForMQLengthCalc());
    sb.append(", mqterm:" + getMethodForMQTermination());
    sb.append(", codeseg:" + getCodeSegSymbol());
    sb.append(", filters:" + getFilters());
    sb.append(", quantstep:" + getQuantizationStep());
    sb.append(", quanttype:" + getQuantizationType());
    sb.append(", guardbits:" + getGuardBits());
    sb.append(", rois:" + getROIs());
    sb.append(", startlevelroi:" + getStartLevelROI());
    sb.append(", alignroi:" + getAlignROI());
    sb.append("}");
    return sb.toString();
  }

}
