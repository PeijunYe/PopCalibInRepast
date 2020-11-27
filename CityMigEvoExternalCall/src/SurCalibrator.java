import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class SurCalibrator
{
	private LinkedList<Float> incWeights;
	private LinkedList<Float> famWeights;
	private LinkedList<Float> regWeights;
	private LinkedList<Float> evaluations;
	private String BASE_DIR = "../CityMigEvolution";
	private DecimalFormat df;
	private ArrayList<LinkedList<Float>> decisionRules;

	public SurCalibrator()
	{
		incWeights = new LinkedList<Float>();
		famWeights = new LinkedList<Float>();
		regWeights = new LinkedList<Float>();
		evaluations = new LinkedList<Float>();
		df = new DecimalFormat("0.0");
		decisionRules = new ArrayList<LinkedList<Float>>();
	}

	public void Caliration(String calType)
	{
		// sampling
		ResultEval eval = null;
		for (int inc_index = 0; inc_index <= 6; inc_index++)
		{
			for (int fam_index = 0; fam_index <= 6; fam_index++)
			{
				for (int reg_index = 0; reg_index <= 6; reg_index++)
				{
					float incWei = (float) (0.1 + inc_index * 0.1);
					float famWei = (float) (0.1 + fam_index * 0.1);
					float regWei = (float) (0.1 + reg_index * 0.1);
					if (incWei + famWei + regWei > 0.9)
						continue;
					WritePara(incWei, famWei, regWei);
					eval = new ResultEval();
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
						FileOutputStream outStream = new FileOutputStream("../CityMigEvolution/ParaAndRunTime_" + calType + ".txt", true);
						BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
						String writeLine = df.format(incWei) + "-" + df.format(famWei) + "-" + df.format(regWei) + "-" + eval.df.format(totalError) + "-"
										+ secondsLast;
						out.write(writeLine + "\n");
						out.close();
						outStream.close();
						System.out.println("Running Time: " + secondsLast + " s");
					} catch (IOException | InterruptedException e)
					{
						e.printStackTrace();
					}
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
		// leaning
		FitDecisionTree();
		// return calibrated para
		float minError = Float.MAX_VALUE;
		int minIndex = -1;
		for (int i = 0; i < decisionRules.size(); i++)
		{
			if (minError < decisionRules.get(i).get(3))
			{
				minError = decisionRules.get(i).get(3);
				minIndex = i;
			}
		}
		System.out.println("Final Calibrated Para : " + df.format(decisionRules.get(minIndex).get(0)) + "-" + df.format(decisionRules.get(minIndex).get(1))
						+ "-" + df.format(decisionRules.get(minIndex).get(2)));
	}

	private void FitDecisionTree()
	{
		ArrayList<LinkedList<Float>> sampleSet = new ArrayList<LinkedList<Float>>();
		for (int i = 0; i < evaluations.size(); i++)
		{
			LinkedList<Float> sample = new LinkedList<Float>();
			sample.add(incWeights.get(i));
			sample.add(famWeights.get(i));
			sample.add(regWeights.get(i));
			sample.add(evaluations.get(i));
			sampleSet.add(sample);
		}
		LinkedList<String> attrList = new LinkedList<String>();
		attrList.add("incWeights");
		attrList.add("famWeights");
		attrList.add("regWeights");
		while (!attrList.isEmpty())
		{
			HashMap<Integer, Float> attrEntropy = new HashMap<Integer, Float>();
			for (int i = 0; i < attrList.size(); i++)
			{
				float entropy = 0;
				for (int j = 0; j < 7; j++)
				{
					float value = (float) (0.1 + 0.1 * j);
					entropy += ComputEntropy(sampleSet, attrList.get(i), value);
				}
				attrEntropy.put(i, entropy);
			}
			int[] labelFreq = new int[5];
			int totalSamNum = 0;
			for (int i = 0; i < sampleSet.size(); i++)
			{
				float fitness = sampleSet.get(i).get(3);
				if (fitness < 0.2)
					labelFreq[0]++;
				else if (fitness < 0.4)
					labelFreq[1]++;
				else if (fitness < 0.6)
					labelFreq[2]++;
				else if (fitness < 0.8)
					labelFreq[3]++;
				else
					labelFreq[4]++;
				totalSamNum++;
			}
			float initEntropy = 0;
			for (int i = 0; i < labelFreq.length; i++)
			{
				initEntropy += (((float) labelFreq[i]) / totalSamNum) * (Math.log((float) labelFreq[i] / totalSamNum) - Math.log(2));
			}

			HashMap<Integer, Float> attrGain = new HashMap<Integer, Float>();
			for (int attrIndex : attrEntropy.keySet())
			{
				attrGain.put(attrIndex, initEntropy - attrEntropy.get(attrIndex));
			}
			int attrIndex = CheckMaxMapValue(attrGain);
			if (attrList.get(attrIndex).contains("incWeights"))
				ExtendDecRules(sampleSet, 0);
			else if (attrList.get(attrIndex).contains("famWeights"))
				ExtendDecRules(sampleSet, 1);
			else if (attrList.get(attrIndex).contains("regWeights"))
				ExtendDecRules(sampleSet, 2);
			attrList.remove(attrIndex);
		}
		// voting
		for (Iterator<LinkedList<Float>> iterator = decisionRules.iterator(); iterator.hasNext();)
		{
			LinkedList<Float> rule = iterator.next();
			if (Math.abs(rule.get(3) + 1) > 0.001)
				continue;
			int[] labelFreq = new int[5];
			for (int i = 0; i < sampleSet.size(); i++)
			{
				float fitness = sampleSet.get(i).get(3);
				if (fitness < 0.2)
					labelFreq[0]++;
				else if (fitness < 0.4)
					labelFreq[1]++;
				else if (fitness < 0.6)
					labelFreq[2]++;
				else if (fitness < 0.8)
					labelFreq[3]++;
				else
					labelFreq[4]++;
			}
			int maxFreq = -1;
			int maxIndex = -1;
			for (int i = 0; i < labelFreq.length; i++)
			{
				if (maxFreq < labelFreq[i])
				{
					maxFreq = labelFreq[i];
					maxIndex = i;
				}
			}
			rule.set(3, (float) maxIndex);
		}
	}

	private void ExtendDecRules(ArrayList<LinkedList<Float>> sampleSet, int attrIndex)
	{
		if (decisionRules.isEmpty())
		{
			for (int i = 0; i < 7; i++)
			{
				float value = (float) (0.1 + 0.1 * i);
				LinkedList<Float> rule = new LinkedList<Float>();
				for (int j = 0; j < 3; j++)
				{
					if (j == attrIndex)
						rule.add(value);
					else
						rule.add((float) -1);
				}
				boolean labelSame = true;
				int label = -1;
				for (int j = 0; j < sampleSet.size(); j++)
				{
					LinkedList<Float> sample = sampleSet.get(j);
					if (Math.abs(sample.get(attrIndex) - value) > 0.001)
						continue;
					float fitness = sample.get(3);
					if (label == -1)
					{
						if (fitness < 0.2)
							label = 0;
						else if (fitness < 0.4)
							label = 1;
						else if (fitness < 0.6)
							label = 2;
						else if (fitness < 0.8)
							label = 3;
						else
							label = 4;
					} else
					{
						int fit_label = -1;
						if (fitness < 0.2)
							fit_label = 0;
						else if (fitness < 0.4)
							fit_label = 1;
						else if (fitness < 0.6)
							fit_label = 2;
						else if (fitness < 0.8)
							fit_label = 3;
						else
							fit_label = 4;
						if (label != fit_label)
							labelSame = false;
					}
					if (!labelSame)
						break;
				}
				if (labelSame)
				{
					assert label != -1;
					rule.add((float) label);
					for (Iterator<LinkedList<Float>> iterator = sampleSet.iterator(); iterator.hasNext();)
					{
						LinkedList<Float> sample = iterator.next();
						if (Math.abs(sample.get(attrIndex) - value) < 0.001)
							sampleSet.remove(sample);
					}
				} else
				{
					rule.add((float) -1);
				}
				decisionRules.add(rule);
			}
		} else
		{
			for (Iterator<LinkedList<Float>> iterator = decisionRules.iterator(); iterator.hasNext();)
			{
				LinkedList<Float> orig_rule = iterator.next();
				decisionRules.remove(orig_rule);
				for (int i = 0; i < 7; i++)
				{
					float value = (float) (0.1 + 0.1 * i);
					float sum = 0;
					for (int j = 0; j < 3; j++)
						if (orig_rule.get(j) > 0)
							sum += orig_rule.get(j);
					if (sum + value > 1)
						continue;
					LinkedList<Float> rule = new LinkedList<Float>();
					for (int j = 0; j < 3; j++)
						rule.add(orig_rule.get(j));
					rule.set(attrIndex, value);
					boolean labelSame = true;
					int label = -1;
					for (int j = 0; j < sampleSet.size(); j++)
					{
						LinkedList<Float> sample = sampleSet.get(j);
						if (Math.abs(sample.get(attrIndex) - value) > 0.001)
							continue;
						float fitness = sample.get(3);
						if (label == -1)
						{
							if (fitness < 0.2)
								label = 0;
							else if (fitness < 0.4)
								label = 1;
							else if (fitness < 0.6)
								label = 2;
							else if (fitness < 0.8)
								label = 3;
							else
								label = 4;
						} else
						{
							int fit_label = -1;
							if (fitness < 0.2)
								fit_label = 0;
							else if (fitness < 0.4)
								fit_label = 1;
							else if (fitness < 0.6)
								fit_label = 2;
							else if (fitness < 0.8)
								fit_label = 3;
							else
								fit_label = 4;
							if (label != fit_label)
								labelSame = false;
						}
						if (!labelSame)
							break;
					}
					if (labelSame)
					{
						assert label != -1;
						rule.add((float) label);
						for (Iterator<LinkedList<Float>> iterator_1 = sampleSet.iterator(); iterator_1.hasNext();)
						{
							LinkedList<Float> sample = iterator_1.next();
							if (Math.abs(sample.get(attrIndex) - value) < 0.001)
								sampleSet.remove(sample);
						}
					} else
					{
						rule.add((float) -1);
					}
					decisionRules.add(rule);
				}
			}
		}
	}

	private float ComputEntropy(ArrayList<LinkedList<Float>> sampleSubset, String attrName, Float attrValue)
	{
		int[] labelFreq = new int[5];
		int attrIndex = -1;
		if (attrName.contains("incWeights"))
			attrIndex = 0;
		else if (attrName.contains("famWeights"))
			attrIndex = 1;
		else if (attrName.contains("regWeights"))
			attrIndex = 2;
		int totalSamNum = 0;
		for (int i = 0; i < sampleSubset.size(); i++)
		{
			LinkedList<Float> sample = sampleSubset.get(i);
			if (Math.abs(sample.get(attrIndex) - attrValue) > 0.001)
				continue;
			float fitness = sample.get(3);
			if (fitness < 0.2)
				labelFreq[0]++;
			else if (fitness < 0.4)
				labelFreq[1]++;
			else if (fitness < 0.6)
				labelFreq[2]++;
			else if (fitness < 0.8)
				labelFreq[3]++;
			else
				labelFreq[4]++;
			totalSamNum++;
		}
		float entropy = 0;
		for (int i = 0; i < labelFreq.length; i++)
		{
			entropy += -((float) labelFreq[i] / totalSamNum) * (Math.log((float) labelFreq[i] / totalSamNum) - Math.log(2));
		}
		return entropy;
	}

	private int CheckMaxMapValue(HashMap<Integer, Float> attrEntropy)
	{
		int maxIndex = -1;
		float maxValue = -1;
		for (Integer index : attrEntropy.keySet())
		{
			float value = attrEntropy.get(index);
			if (maxValue < value)
			{
				maxValue = value;
				maxIndex = index;
			}
		}
		return maxIndex;
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
