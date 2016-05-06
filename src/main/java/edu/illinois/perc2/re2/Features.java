package edu.illinois.perc2.re2;

public class Features {
	// Zhou et al. (2005) features
	public static final String WM1    = "wm1:";    // bag-of-words in M1
	public static final String HM1    = "hm1:";	   // head word of M1
	public static final String WM2    = "wm2:";    // bag-of-words in M2
	public static final String HM2    = "hm2:";	   // head word of M2
	public static final String HM12   = "hm12:";   // combination of HM1 and HM2
	public static final String WBNULL = "wbnull:";
	public static final String WBFL   = "wbfl:";
	public static final String WBF    = "wbf:";
	public static final String WBL    = "wbl:";
	public static final String WBO    = "wbo:";
	public static final String BM1F   = "bm1f:";
	public static final String BM1L   = "bm1l:";
	public static final String AM1F   = "am1f:";
	public static final String AM1L   = "am1l:";
	public static final String ET12   = "et12:";
	public static final String ML12   = "ml12:";
	public static final String REL	  = "rel:";
	
	public static final String UNK	  = "UNK";
	
	// enum of features for switch statement convenience
	public enum FeatureEnum {
		WM1, HM1, WM2, HM2, HM12, WBNULL, WBFL, WBF, WBL, WBO, BM1F, BM1L, AM1F, AM1L, ET12, ML12, REL
	}
	
	public static String getFeatureString(FeatureEnum feature) {
		switch (feature) {
		case WM1:
			return WM1;
		case HM1:
			return HM1;
		case WM2:
			return WM2;
		case HM2:
			return HM2;
		case HM12:
			return HM12;
		case WBNULL:
			return WBNULL;
		case WBFL:
			return WBFL;
		case WBF:
			return WBF;
		case WBL:
			return WBL;
		case WBO:
			return WBO;
		case BM1F:
			return BM1F;
		case BM1L:
			return BM1L;
		case AM1F:
			return AM1F;
		case AM1L:
			return AM1L;
		case ET12:
			return ET12;
		case ML12:
			return ML12;
		case REL:
			return REL;
		default:
			System.err.println("Unimplemented feature: "+feature);
			return null;
		}
	}
}
