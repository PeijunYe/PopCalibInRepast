package Agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import Environment.City;
import Global.CodeDictionary;
import Global.GlobalAttr;
import cityMigEvolution.MainContext;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.ContextUtils;

public class Agent
{
	public int indID;
	public int hhID;
	public int resideProv;
	public int resideCity;
	public int gender; // Male = 1, Female = 2
	public int race;
	public int age;
	public int registProv;
	public int maritalStatus;// Married = 1, NotMarried = 2
	public boolean hasChild;
	private float income;
	private float regist_satisfaction;
	private float race_satisfaction;
	public CopyOnWriteArrayList<Agent> hhMembers;
	public CopyOnWriteArrayList<Integer> friendsID;
	public CopyOnWriteArrayList<Agent> friends;
	// state variables:
	public City currentCity;
	public float tolerant;// personality
	public float family_satisfaction;
	public float income_satis_level;
	public float regist_satis_level;
	public float race_satis_level;
	public float famWei = (float) 0.194;
	public float incomeWei = (float) 0.279;
	public float registWei = (float) 0.489;
	public City max_income_city;
	public City max_race_city;
	public City max_family_city;
	public City max_regist_city;

	public City targetCity;

	public Agent(int indID, int hhID, int resideProv, int resideCity, int gender, int race, int age, int registProv, int maritalStatus)
	{
		super();
		this.indID = indID;
		this.hhID = hhID;
		this.resideProv = resideProv;
		this.resideCity = resideCity;
		this.gender = gender;
		this.race = race;
		this.age = age;
		this.registProv = registProv;
		this.maritalStatus = maritalStatus;
		hasChild = false;
		hhMembers = new CopyOnWriteArrayList<Agent>();
		friendsID = new CopyOnWriteArrayList<Integer>();
		friends = new CopyOnWriteArrayList<Agent>();
		double seed = RandomHelper.getUniform().nextDouble();
		if (seed < 0.4)
		{
			tolerant = (float) 0.0;
		} else if (seed < 0.7)
		{
			tolerant = (float) 0.1;
		} else
		{
			tolerant = (float) -0.1;
		}
	}

	public void UpdateSatisfaction()
	{
		// initialize income
		if (gender == 1)
		{
			if (age > 18 && age < 60)
			{
				float averIncome = CodeDictionary.cityAnnualIncomeLv.get(currentCity.cityCode).get(MainContext.getSimYear());
				income = (float) (averIncome / 2 * GlobalAttr.rand.nextGaussian() + averIncome);
			} else
			{
				income = (float) 0.0;
			}
		} else
		{
			if (age > 18 && age < 55)
			{
				float averIncome = CodeDictionary.cityAnnualIncomeLv.get(currentCity.cityCode).get(MainContext.getSimYear());
				income = (float) (averIncome / 2 * GlobalAttr.rand.nextGaussian() + averIncome);
			} else
			{
				income = (float) 0.0;
			}
		}
		// initialize registration satisfaction
		if (resideProv == registProv || registProv == 0)
		{
			regist_satisfaction = (float) 1.0;
		} else
		{
			String distKey = registProv + "-" + currentCity.cityCode;
			float distance = -1;
			for (String provCityPair : CodeDictionary.dist2ProvCapital.keySet())
			{
				if (provCityPair.contains(distKey))
				{
					distance = CodeDictionary.dist2ProvCapital.get(provCityPair);
				}
			}
			if (distance > 0)
			{
				regist_satisfaction = 10 / distance;
			} else
			{
				System.out.println("Agent " + indID + " distance = -1");
			}
		}
		// initialize race satisfaction
		if (race >= 2 && race <= 56)
		{
			if (CodeDictionary.minorPrincipalProv.get(race).contains(resideProv))
			{
				int provNum = CodeDictionary.minorPrincipalProv.get(race).size();
				int provIndex = CodeDictionary.minorPrincipalProv.get(race).indexOf(resideProv);
				race_satisfaction = (provNum - provIndex) / provNum;
			} else
			{
				race_satisfaction = (float) 0.0;
			}
		} else
		{
			race_satisfaction = (float) 0.0;
		}
		UpdateFamSat();
	}

	@ScheduledMethod(start = 1, interval = 2)
	public void Step_former()
	{
		UpdateAge();
		UpdateFamily();
		UpdateFriends();
		UpdateSatisfaction();
		Perceive();
	}

	public void Step_latter()
	{
		Migration();
	}

	private void Exit()
	{
		for (Iterator<Agent> iterator = hhMembers.iterator(); iterator.hasNext();)
		{
			Agent hhMem = iterator.next();
			hhMem.hhMembers.remove(this);
		}
		for (Iterator<Agent> iterator = this.friends.iterator(); iterator.hasNext();)
		{
			Agent friend = iterator.next();
			friend.friends.remove(this);
		}
		currentCity.RemoveOneInd(this);
		Context<Agent> agentContext = ContextUtils.getContext(this);
		agentContext.remove(this);
	}

