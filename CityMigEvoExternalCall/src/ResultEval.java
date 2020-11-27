import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class ResultEval
{
	private String BASE_DIR = "../CityMigEvolution";
	private String ERROR_FILE = "ParaAndRunTime.txt";

	public Hashtable<Integer, Float> incomeWeights;
	public Hashtable<Integer, Float> famWeights;
	public Hashtable<Integer, Float> registWeights;

	private Hashtable<Integer, Hashtable<Integer, Integer>> statPopNum;
	public DecimalFormat df;

	public ResultEval()
	{
		statPopNum = new Hashtable<Integer, Hashtable<Integer, Integer>>();
		df = new DecimalFormat("0.0000");
		// for Surrogate Calibration:
		incomeWeights = new Hashtable<Integer, Float>();
		famWeights = new Hashtable<Integer, Float>();
		registWeights = new Hashtable<Integer, Float>();
		Document document = null;
		try
		{
			SAXReader saxReader = new SAXReader();
			document = saxReader.read(new File(BASE_DIR + "/CityMigEvolution.rs/parameters.xml"));
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		Element root = document.getRootElement();
		Iterator iterator = root.elementIterator();
		while (iterator.hasNext())
		{
			Element para = (Element) iterator.next();
			String attr_name = para.attribute("name").getText();
			if (attr_name.contains("IncWei"))
			{
				String year_str = attr_name.substring(attr_name.indexOf("_") + 1);
				int year = Integer.parseInt(year_str);
				String default_value = para.attribute("defaultValue").getText();
				float incWei = Float.parseFloat(default_value);
				incomeWeights.put(year, incWei);
			}
			if (attr_name.contains("FamWei"))
			{
				String year_str = attr_name.substring(attr_name.indexOf("_") + 1);
				int year = Integer.parseInt(year_str);
				String default_value = para.attribute("defaultValue").getText();
				float famWei = Float.parseFloat(default_value);
				famWeights.put(year, famWei);
			}
			if (attr_name.contains("RegWei"))
			{
				String year_str = attr_name.substring(attr_name.indexOf("_") + 1);
				int year = Integer.parseInt(year_str);
				String default_value = para.attribute("defaultValue").getText();
				float regWei = Float.parseFloat(default_value);
				registWeights.put(year, regWei);
			}
		}
		try
		{
			InitStatPopNum(BASE_DIR + "/data/BasicData/AnnualStatPopNum.txt");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void InitStatPopNum(String statPopNumFile) throws IOException
	{
		File statPopFile = new File(statPopNumFile);
		InputStreamReader reader = new InputStreamReader(new FileInputStream(statPopFile));
		BufferedReader br = new BufferedReader(reader);
		String line = "";
		line = br.readLine();
		while ((line = br.readLine()) != null)
		{
			String[] items = line.split(",");
			int year = Integer.parseInt(items[0]);
			int cityCode = Integer.parseInt(items[1]);
			int popNum = Integer.parseInt(items[2]);
			if (statPopNum.containsKey(year))
			{
				statPopNum.get(year).put(cityCode, popNum);
			} else
			{
				Hashtable<Integer, Integer> annualPop = new Hashtable<Integer, Integer>();
				annualPop.put(cityCode, popNum);
				statPopNum.put(year, annualPop);
			}
		}
		br.close();
		reader.close();
	}

	public float Evaluation(int scaleFactor) throws IOException
	{
		// get the latest CityPop*.txt file
		ArrayList<String> listPath = getFileList("../CityMigEvolution/output");
		HashMap<Object, String> map = new HashMap<Object, String>();
		for (String path : listPath)
		{
			File f = new File(path);
			if (path.contains("batch_param_map"))
				continue;
			map.put(f.lastModified(), path);
		}
		Object[] obj = map.keySet().toArray();
		Arrays.sort(obj);
		String latestFile = map.get(obj[obj.length - 1]);
		// read synthetic pop num
		Hashtable<Integer, Hashtable<Integer, Integer>> synPopNum = new Hashtable<Integer, Hashtable<Integer, Integer>>();
		File synPopFile = new File(latestFile);
		InputStreamReader reader = new InputStreamReader(new FileInputStream(synPopFile));
		BufferedReader br = new BufferedReader(reader);
		String line = "";
		line = br.readLine();
		String[] headItems = line.split(",");
		int tick_index = -1;
		int cityCode_index = -1;
		int popNum_index = -1;
		for (int i = 0; i < headItems.length; i++)
		{
			if (headItems[i].replaceAll("\"", "").contains("tick"))
				tick_index = i;
			if (headItems[i].replaceAll("\"", "").contains("CityCode"))
				cityCode_index = i;
			if (headItems[i].replaceAll("\"", "").contains("PopNum"))
				popNum_index = i;
		}
		while ((line = br.readLine()) != null)
		{
			String[] items = line.split(",");
			int year = (int) (Float.parseFloat(items[tick_index]) + 2000);
			int cityCode = Integer.parseInt(items[cityCode_index]);
			int popNum = Integer.parseInt(items[popNum_index]);
			if (synPopNum.containsKey(year))
			{
				synPopNum.get(year).put(cityCode, popNum);
			} else
			{
				Hashtable<Integer, Integer> annualPop = new Hashtable<Integer, Integer>();
				annualPop.put(cityCode, popNum);
				synPopNum.put(year, annualPop);
			}
		}
		br.close();
		reader.close();
		// compute error
		float error = (float) 0.0;
		int errorPointNum = 0;
		for (Integer year : statPopNum.keySet())
		{
			if (!synPopNum.containsKey(year))
				continue;
			float annualError = (float) 0.0;
			int annualPointNum = 0;
			Hashtable<Integer, Integer> annualPop = statPopNum.get(year);
			for (Integer cityCode : annualPop.keySet())
			{
				int actPop = annualPop.get(cityCode);
				if (!synPopNum.get(year).containsKey(cityCode))
					continue;
				int synPop = synPopNum.get(year).get(cityCode) * scaleFactor;
				if (actPop > 0)
				{
					annualError += ((float) Math.abs(actPop - synPop)) / actPop;
					annualPointNum++;
				} else
				{
					System.out.println("actPop = " + actPop);
				}
			}
			System.out.println(year + "annual error = " + annualError / annualPointNum);
			error += annualError;
			errorPointNum += annualPointNum;
		}
		if (errorPointNum > 0)
			return error / errorPointNum;
		else
			return -1;
	}

	public static ArrayList<String> getFileList(String dirPath)
	{
		File dir = new File(dirPath);
		File[] fileList = dir.listFiles();
		ArrayList<String> strList = new ArrayList<String>();
		for (File f : fileList)
		{
			if ((f.isFile()) && (".txt".equals(f.getName().substring(f.getName().lastIndexOf("."), f.getName().length()))) && (f.getName().contains("CityPop")))
			{
				strList.add(f.getAbsolutePath());
			}
		}
		return strList;
	}
}
