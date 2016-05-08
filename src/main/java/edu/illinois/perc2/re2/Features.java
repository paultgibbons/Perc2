package edu.illinois.perc2.re2;

public class Features {
	// Zhou et al. (2005) features
	public static final String WM1    = "wm1:";    // bag-of-words in M1
	public static final String HM1    = "hm1:";	   // head word of M1
	public static final String WM2    = "wm2:";    // bag-of-words in M2
	public static final String HM2    = "hm2:";	   // head word of M2
	public static final String HM12   = "hm12:";   // combination of HM1 and HM2
	public static final String WBNULL = "wbnull:"; // when no word in between
	public static final String WBFL   = "wbfl:";   // the only word in between when only one word in between
	public static final String WBF    = "wbf:";    // first word in between when at least two words in between
	public static final String WBL    = "wbl:";	   // last word in between when at least two words in between
	public static final String WBO    = "wbo:";    // other words in between except first and last words when at least three words in between
	public static final String BM1F   = "bm1f:";   // first word before M1
	public static final String BM1L   = "bm1l:";   // second word before M1
	public static final String AM2F   = "am2f:";   // first word after M2
	public static final String AM2L   = "am2l:";   // second word after M2
	public static final String ET12   = "et12:";   // combination of mention entity types (PERSON, ORGANIZATION, FACILITY, LOCATION, and Geo-Political Entity or GPE)
	public static final String ML12   = "ml12:";   // combination of entity levels (NAME, NOMIAL, and PRONOUN)
	public static final String REL	  = "rel:";	   // relation (label)
	
	public static final String UNK	  = "UNK";	   // UNK token
	public static final String NULL   = "NULL";	   // NULL token
	public static final String TRUE   = "TRUE";    // TRUE
	public static final String FALSE  = "FALSE";   // FALSE
	
	// enum of features for switch statement convenience
	public enum FeatureEnum {
		WM1, HM1, WM2, HM2, HM12, WBNULL, WBFL, WBF, WBL, WBO, BM1F, BM1L, AM2F, AM2L, ET12, ML12, REL
	}
	
	/**
	 * Returns the string prefix for a given feature enum value.
	 * @param feature Enum value of feature
	 * @return String prefix for given feature
	 */
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
		case AM2F:
			return AM2F;
		case AM2L:
			return AM2L;
		case ET12:
			return ET12;
		case ML12:
			return ML12;
		case REL:
			return REL;
		default:
			System.err.println("Unimplemented feature: "+feature);
			return "";
		}
	}
}
