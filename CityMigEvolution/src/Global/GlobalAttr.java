package Global;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;

public class GlobalAttr
{
	private static GlobalAttr global;
	public static final String GIS_SHAPE_FILE = "./data/gis/CityBorder.shp";
	public static final String MINOR_GROUP_PROV_FILE = "./data/BasicData/MinorGroupProv.txt";
	public static final String CITY_CODE_NAME_FILE = "./data/BasicData/CityCodeName.txt";
	public static final String CITY_PROV_MAPPING_FILE = "./data/BasicData/CityProvMapping.txt";
	public static final String CITY_PROV_CAP_DIST_FILE = "./data/BasicData/Distance2ProvCapital.txt";
	public static final String CITY_INCOME_LEVEL_FILE = "./data/BasicData/CityIncomeLv.txt";
	public static final String CITY_BIRTH_RATE_FILE = "./data/BasicData/AnnualBirthRate.txt";
	public static final String CITY_DEATH_RATE_FILE = "./data/BasicData/AnnualDeathRate.txt";
	public static final String CITY_MIGRATION_RATE_FILE = "./data/BasicData/MigrationRate.txt";
	public static final String CITY_STAT_POP_FILE = "./data/BasicData/AnnualStatPopNum.txt";
	public static final String ERROR_FILE = "./ParaAndRunTime.txt";
	public static String POP_FILE_DIR = "./data/DividedBy10000";
	public static String SOC_NET_FILE_DIR = "./data/DividedBy10000/SocialNetwork";
	public static float FRIEND_SUBSTITUTION_RATE = (float) 0.05;
	public static HashMap<Integer, Integer> globalMaxHHID;
	public static HashMap<Integer, Integer> globalMaxIndID;
	public static Random rand;
	public static DecimalFormat df;

	private GlobalAttr( )
	{
		globalMaxHHID = new HashMap<Integer, Integer>( );
		globalMaxIndID = new HashMap<Integer, Integer>( );
		rand = new Random( );
		df = new DecimalFormat("0.0000");
	}

	public static synchronized GlobalAttr getGlobalAttr( )
	{
		if (global == null)
		{
			global = new GlobalAttr( );
		}
		return global;
	}

	public static boolean isNumeric(String str)
	{
		for (int i = str.length( ); --i >= 0;)
		{
			if (!Character.isDigit(str.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}
}