	private void UpdateAge()
	{
		age = age + 1;
		// death
		if (age >= 50)
		{
			if (!CodeDictionary.annualDeathRate.containsKey(currentCity.cityCode))
				System.out.println("currentCity.cityCode = " + currentCity.cityCode);
			if (!CodeDictionary.annualDeathRate.get(currentCity.cityCode).containsKey(MainContext.simYear))
				System.out.println("simYear = " + MainContext.simYear);
			float deathRate = CodeDictionary.annualDeathRate.get(currentCity.cityCode).get(MainContext.simYear);// deathRate
			if (currentCity.getElderNum() > 0)
			{
				float ageDeathRate = deathRate * ((float) (currentCity.population.size()) / currentCity.getElderNum());
				if (GlobalAttr.rand.nextFloat() < ageDeathRate)
				{
					Exit();
				}
			} else
			{
			}
		}
		// birth
		if (gender == 2 && age < 50 && age > 20 && !hasChild)
		{
			float birthRate = CodeDictionary.annualBirthRate.get(currentCity.cityCode).get(MainContext.simYear);// birthRate
			if (currentCity.getPregnantNum() > 0)
			{
				float ageBirthRate = birthRate * ((float) (currentCity.population.size()) / currentCity.getPregnantNum());
				if (GlobalAttr.rand.nextFloat() < ageBirthRate)
				{
					// create a new child
					int childID = GlobalAttr.globalMaxIndID.get(currentCity.cityCode) + 1;
					GlobalAttr.globalMaxIndID.put(currentCity.cityCode, childID);
					childID = childID * 1000 + currentCity.cityCode;
					int childGender = RandomHelper.getUniform().nextBoolean() ? 1 : 2;
					int childAge = 0;
					int childEdu = 0;
					int childMarStat = 2;
					Agent child = new Agent(childID, hhID, resideProv, currentCity.cityCode, childGender, race, childAge, resideProv, childMarStat);
					// child.indID = childID;
					// child.hhID = hhID;
					// child.resideProv = resideProv;
					// child.resideCity = currentCity.cityCode;
					// child.gender = childGender;
					// child.race = race;
					// child.resideType = resideType;
					// child.age = childAge;
					// child.eduLv = childEdu;
					// child.registType = registType;
					// child.registProv = resideProv;
					// child.maritalStatus = childMarStat;
					child.currentCity = this.currentCity;
					this.currentCity.CaptureOneInd(child);
					for (Iterator<Agent> iterator = hhMembers.iterator(); iterator.hasNext();)
					{
						Agent hhMem = iterator.next();
						child.hhMembers.add(hhMem);
						hhMem.hhMembers.add(child);
					}
					ContextUtils.getContext(this).add(child);
					this.hasChild = true;
				}
			} else
			{
			}
		}
	}

