
package ibxm;

public class Envelope {
	public boolean enabled = false, sustain = false, looped = false;
	public int sustainTick = 0, loopStartTick = 0, loopEndTick = 0;
	public int numPoints = 1;
	public int[] pointsTick = new int[ 1 ];
	public int[] pointsAmpl = new int[ 1 ];
	
	public int nextTick( int tick, boolean keyOn ) {
		tick++;
		if( looped && tick >= loopEndTick ) tick = loopStartTick;
		if( sustain && keyOn && tick >= sustainTick ) tick = sustainTick;
		return tick;
	}
	
	public int calculateAmpl( int tick ) {
		int ampl = pointsAmpl[ numPoints - 1 ];
		if( tick < pointsTick[ numPoints - 1 ] ) {
			int point = 0;
			for( int idx = 1; idx < numPoints; idx++ )
				if( pointsTick[ idx ] <= tick ) point = idx;
			ampl = pointsAmpl[ point ];
			int dt = pointsTick[ point + 1 ] - pointsTick[ point ];
			if( dt > 0 ) {
				int da = pointsAmpl[ point + 1 ] - pointsAmpl[ point ];
				ampl += ( ( da << 24 ) / dt ) * ( tick - pointsTick[ point ] ) >> 24;
			}
		}
		return ampl;
	}
	
	public void toStringBuffer( StringBuffer out, String prefix ) {
		if( sustain ) {
			out.append( prefix + "Sustain Tick: " + sustainTick + '\n' );
		}
		if( looped ) {
			out.append( prefix + "Loop Start Tick: " + loopStartTick + '\n' );
			out.append( prefix + "Loop End Tick: " + loopEndTick + '\n' );
		}
		out.append( prefix + "Points:" );
		for( int point = 0; point < numPoints; point++ ) {
			if( point % 3 == 0 ) {
				out.append( '\n' + prefix + prefix );
			}
			out.append( "(" + pointsTick[ point ] + ", " + pointsAmpl[ point ] + "), " );
		}
		out.append( '\n' );
	}
}
