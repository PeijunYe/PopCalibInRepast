package cityMigEvolution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

import Global.GlobalAttr;

public class ResultEval
{
	private MainContext mContext;
	// <year,<cityCode, pop Num>>
	private Hashtable<Integer, Hashtable<Integer, Integer>> statPopNum;
	public DecimalFormat df;

	public ResultEval(MainContext mContext)
	{
		this.mContext = mContext;
		statPopNum = new Hashtable<Integer, Hashtable<Integer, Integer>>();
		df = new DecimalFormat("0.0000");
	}

	public void InitStatPopNum(String statPopNumFile) throws IOException
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
		File outFile = new File(GlobalAttr.ERROR_FILE);
		if (!outFile.exists())
		{
			outFile.createNewFile();
			FileOutputStream outStream = new FileOutputStream(outFile);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
			out.write("Year,IncomeWeights,FamWeights,RegWeights,AnnualError\n");
			out.close();
			outStream.close();
		}
	}

	public float Evaluation(int scaleFactor) throws IOException
	{
		ArrayList<String> listPath = getFileList("./");
		HashMap<Object, String> map = new HashMap<Object, String>();
		for (String path : listPath)
		{
			File f = new File(path);
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
		FileOutputStream outStream = new FileOutputStream(GlobalAttr.ERROR_FILE, true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
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
			float incomeWeight = mContext.incomeWeights.get(year);
			float famWeight = mContext.famWeights.get(year);
			float registWeight = mContext.registWeights.get(year);
			String writeLine = year + "," + df.format(incomeWeight) + "," + df.format(famWeight);
			writeLine += "," + df.format(registWeight) + "," + df.format(annualError / annualPointNum);
			out.write(writeLine + "\n");
			error += annualError;
			errorPointNum += annualPointNum;
		}
		out.close();
		outStream.close();
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
