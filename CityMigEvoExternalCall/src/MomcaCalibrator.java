import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class MomcaCalibrator
{
	private LinkedList<Float> incWeights;
	private LinkedList<Float> famWeights;
	private LinkedList<Float> regWeights;
	private LinkedList<Float> evaluations;
	private String BASE_DIR = "../CityMigEvolution";
	private DecimalFormat df;
	private Random rand;

	public MomcaCalibrator()
	{
		incWeights = new LinkedList<Float>();
		famWeights = new LinkedList<Float>();
		regWeights = new LinkedList<Float>();
		evaluations = new LinkedList<Float>();
		df = new DecimalFormat("0.0");
		rand = new Random();
	}

	public void Caliration()
	{
		// init:
		for (int inc_index = 0; inc_index <= 3; inc_index++)
		{
			for (int fam_index = 0; fam_index <= 3; fam_index++)
			{
				for (int reg_index = 0; reg_index <= 3; reg_index++)
				{
					float incWei = (float) (0.1 + inc_index * 0.2);
					float famWei = (float) (0.1 + fam_index * 0.2);
					float regWei = (float) (0.1 + reg_index * 0.2);
					if (incWei + famWei + regWei > 0.9)
						continue;
					float totalError = ModelRun(incWei, famWei, regWei);
					if (totalError > 0)
					{
						incWeights.add(incWei);
						famWeights.add(famWei);
						regWeights.add(regWei);
						evaluations.add(totalError);
					} else
					{
						System.out.println("ERROR!!! totalError = " + totalError);
					}
				}
			}
		}
		// evolution:
		int maxIterNum = 100;
		for (int i = 0; i < maxIterNum; i++)
		{
			LinkedList<Float> newIncWeights = new LinkedList<Float>();
			LinkedList<Float> newFamWeights = new LinkedList<Float>();
			LinkedList<Float> newRegWeights = new LinkedList<Float>();
			LinkedList<Float> newEvaluations = new LinkedList<Float>();
			// niching:
			int bestKNum = 4;
			LinkedList<Integer> subsripts = new LinkedList<Integer>();
			for (int j = 0; j < bestKNum; j++)
			{
				float minError = Float.MAX_VALUE;
				int minIndex = -1;
				for (int k = 0; k < evaluations.size(); k++)
				{
					if (minError > evaluations.get(k))
					{
						minError = evaluations.get(k);
						minIndex = k;
					}
				}
				newIncWeights.add(incWeights.get(minIndex));
				newFamWeights.add(famWeights.get(minIndex));
				newRegWeights.add(regWeights.get(minIndex));
				newEvaluations.add(evaluations.get(minIndex));
				incWeights.remove(minIndex);
				famWeights.remove(minIndex);
				regWeights.remove(minIndex);
				evaluations.remove(minIndex);
				subsripts.add(j);
			}
			for (int j = 0; j < bestKNum; j++)
			{
				float maxError = Float.MIN_VALUE;
				int maxIndex = -1;
				for (int k = 0; k < evaluations.size(); k++)
				{
					if (maxError < evaluations.get(k))
					{
						maxError = evaluations.get(k);
						maxIndex = k;
					}
				}
				newIncWeights.add(incWeights.get(maxIndex));
				newFamWeights.add(famWeights.get(maxIndex));
				newRegWeights.add(regWeights.get(maxIndex));
				newEvaluations.add(evaluations.get(maxIndex));
				incWeights.remove(maxIndex);
				famWeights.remove(maxIndex);
				regWeights.remove(maxIndex);
				evaluations.remove(maxIndex);
				subsripts.add(j + bestKNum);
			}
			incWeights.clear();
			famWeights.clear();
			regWeights.clear();
			evaluations.clear();
			Collections.shuffle(subsripts);
			for (int j = 0; j < subsripts.size(); j++)
			{
				incWeights.add(newIncWeights.get(subsripts.get(j)));
				famWeights.add(newFamWeights.get(subsripts.get(j)));
				regWeights.add(newRegWeights.get(subsripts.get(j)));
				evaluations.add(newEvaluations.get(subsripts.get(j)));
			}
			// crossover:
			for (int j = 0; j < subsripts.size(); j++)
			{
				float dev = Math.abs(newIncWeights.get(j) - incWeights.get(subsripts.get(j)));
				float incWei = (float) (dev < 0.0001 ? incWeights.get(subsripts.get(j))
								: Math.min(newIncWeights.get(j), incWeights.get(subsripts.get(j))) + rand.nextInt((int) (Math.ceil(dev / 0.1))) * 0.1);
				dev = Math.abs(newFamWeights.get(j) - famWeights.get(subsripts.get(j)));
				float famWei = (float) (dev < 0.0001 ? famWeights.get(subsripts.get(j))
								: Math.min(newFamWeights.get(j), famWeights.get(subsripts.get(j))) + rand.nextInt((int) (Math.ceil(dev / 0.1))) * 0.1);
				dev = Math.abs(newRegWeights.get(j) - regWeights.get(subsripts.get(j)));
				float regWei = (float) (dev < 0.0001 ? regWeights.get(subsripts.get(j))
								: Math.min(newRegWeights.get(j), regWeights.get(subsripts.get(j))) + rand.nextInt((int) (Math.ceil(dev / 0.1))) * 0.1);
				float totalError = ModelRun(incWei, famWei, regWei);
				if (totalError > 0)
				{
					incWeights.add(incWei);
					famWeights.add(famWei);
					regWeights.add(regWei);
					evaluations.add(totalError);
				} else
				{
					System.out.println("ERROR!!! totalError = " + totalError);
				}
			}
		}
		float minError = Float.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < evaluations.size(); i++)
		{
			if (minError > evaluations.get(i))
			{
				minError = evaluations.get(i);
				minIndex = i;
			}
		}
		System.out.println("Final Calibrated Para : " + df.format(incWeights.get(minIndex)) + "-" + df.format(famWeights.get(minIndex)) + "-"
						+ df.format(regWeights.get(minIndex)) + "-" + new DecimalFormat("0.0000").format(evaluations.get(minIndex)));
	}

	private float ModelRun(float incWei, float famWei, float regWei)
	{
		WritePara(incWei, famWei, regWei);
		ResultEval eval = new ResultEval();
		String command = "java -jar C:\\Users\\RepastSimphony-2.6\\batch_runner.jar -hl -r -c D:\\project\\CityMigEvolution\\batch\\batch_configuration.properties";
		Runtime runtime = Runtime.getRuntime();
		String line = null;
		int secondsLast = -1;
		float totalError = -1;
		try
		{
			Process process = runtime.exec(command);
			final InputStream is = process.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			try
			{
				while ((line = br.readLine()) != null)
				{
					if (line != null)
					{
						System.out.println(line);
						if (line.contains("RunningTime"))
						{
							String timeStr = line.substring(line.indexOf("RunningTime"));
							secondsLast = Integer.parseInt(timeStr.split(" ")[1].trim());
						}
					}
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			} finally
			{
				try
				{
					is.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			process.waitFor();
			process.destroy();
			totalError = eval.Evaluation(10000);
			System.out.println("total error = " + totalError);
			FileOutputStream outStream = new FileOutputStream("../CityMigEvolution/ParaAndRunTime.txt", true);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
			String writeLine = df.format(incWei) + "-" + df.format(famWei) + "-" + df.format(regWei) + "-" + eval.df.format(totalError) + "-" + secondsLast;
			out.write(writeLine + "\n");
			out.close();
			outStream.close();
			System.out.println("Running Time: " + secondsLast + " s");
		} catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		return totalError;
	}

	private void WritePara(float incWei, float famWei, float regWei)
	{
		Document document = null;
		try
		{
			SAXReader saxReader = new SAXReader();
			document = saxReader.read(new File(BASE_DIR + "/batch/batch_params.xml"));
			Element root = document.getRootElement();
			Iterator iterator = root.elementIterator();
			while (iterator.hasNext())
			{
				Element para = (Element) iterator.next();
				String attr_name = para.attribute("name").getText();
				if (attr_name.contains("IncWei"))
				{
					para.attribute("value").setText(df.format(incWei).toString());
				}
				if (attr_name.contains("FamWei"))
				{
					para.attribute("value").setValue(df.format(famWei).toString());
				}
				if (attr_name.contains("RegWei"))
				{
					para.attribute("value").setValue(df.format(regWei).toString());
				}
			}
			FileOutputStream output = new FileOutputStream(new File(BASE_DIR + "/batch/batch_params.xml"));
			XMLWriter writer = new XMLWriter(output);
			writer.write(document);
			writer.flush();
			writer.close();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
