package Global;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class CodeDictionary
{
	private static CodeDictionary codeDic;
	public static HashMap<Integer, LinkedList<Integer>> minorPrincipalProv;
	public static HashMap<Integer, String> cityCodeName;
	public static HashMap<Integer, Integer> cityProvMapping;
	public static HashMap<String, Float> dist2ProvCapital;
	public static HashMap<Integer, HashMap<Integer, Float>> cityAnnualIncomeLv;
	public static HashMap<Integer, HashMap<Integer, Float>> annualBirthRate;
	public static HashMap<Integer, HashMap<Integer, Float>> annualDeathRate;
	public static HashMap<Integer, Float> annualMarryRate;
	public static HashMap<Integer, Float> annualDivorceRate;
	public static HashMap<String, HashMap<Integer, Float>> annualCityMigRate;

	private CodeDictionary()
	{
		minorPrincipalProv = new HashMap<Integer, LinkedList<Integer>>();
		InitMinorProv();
		cityCodeName = new HashMap<Integer, String>();
		InitCityCode();
		cityProvMapping = new HashMap<Integer, Integer>();
		InitCityProvMapping();
		dist2ProvCapital = new HashMap<String, Float>();
		InitCityDist2ProvCapital();
		cityAnnualIncomeLv = new HashMap<Integer, HashMap<Integer, Float>>();
		InitCityAnnualLevel();
		annualBirthRate = new HashMap<Integer, HashMap<Integer, Float>>();
		InitBirthRate();
		annualDeathRate = new HashMap<Integer, HashMap<Integer, Float>>();
		InitDeathRate();
		annualMarryRate = new HashMap<Integer, Float>() {
			{
				put(2001, (float) 0.00630);
				put(2002, (float) 0.00610);
				put(2003, (float) 0.00630);
				put(2004, (float) 0.00665);
				put(2005, (float) 0.00630);
				put(2006, (float) 0.00719);
				put(2007, (float) 0.00750);
				put(2008, (float) 0.00827);
				put(2009, (float) 0.0091);
				put(2010, (float) 0.0093);
				put(2011, (float) 0.0097);
				put(2012, (float) 0.0098);
				put(2013, (float) 0.0099);
				put(2014, (float) 0.0096);
				put(2015, (float) 0.009);
				put(2016, (float) 0.0083);
				put(2017, (float) 0.0077);
			}
		};
		annualDivorceRate = new HashMap<Integer, Float>() {
			{
				put(2001, (float) 0.00098);
				put(2002, (float) 0.00090);
				put(2003, (float) 0.00105);
				put(2004, (float) 0.00128);
				put(2005, (float) 0.00137);
				put(2006, (float) 0.00146);
				put(2007, (float) 0.00159);
				put(2008, (float) 0.00171);
				put(2009, (float) 0.00185);
				put(2010, (float) 0.002);
				put(2011, (float) 0.00213);
				put(2012, (float) 0.0023);
				put(2013, (float) 0.0026);
				put(2014, (float) 0.0027);
				put(2015, (float) 0.0028);
				put(2016, (float) 0.003);
				put(2017, (float) 0.0032);
			}
		};
		annualCityMigRate = new HashMap<String, HashMap<Integer, Float>>();
		InitCityMigRate();
	}

	public static synchronized CodeDictionary getCodeDic()
	{
		if (codeDic == null)
		{
			codeDic = new CodeDictionary();
		}
		return codeDic;
	}

	private void InitMinorProv()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.MINOR_GROUP_PROV_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				int minorRaceCode = Integer.parseInt(line.substring(0, line.indexOf(":")));
				String[] provStr = line.substring(line.indexOf("[") + 1, line.indexOf("]")).split(",");
				LinkedList<Integer> provCodeList = new LinkedList<Integer>();
				for (int i = 0; i < provStr.length; i++)
				{
					int provCode = Integer.parseInt(provStr[i]);
					provCodeList.add(provCode);
				}
				minorPrincipalProv.put(minorRaceCode, provCodeList);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitCityCode()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_CODE_NAME_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String cityCodeStr = line.substring(0, line.indexOf(":"));
				int cityCode = 0;
				if (GlobalAttr.isNumeric(cityCodeStr))
				{
					cityCode = Integer.parseInt(cityCodeStr);
				} else
				{
					System.out.println(cityCodeStr);
				}
				String cityName = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
				cityCodeName.put(cityCode, cityName);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitCityProvMapping()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_PROV_MAPPING_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] cityProvStr = line.split(",");
				for (int i = 0; i < cityProvStr.length; i++)
				{
					int cityCode = Integer.parseInt(cityProvStr[i].substring(0, cityProvStr[i].indexOf(":")));
					int provCode = Integer.parseInt(cityProvStr[i].substring(cityProvStr[i].lastIndexOf(":") + 1));
					cityProvMapping.put(cityCode, provCode);
				}
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitCityDist2ProvCapital()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_PROV_CAP_DIST_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] cityDistStr = line.split(",\\{");
				for (int i = 0; i < cityDistStr.length; i++)
				{
					String cityCapStr = null;
					if (cityDistStr[i].contains("{"))
					{
						cityCapStr = cityDistStr[i].substring(cityDistStr[i].indexOf("{") + 1, cityDistStr[i].indexOf("}"));
					} else
					{
						cityCapStr = cityDistStr[i].substring(0, cityDistStr[i].indexOf("}"));
					}
					cityCapStr = cityCapStr.replace(',', '-');
					String distStr = cityDistStr[i].substring(cityDistStr[i].lastIndexOf(":") + 1);
					float distance = -1;
					if (distStr.contains(","))
					{
						distance = Float.parseFloat(distStr.substring(0, distStr.lastIndexOf(',')));
					} else
					{
						distance = Float.parseFloat(distStr);
					}
					dist2ProvCapital.put(cityCapStr, distance);
				}
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitCityAnnualLevel()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_INCOME_LEVEL_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String cityCodeStr = line.substring(0, line.indexOf(":"));
				int cityCode = Integer.parseInt(cityCodeStr);
				String[] annualIncomeStr = line.substring(line.indexOf("[") + 1, line.indexOf("]")).split(",");
				HashMap<Integer, Float> annualIncome = new HashMap<Integer, Float>();
				for (int i = 0; i < annualIncomeStr.length; i++)
				{
					int year = Integer.parseInt(annualIncomeStr[i].substring(0, annualIncomeStr[i].indexOf(":")));
					float income = Float.parseFloat(annualIncomeStr[i].substring(annualIncomeStr[i].lastIndexOf(":") + 1));
					annualIncome.put(year, income);
				}
				cityAnnualIncomeLv.put(cityCode, annualIncome);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitBirthRate()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_BIRTH_RATE_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String cityCodeStr = line.substring(0, line.indexOf(":"));
				int cityCode = Integer.parseInt(cityCodeStr);
				String[] annualBirthStr = line.substring(line.indexOf("[") + 1, line.indexOf("]")).split(",");
				HashMap<Integer, Float> annualBirth = new HashMap<Integer, Float>();
				for (int i = 0; i < annualBirthStr.length; i++)
				{
					int year = Integer.parseInt(annualBirthStr[i].substring(0, annualBirthStr[i].indexOf(":")));
					float income = Float.parseFloat(annualBirthStr[i].substring(annualBirthStr[i].lastIndexOf(":") + 1));
					annualBirth.put(year, income);
				}
				annualBirthRate.put(cityCode, annualBirth);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitDeathRate()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_DEATH_RATE_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String cityCodeStr = line.substring(0, line.indexOf(":"));
				int cityCode = Integer.parseInt(cityCodeStr);
				String[] annualDeathStr = line.substring(line.indexOf("[") + 1, line.indexOf("]")).split(",");
				HashMap<Integer, Float> annualDeath = new HashMap<Integer, Float>();
				for (int i = 0; i < annualDeathStr.length; i++)
				{
					int year = Integer.parseInt(annualDeathStr[i].substring(0, annualDeathStr[i].indexOf(":")));
					float income = Float.parseFloat(annualDeathStr[i].substring(annualDeathStr[i].lastIndexOf(":") + 1));
					annualDeath.put(year, income);
				}
				annualDeathRate.put(cityCode, annualDeath);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitCityMigRate()
	{
		FileReader reader = null;
		BufferedReader br = null;
		try
		{
			reader = new FileReader(GlobalAttr.CITY_MIGRATION_RATE_FILE);
			br = new BufferedReader(reader);
			String line;
			while ((line = br.readLine()) != null)
			{
				String cityCodeStr = line.substring(line.indexOf("{") + 1, line.indexOf("}"));
				String[] cityCodes = cityCodeStr.split(",");
				int origCityCode = Integer.parseInt(cityCodes[0]);
				int destCityCode = Integer.parseInt(cityCodes[1]);
				String migRateStr = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
				String[] migRates = migRateStr.split(",");
				HashMap<Integer, Float> annualMigRate = new HashMap<Integer, Float>();
				for (int i = 0; i < migRates.length; i++)
				{
					int year = Integer.parseInt(migRates[i].substring(0, migRates[i].indexOf(":")));
					float migRate = Float.parseFloat(migRates[i].substring(migRates[i].lastIndexOf(":") + 1));
					annualMigRate.put(year, migRate);
				}
				annualCityMigRate.put(origCityCode + "-" + destCityCode, annualMigRate);
			}
			if (reader != null)
				reader.close();
			if (br != null)
				br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
