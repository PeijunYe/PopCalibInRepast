package Environment;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import Agent.Agent;

public class City implements FixedGeography
{
	public int cityCode;
	private String name;
	private Coordinate coords;
	public List<Agent> population;
	public float popRange;
	private int elderNum;
	private int pregnantNum;
	private int unMarriedNum;
	private int marriedNum;
	private int migBasicNum;

	public City()
	{
		cityCode = -1;
		name = "";
		population = new ArrayList<Agent>();
		popRange = 0;
		elderNum = 0;
		pregnantNum = 0;
		unMarriedNum = 0;
		marriedNum = 0;
		migBasicNum = 0;
	}

	public void InitParPopNum()
	{
		elderNum = 0;
		pregnantNum = 0;
		unMarriedNum = 0;
		marriedNum = 0;
		for (Agent ind : population)
		{
			if (ind.age >= 50)
			{
				elderNum++;
			}
			if (ind.gender == 2 && ind.age < 50 && ind.age > 20 && !ind.hasChild)
			{
				pregnantNum++;
			}
			if (ind.gender == 1 && ind.age >= 22 && ind.age <= 70)
			{
				if (ind.maritalStatus == 1)
				{
					marriedNum++;
				} else
				{
					unMarriedNum++;
				}
			}
			if (ind.gender == 2 && ind.age >= 20 && ind.age <= 70)
			{
				if (ind.maritalStatus == 1)
				{
					marriedNum++;
				} else
				{
					unMarriedNum++;
				}
			}
		}
	}

	public void CaptureOneInd(Agent one_ind)
	{
		population.add(one_ind);
		if (one_ind.age >= 50)
		{
			elderNum++;
		}
		if (one_ind.gender == 2 && one_ind.age < 50 && one_ind.age > 20 && !one_ind.hasChild)
		{
			pregnantNum++;
		}
		if (one_ind.gender == 1 && one_ind.age >= 22 && one_ind.age <= 70)
		{
			if (one_ind.maritalStatus == 1)
			{
				marriedNum++;
			} else
			{
				unMarriedNum++;
			}
		}
		if (one_ind.gender == 2 && one_ind.age >= 20 && one_ind.age <= 70)
		{
			if (one_ind.maritalStatus == 1)
			{
				marriedNum++;
			} else
			{
				unMarriedNum++;
			}
		}
	}

	public void RemoveOneInd(Agent one_ind)
	{
		population.remove(one_ind);
		if (one_ind.age >= 50)
		{
			elderNum--;
		}
		if (one_ind.gender == 2 && one_ind.age < 50 && one_ind.age > 20 && !one_ind.hasChild)
		{
			pregnantNum--;
		}
		if (one_ind.gender == 1 && one_ind.age >= 22 && one_ind.age <= 70)
		{
			if (one_ind.maritalStatus == 1)
			{
				marriedNum--;
			} else
			{
				unMarriedNum--;
			}
		}
		if (one_ind.gender == 2 && one_ind.age >= 20 && one_ind.age <= 70)
		{
			if (one_ind.maritalStatus == 1)
			{
				marriedNum--;
			} else
			{
				unMarriedNum--;
			}
		}
	}

	public void UpdateMigBasicNum()
	{
		migBasicNum = 0;
		for (Agent ind : population)
		{
			if (ind.age >= 16 && (ind.income_satis_level + ind.race_satis_level < 0)
							&& (ind.max_income_city != ind.currentCity || ind.max_race_city != ind.currentCity))
			{
				migBasicNum++;
			}
		}
	}

	public void IncreaseMarriedNum(int num)
	{
		marriedNum += num;
	}

	public void DecreaseMarriedNum(int num)
	{
		marriedNum -= num;
	}

	public void IncreaseUnmarriedNum(int num)
	{
		unMarriedNum += num;
	}

	public void DecreaseUnmarriedNum(int num)
	{
		unMarriedNum -= num;
	}

	@Override
	public Coordinate getCoords()
	{
		return this.coords;
	}

	@Override
	public void setCoords(Coordinate c)
	{
		this.coords = c;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getElderNum()
	{
		return elderNum;
	}

	public int getPregnantNum()
	{
		return pregnantNum;
	}

	public int getUnMarriedNum()
	{
		return unMarriedNum;
	}

	public int getMarriedNum()
	{
		return marriedNum;
	}

	public int getMigBasicNum()
	{
		return migBasicNum;
	}

	public float getPopRange()
	{
		return popRange;
	}

	public int getCityCode()
	{
		return cityCode;
	}

	public int getPopNum()
	{
		return population.size();
	}
}
