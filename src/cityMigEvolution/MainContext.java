package cityMigEvolution;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

import Agent.Agent;
import Agent.AgentFactory;
import Environment.City;
import Environment.GISFunctions;
import Environment.SpatialIndexManager;
import Environment.Context.AgentContext;
import Environment.Context.CityContext;
import Global.CodeDictionary;
import Global.GlobalAttr;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.PriorityType;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.gis.SimpleAdder;

public class MainContext implements ContextBuilder<Object>
{
	private static Context<Object> mainContext;
	private static Context<City> cityContext;
	public static Geography<City> cityProjection;
	private static Context<Agent> agentContext;
	private int scaleFactor4Display = 1;
	public static int simYear = 2000;
	public long initStartTime;
	public long initEndTime;
	public long simStartTime;
	public long simEndTime;
	public Hashtable<Integer, Float> incomeWeights;
	public Hashtable<Integer, Float> famWeights;
	public Hashtable<Integer, Float> registWeights;
	public ResultEval eval;

	@Override
	public Context build(Context<Object> context)
	{
		initStartTime = System.currentTimeMillis();
		mainContext = context;
		mainContext.setId("MainContext");
		GlobalAttr.getGlobalAttr();
		CodeDictionary.getCodeDic();
		Parameters para = RunEnvironment.getInstance().getParameters();
		scaleFactor4Display = (Integer) para.getValue("ScaleFactor4Display");
		cityContext = new CityContext();
		cityProjection = GeographyFactoryFinder.createGeographyFactory(null).createGeography("CityGeography", cityContext,
						new GeographyParameters<City>(new SimpleAdder<City>()));
		try
		{
			GISFunctions.readShapefile(City.class, GlobalAttr.GIS_SHAPE_FILE, cityProjection, cityContext);
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		InitCity(cityContext);
		mainContext.addSubContext(cityContext);
		SpatialIndexManager.createIndex(cityProjection, City.class);
		agentContext = new AgentContext();
		AgentFactory agentFac = new AgentFactory();
		try
		{
			agentFac.InitPop(agentContext, GlobalAttr.POP_FILE_DIR);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		agentFac.InitHHMember(agentContext);
		try
		{
			agentFac.InitSocNetwork(agentContext, GlobalAttr.SOC_NET_FILE_DIR);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		LinkCityAgent(agentContext, cityContext);
		UpdateCityPopNum(cityContext);
		agentFac.InitSatisfaction(agentContext);
		mainContext.addSubContext(agentContext);
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters first_params = ScheduleParameters.createRepeating(1, 1, PriorityType.FIRST);
		schedule.schedule(first_params, this, "TimeTick");
		ScheduleParameters last_params = ScheduleParameters.createRepeating(1, 1, PriorityType.LAST);
		schedule.schedule(last_params, this, "Step");
		// for Surrogate Calibration:
		incomeWeights = new Hashtable<Integer, Float>();
		famWeights = new Hashtable<Integer, Float>();
		registWeights = new Hashtable<Integer, Float>();
		for (int year = 2001; year <= 2010; year++)
		{
			float incWei = para.getFloat("IncWei_" + year);
			float famWei = para.getFloat("FamWei_" + year);
			float regWei = para.getFloat("RegWei_" + year);
			incomeWeights.put(year, incWei);
			famWeights.put(year, famWei);
			registWeights.put(year, regWei);
		}
		// for evaluation
		eval = new ResultEval(this);
		try
		{
			eval.InitStatPopNum(GlobalAttr.CITY_STAT_POP_FILE);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		initEndTime = System.currentTimeMillis();
		return mainContext;
	}

	public void TimeTick()
	{
		if (simYear == 2000)
		{
			simStartTime = System.currentTimeMillis();
		}
		simYear++;
		System.out.println("simYear = " + simYear);
	}

	public void Step()
	{
		// update ind's destination
		Iterable<Agent> population = agentContext.getObjects(Agent.class);
		for (Iterator<Agent> iterator = population.iterator(); iterator.hasNext();)
		{
			Agent ind = iterator.next();
			ind.ComputeInterestCity();
		}
		// update city basic migNum
		for (Iterator<City> iterator = cityContext.getObjects(City.class).iterator(); iterator.hasNext();)
		{
			iterator.next().UpdateMigBasicNum();
		}
		// Calibration:
		MeanFieldCal();
//		ExternalCal();
		// migration:
		for (Iterator<Agent> iterator = population.iterator(); iterator.hasNext();)
		{
			iterator.next().Step_latter();
		}
		UpdateCityPopNum(cityContext);
		try
		{
			Thread.sleep(2 * 1000);
		} catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
		if (simYear == 2010)
		{
			simEndTime = System.currentTimeMillis();
			long secondsLast = ((initEndTime - initStartTime) + (simEndTime - simStartTime)) / 1000;
			System.out.println("RunningTime: " + secondsLast + " s");
			RunEnvironment.getInstance().endRun();
		}
	}

	private int ComputeMigNum(City origCity, City targetCity, float incomeWei, float famWei, float registWei)
	{
		int migSum = 0;
		for (Agent ind : origCity.population)
		{
			if (incomeWei * ind.income_satis_level + (1 - famWei - incomeWei - registWei) * ind.race_satis_level + famWei * (ind.family_satisfaction - 0.5)
							+ registWei * ind.regist_satis_level < ind.tolerant && ind.targetCity == targetCity)
			{
				migSum++;
			}
		}
		return migSum;
	}

	// mean-field calibration
	public void MeanFieldCal()
	{
		Iterable<Agent> population = agentContext.getObjects(Agent.class);
		LinkedHashMap<String, Integer> odAggState = new LinkedHashMap<String, Integer>();
		// compute state vector
		for (Iterator<Agent> iterator = population.iterator(); iterator.hasNext();)
		{
			Agent ind = iterator.next();
			int currCityID = ind.currentCity.cityCode;
			int destCityID = ind.targetCity.cityCode;
			boolean hasKey = false;
			for (String odStr : odAggState.keySet())
			{
				int currID = Integer.parseInt(odStr.substring(0, odStr.indexOf("-")));
				int destID = Integer.parseInt(odStr.substring(odStr.indexOf("-") + 1));
				if (currID == currCityID && destID == destCityID)
				{
					int popNum = odAggState.get(odStr);
					odAggState.put(odStr, popNum + 1);
					hasKey = true;
					break;
				}
			}
			if (!hasKey)
			{
				String keyStr = currCityID + "-" + destCityID;
				odAggState.put(keyStr, 1);
			}
		}
		// compute state transfer probability
		LinkedHashMap<String, Float> stateTransProb = new LinkedHashMap<String, Float>();
		for (String origStr : odAggState.keySet())
		{
			int beforeOrigID = Integer.parseInt(origStr.substring(0, origStr.indexOf("-")));
			int beforeDestID = Integer.parseInt(origStr.substring(origStr.indexOf("-") + 1));
			String migKey = beforeOrigID + "-" + beforeDestID;
			if (!CodeDictionary.annualCityMigRate.containsKey(migKey))
				continue;
			if (!CodeDictionary.annualCityMigRate.get(migKey).containsKey(simYear))
				continue;
			float interCityMigRate = CodeDictionary.annualCityMigRate.get(migKey).get(simYear);
			City origCity = null;
			Iterable<City> cityCollect = cityContext.getObjects(City.class);
			for (Iterator<City> iter = cityCollect.iterator(); iter.hasNext();)
			{
				City parCity = iter.next();
				if (parCity.cityCode == beforeOrigID)
				{
					origCity = parCity;
				}
			}
			if (origCity == null)
				continue;
			int basicMigNum = odAggState.get(origStr);
			if (basicMigNum <= 0)
				continue;
			interCityMigRate = ((float) (origCity.population.size())) / basicMigNum * interCityMigRate;
			LinkedHashMap<Integer, Integer> currPopNum = new LinkedHashMap<Integer, Integer>();
			int popSum = 0;
			for (String parStr : odAggState.keySet())
			{
				int origID = Integer.parseInt(parStr.substring(0, parStr.indexOf("-")));
				int destID = Integer.parseInt(parStr.substring(parStr.indexOf("-") + 1));
				if (origID == beforeDestID)
				{
					currPopNum.put(destID, odAggState.get(parStr));
					popSum += odAggState.get(parStr);
				}
			}
			for (Integer destID : currPopNum.keySet())
			{
				float transferProb = interCityMigRate * ((float) currPopNum.get(destID)) / popSum;
				String keyStr = beforeOrigID + "-" + beforeDestID + "-" + beforeDestID + "-" + destID;
				stateTransProb.put(keyStr, transferProb);
			}
		}
		// para calibration:
		for (Iterator<Agent> iterator = population.iterator(); iterator.hasNext();)
		{
			Agent ind = iterator.next();
			float migProb = 0;
			for (String stateTransKey : stateTransProb.keySet())
			{
				String[] cityIDs = stateTransKey.split("-");
				int currOrigID = Integer.parseInt(cityIDs[0]);
				int destID = Integer.parseInt(cityIDs[1]);
				if (currOrigID == ind.currentCity.cityCode && destID == ind.targetCity.cityCode)
				{
					migProb += stateTransProb.get(stateTransKey);
				}
			}
			if (GlobalAttr.rand.nextFloat() < migProb)
			{
				if (ind.tolerant > 0)
				{
					float maxCoef = ind.income_satis_level > ind.race_satis_level ? ind.income_satis_level : ind.race_satis_level;
					maxCoef = (float) ((maxCoef > (ind.family_satisfaction - 0.5) ? maxCoef : (ind.family_satisfaction - 0.5)));
					maxCoef = maxCoef > ind.regist_satis_level ? maxCoef : ind.regist_satis_level;
					if (Math.abs(maxCoef - ind.income_satis_level) < 0.01)
					{
						ind.incomeWei = 0;
						ind.famWei = (float) 0.3;
						ind.registWei = (float) 0.3;
					} else if (Math.abs(maxCoef - ind.race_satis_level) < 0.01)
					{
						ind.incomeWei = (float) 0.3;
						ind.famWei = (float) 0.4;
						ind.registWei = (float) 0.3;
					} else if (Math.abs(maxCoef - ind.family_satisfaction + 0.5) < 0.01)
					{
						ind.incomeWei = (float) 0.3;
						ind.famWei = 0;
						ind.registWei = (float) 0.3;
					} else
					{
						ind.incomeWei = (float) 0.3;
						ind.famWei = (float) 0.3;
						ind.registWei = 0;
					}
				} else if (ind.tolerant < 0)
				{
					float maxCoef = ind.income_satis_level < ind.race_satis_level ? ind.income_satis_level : ind.race_satis_level;
					maxCoef = (float) ((maxCoef < (ind.family_satisfaction - 0.5) ? maxCoef : (ind.family_satisfaction - 0.5)));
					maxCoef = maxCoef < ind.regist_satis_level ? maxCoef : ind.regist_satis_level;
					if (Math.abs(maxCoef - ind.income_satis_level) < 0.01)
					{
						ind.incomeWei = 1;
						ind.famWei = 0;
						ind.registWei = 0;
					} else if (Math.abs(maxCoef - ind.race_satis_level) < 0.01)
					{
						ind.incomeWei = 0;
						ind.famWei = 0;
						ind.registWei = 0;
					} else if (Math.abs(maxCoef - ind.family_satisfaction + 0.5) < 0.01)
					{
						ind.incomeWei = 0;
						ind.famWei = 1;
						ind.registWei = 0;
					} else
					{
						ind.incomeWei = 0;
						ind.famWei = 0;
						ind.registWei = 1;
					}
				} else
				{
					if (ind.income_satis_level < 0)
					{
						ind.incomeWei = 1;
						ind.famWei = 0;
						ind.registWei = 0;
					} else if (ind.race_satis_level < 0)
					{
						ind.incomeWei = 0;
						ind.famWei = 0;
						ind.registWei = 0;
					} else if (ind.family_satisfaction - 0.5 < 0)
					{
						ind.incomeWei = 0;
						ind.famWei = 1;
						ind.registWei = 0;
					} else if (ind.regist_satis_level < 0)
					{
						ind.incomeWei = 0;
						ind.famWei = 0;
						ind.registWei = 1;
					} else
					{
						System.out.println(ind.income_satis_level + "," + ind.race_satis_level + "," + (ind.family_satisfaction - 0.5) + ","
										+ ind.regist_satis_level);
					}
				}
			}
		}
	}

	private void InitCity(Context<City> context)
	{
		Iterable<City> cityCollect = context.getObjects(City.class);
		int index = -1;
		for (Iterator<City> iterator = cityCollect.iterator(); iterator.hasNext();)
		{
			City parCity = iterator.next();
			index++;
			parCity.cityCode = index;
			parCity.setName(Global.CodeDictionary.cityCodeName.get(index));
			GlobalAttr.globalMaxHHID.put(index, -1);
			GlobalAttr.globalMaxIndID.put(index, -1);
		}
		GlobalAttr.globalMaxHHID.put(362, -1);
	}

	public void LinkCityAgent(Context<Agent> agentContext, Context<City> cityContext)
	{
		Iterable<Agent> population = agentContext.getObjects(Agent.class);
		Iterable<City> cityCollect = cityContext.getObjects(City.class);
		for (Iterator<Agent> iterator = population.iterator(); iterator.hasNext();)
		{
			Agent ind = iterator.next();
			for (Iterator<City> cityIter = cityCollect.iterator(); cityIter.hasNext();)
			{
				City parCity = cityIter.next();
				if (parCity.cityCode == ind.resideCity)
				{
					ind.currentCity = parCity;
					ind.targetCity = parCity;
					parCity.CaptureOneInd(ind);
				}
			}
		}
	}

	public void UpdateCityPopNum(Context<City> cityContext)
	{
		Iterable<City> cityCollect = cityContext.getObjects(City.class);
		for (Iterator<City> cityIter = cityCollect.iterator(); cityIter.hasNext();)
		{
			City parCity = cityIter.next();
			parCity.popRange = ((float) parCity.population.size()) / scaleFactor4Display;
			if (parCity.popRange > 10)
			{
				parCity.popRange = 10;
			}
		}
	}

	public static int getSimYear()
	{
		return simYear;
	}
}
