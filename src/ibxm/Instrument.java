
package ibxm;

public class Instrument {
	public String name = "";
	public int numSamples = 1;
	public int vibratoType = 0, vibratoSweep = 0, vibratoDepth = 0, vibratoRate = 0;
	public int volumeFadeOut = 0;
	public Envelope volumeEnvelope = new Envelope();
	public Envelope panningEnvelope = new Envelope();
	public int[] keyToSample = new int[ 97 ];
	public Sample[] samples = new Sample[] { new Sample() };

	public void toStringBuffer( StringBuffer out ) {
		out.append( "Name: " + name + '\n' );
		if( numSamples > 0 ) {
			if( vibratoDepth > 0 ) {
				out.append( "Vibrato Type: " + vibratoType + '\n' );
				out.append( "Vibrato Sweep: " + vibratoSweep + '\n' );
				out.append( "Vibrato Depth: " + vibratoDepth + '\n' );
				out.append( "Vibrato Rate: " + vibratoRate + '\n' );
			}
			if( volumeFadeOut > 0 ) {
				out.append( "Volume Fade Out: " + volumeFadeOut + '\n' );
			}
			if( volumeEnvelope.enabled ) {
				out.append( "Volume Envelope:\n" );
				volumeEnvelope.toStringBuffer( out, "   " );
			}
			if( panningEnvelope.enabled ) {
				out.append( "Panning Envelope:\n" );
				panningEnvelope.toStringBuffer( out, "   " );
			}
			out.append( "Num Samples: " + numSamples + '\n' );
			for( int samIdx = 0; samIdx < numSamples; samIdx++ ) {
				out.append( "Sample " + samIdx + ":\n" );
				samples[ samIdx ].toStringBuffer( out, "   " );
			}
			if( numSamples > 1 ) {
				out.append( "Key To Sample:\n" );
				for( int oct = 0; oct < 8; oct++ ) {
					out.append( "   Oct " + oct + ": " );
					for( int key = 0; key < 12; key++ ) {
						out.append( keyToSample[ oct * 12 + key + 1 ] );
						if( key < 11 ) {
							out.append( "," );
						}
					}
					out.append( '\n' );
				}
			}
		}
	}
}