	private void UpdateFamily()
	{
		// marriage
		if (gender == 1 && age >= 22 && age < 70 && maritalStatus == 2)
		{
			if (!CodeDictionary.annualMarryRate.containsKey(MainContext.getSimYear()))
				System.out.println("simYear = " + MainContext.simYear);
			float marryRate = currentCity.getUnMarriedNum() == 0 ? 0
							: ((float) (currentCity.population.size())) / currentCity.getUnMarriedNum()
											* CodeDictionary.annualMarryRate.get(MainContext.simYear);
			if (GlobalAttr.rand.nextFloat() < marryRate)
			{
				for (Agent parAgent : friends)
				{
					if (parAgent.gender == 2 && parAgent.age >= 20 && parAgent.age < 70 && parAgent.maritalStatus == 2 && Math.abs(parAgent.age - age) <= 10)
					{
						int maxHHID = GlobalAttr.globalMaxHHID.get(currentCity.cityCode) + 1;
						GlobalAttr.globalMaxHHID.put(currentCity.cityCode, maxHHID);
						hhID = maxHHID * 10000 + currentCity.cityCode;
						parAgent.hhID = hhID;
						this.maritalStatus = 1;
						parAgent.maritalStatus = 1;
						this.hhMembers.clear();
						this.hhMembers.add(parAgent);
						parAgent.hhMembers.clear();
						parAgent.hhMembers.add(this);
						currentCity.DecreaseUnmarriedNum(2);
						currentCity.IncreaseMarriedNum(2);
					}
				}
			}
		} else if (gender == 2 && age >= 20 && age < 70 && maritalStatus == 2)
		{
			if (!CodeDictionary.annualMarryRate.containsKey(MainContext.getSimYear()))
				System.out.println("simYear = " + MainContext.simYear);
			float marryRate = currentCity.getUnMarriedNum() == 0 ? 0
							: ((float) (currentCity.population.size())) / currentCity.getUnMarriedNum()
											* CodeDictionary.annualMarryRate.get(MainContext.simYear);
			if (GlobalAttr.rand.nextFloat() < marryRate)
			{
				for (Agent parAgent : friends)
				{
					if (parAgent.gender == 1 && parAgent.age >= 22 && parAgent.age < 70 && parAgent.maritalStatus == 2 & Math.abs(parAgent.age - age) <= 10)
					{
						int maxHHID = GlobalAttr.globalMaxHHID.get(currentCity.cityCode) + 1;
						GlobalAttr.globalMaxHHID.put(currentCity.cityCode, maxHHID);
						hhID = maxHHID * 10000 + currentCity.cityCode;
						parAgent.hhID = hhID;
						this.maritalStatus = 1;
						parAgent.maritalStatus = 1;
						this.hhMembers.clear();
						this.hhMembers.add(parAgent);
						parAgent.hhMembers.clear();
						parAgent.hhMembers.add(this);
						currentCity.DecreaseUnmarriedNum(2);
						currentCity.IncreaseMarriedNum(2);
					}
				}
			}
		} else if (maritalStatus == 1)// divorce
		{
			if (!CodeDictionary.annualDivorceRate.containsKey(MainContext.getSimYear()))
				System.out.println("simYear = " + MainContext.simYear);
			float divorceRate = currentCity.getMarriedNum() == 0 ? 0
							: ((float) (currentCity.population.size())) / currentCity.getMarriedNum()
											* CodeDictionary.annualDivorceRate.get(MainContext.simYear);
			if (GlobalAttr.rand.nextFloat() < divorceRate)
			{
				int maxHHID = GlobalAttr.globalMaxHHID.get(currentCity.cityCode) + 1;
				GlobalAttr.globalMaxHHID.put(currentCity.cityCode, maxHHID);
				hhID = maxHHID * 10000 + currentCity.cityCode;
				this.hhMembers.clear();
				this.maritalStatus = 2;
				currentCity.IncreaseUnmarriedNum(2);
				currentCity.DecreaseMarriedNum(2);
			}
		}
	}

	private void UpdateFamSat()
	{
		// initialize family satisfaction
		if (hhMembers.size() > 0)
		{
			int sameMemNum = 0;
			for (Agent parAgent : hhMembers)
			{
				if (parAgent.currentCity.cityCode == this.currentCity.cityCode)
				{
					sameMemNum++;
				}
			}
			family_satisfaction = ((float) sameMemNum) / hhMembers.size();
		} else
		{
			family_satisfaction = (float) 1.0;
		}
	}

	private void UpdateFriends()
	{
		if (friends.size() >= 1)
		{
			if (GlobalAttr.rand.nextFloat() < GlobalAttr.FRIEND_SUBSTITUTION_RATE)
			{
				List<Agent> candidates = new ArrayList<Agent>();
				for (Agent parAgent : friends)
				{
					for (Agent parCand : parAgent.friends)
					{
						if (!(friends.contains(parCand)) && parCand.indID != indID && Math.abs(parCand.age - age) <= 5)
						{
							candidates.add(parCand);
						}
					}
				}
				if (candidates.size() <= 0)
					return;
				int index = GlobalAttr.rand.nextInt(this.friends.size());
				Agent indexedFriend = this.friends.get(index);
				indexedFriend.friends.remove(this);
				this.friends.remove(index);
				index = GlobalAttr.rand.nextInt(candidates.size());
				this.friends.add(candidates.get(index));
				candidates.get(index).friends.add(this);
			}
		}
	}

