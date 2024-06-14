
package com.github.jpeg2000;

import jj2000.j2k.IntegerSpec;
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
 * Interface which defines the parameters required to write a JP2 image.
 * Abstracted away from the horror that is J2KImageWriteParamJava
 * 
 * @author http://bfo.com
 */
public interface J2KWriteParam {

  public boolean getAlignROI();

  public StringSpec getBypass();

  public StringSpec getCausalCXInfo();

  public CBlkSizeSpec getCodeBlockSize();

  public StringSpec getCodeSegSymbol();

  public ForwCompTransfSpec getComponentTransformation();

  /**
   * Return the desired compression ratio; a value of
   * 1 implies completely lossless; values of 4-6 are
   * visually lossless, up towards 20 for lossy but
   * acceptable
   */
  public float getCompressionRatio();

  public IntegerSpec getDecompositionLevel();

  public StringSpec getEPH();

  public AnWTFilterSpec getFilters();

  public GuardBitsSpec getGuardBits();

  public String getLayers();

  public boolean getLossless();

  public StringSpec getMethodForMQLengthCalc();

  public StringSpec getMethodForMQTermination();

  public int getNumComponents();

  public int getNumTiles();

  public PrecinctSizeSpec getPrecinctPartition();

  public String getProgressionName();

  public ProgressionSpec getProgressionType();

  public QuantStepSizeSpec getQuantizationStep();

  public QuantTypeSpec getQuantizationType();

  public StringSpec getResetMQ();

  public MaxShiftSpec getROIs();

  public StringSpec getSOP();

  public int getStartLevelROI();

  public StringSpec getTerminateOnByte();

  public void setProgressionType(LayersInfo lyrs, String values);

}
