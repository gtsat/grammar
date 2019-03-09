package geokeyword;

import java.math.BigInteger;

public class GeoKeyword {
	private static final long THRES = 40000L;
	private static final float[] lo = {37.873810f,23.617539f};
	private static final float[] hi = {38.083810f,23.827539f};
	//private static final float[] center = {37.983810f,23.727539f};

	private static float[] coordinates (String x,String y) {
		float lat = encode(x).longValue() * (hi[0]-lo[0])/THRES + lo[0];
		float lon = encode(y).longValue() * (hi[1]-lo[1])/THRES + lo[1];
		return new float[]{lat,lon};
	}

	private static BigInteger encode (String word) {
		BigInteger code = BigInteger.ZERO;
		if (word != null && !word.isEmpty()) {
			for (char c : word.toLowerCase().toCharArray()) {
				int b = c - 96;
				code = code.multiply(new BigInteger("27")).add(BigInteger.valueOf(b));
			}
		}
		return code.mod(BigInteger.valueOf(THRES));
	}


	public String coordinates () {
		float[] coordinates = coordinates(getXkeyword(),getYkeyword());
		return "L.latLng("+coordinates[0]+","+coordinates[1]+");";
	}

	private String xkeyword = "";
	private String ykeyword = "";

	public String getXkeyword() {
		return xkeyword;
	}

	public void setXkeyword(String xkeyword) {
		this.xkeyword = xkeyword;
	}

	public String getYkeyword() {
		return ykeyword;
	}

	public void setYkeyword(String ykeyword) {
		this.ykeyword = ykeyword;
	}
}