	private void Perceive()
	{
		int friendNum = friends.size();
		if (friendNum > 0)
		{
			float totalIncome = (float) 0.0;
			float totalRaceSatis = (float) 0.0;
			float totalRegistSatis = (float) 0.0;
			int employeeNum = 0;
			max_income_city = currentCity;
			max_race_city = currentCity;
			float maxIncome = income;
			float maxRaceSat = race_satisfaction;
			for (Agent parFriend : friends)
			{
				if (parFriend.gender == 1 && parFriend.age > 18 && parFriend.age < 60)
				{
					totalIncome += parFriend.income;
					employeeNum++;
					if (parFriend.income > maxIncome && parFriend.currentCity.cityCode != currentCity.cityCode)
					{
						maxIncome = parFriend.income;
						max_income_city = parFriend.currentCity;
					}
				} else if (parFriend.gender == 2 && parFriend.age > 18 && parFriend.age < 55)
				{
					totalIncome += parFriend.income;
					employeeNum++;
					if (parFriend.income > maxIncome && parFriend.currentCity.cityCode != currentCity.cityCode)
					{
						maxIncome = parFriend.income;
						max_income_city = parFriend.currentCity;
					}
				}
				if (parFriend.race == race && parFriend.race_satisfaction > maxRaceSat && parFriend.currentCity.cityCode != currentCity.cityCode)
				{
					maxRaceSat = parFriend.race_satisfaction;
					max_race_city = parFriend.currentCity;
				}
				totalRaceSatis = totalRaceSatis + parFriend.race_satisfaction;
				totalRegistSatis = totalRegistSatis + parFriend.regist_satisfaction;
			}
			if (employeeNum > 0)
			{
				income_satis_level = (income - totalIncome / employeeNum) / income;
			} else
			{
				income_satis_level = (float) 0.0;
			}
			if (race >= 2 && race <= 56)
			{
				race_satis_level = race_satisfaction - totalRaceSatis / friendNum;
			} else
			{
				race_satis_level = (float) 0.0;
			}
			if (resideProv == registProv || registProv == 0)
			{
				regist_satis_level = (float) 0.0;
			} else
			{
				regist_satis_level = regist_satisfaction - totalRegistSatis / friendNum;
			}
		} else
		{
			income_satis_level = (float) 0.0;
			race_satis_level = (float) 0.0;
			regist_satis_level = (float) 0.0;
			max_income_city = currentCity;
			max_race_city = currentCity;
		}
	}

	public void ComputeInterestCity()
	{
		targetCity = currentCity;
		max_family_city = currentCity;
		if (family_satisfaction < 0.5)
		{
			HashMap<City, Integer> cityFreq = new HashMap<City, Integer>();
			for (Agent parAgent : hhMembers)
			{
				if (cityFreq.containsKey(parAgent.currentCity))
				{
					cityFreq.put(parAgent.currentCity, cityFreq.get(parAgent.currentCity) + 1);
				} else
				{
					cityFreq.put(parAgent.currentCity, 1);
				}
			}
			int maxMemNum = -1;
			for (City parCity : cityFreq.keySet())
			{
				if (cityFreq.get(parCity) >= maxMemNum)
				{
					maxMemNum = cityFreq.get(parCity);
					max_family_city = parCity;
				}
			}
		}
		max_regist_city = currentCity;
		if (regist_satis_level < 0 && registProv != 0)
		{
			List<Integer> regist_city_cand = new ArrayList<Integer>();
			for (Integer parCity : CodeDictionary.cityProvMapping.keySet())
			{
				if (CodeDictionary.cityProvMapping.get(parCity) == registProv)
				{
					regist_city_cand.add(parCity);
				}
			}
			if (regist_city_cand.size() > 0)
			{
				int cityCode = regist_city_cand.get(GlobalAttr.rand.nextInt(regist_city_cand.size()));
				Iterable<City> cityCollect = ContextUtils.getContext(this).getObjects(City.class);
				for (Iterator<City> iterator = cityCollect.iterator(); iterator.hasNext();)
				{
					City parCity = iterator.next();
					if (parCity.cityCode == cityCode)
					{
						max_regist_city = parCity;
						break;
					}
				}
			}
		}
		if (incomeWei * income_satis_level + (1 - famWei - incomeWei - registWei) * race_satis_level + famWei * (family_satisfaction - 0.5)
						+ registWei * regist_satis_level < tolerant
						&& (max_income_city != currentCity || max_regist_city != currentCity || max_race_city != currentCity || max_family_city != currentCity))
		{
			HashMap<City, Float> city_candidates = new HashMap<City, Float>();
			if (max_income_city != currentCity)
			{
				city_candidates.put(max_income_city, income_satis_level);
			}
			if (max_family_city != currentCity)
			{
				city_candidates.put(max_family_city, (float) (family_satisfaction - 0.5));
			}
			if (max_regist_city != currentCity)
			{
				city_candidates.put(max_regist_city, regist_satis_level);
			}
			if (max_race_city != currentCity)
			{
				city_candidates.put(max_race_city, race_satis_level);
			}
			if (city_candidates.size() > 0)
			{
				float minSat = Float.MAX_VALUE;
				for (City cityCand : city_candidates.keySet())
				{
					if (city_candidates.get(cityCand) < minSat && cityCand != null)// attention
					{
						minSat = city_candidates.get(cityCand);
						targetCity = cityCand;
					}
				}
			}
		}
	}

	// migration
	private void Migration()
	{
		if (incomeWei * income_satis_level + (1 - famWei - incomeWei - registWei) * race_satis_level + famWei * (family_satisfaction - 0.5)
						+ registWei * regist_satis_level < tolerant && targetCity != currentCity)
		{
			currentCity.RemoveOneInd(this);
			resideCity = targetCity.cityCode;
			resideProv = CodeDictionary.cityProvMapping.get(resideCity);
			targetCity.CaptureOneInd(this);
			currentCity = targetCity;
		}
	}
}
