package Attributes;

public enum EEducateLevel
{
	infant(0), notEducated(1), literClass(2), primaSchool(3), junMidSchool(4), senMidSchool(5), polytechSchool(6), college(7), university(8), graduate(9);
	private int value = 0;

	private EEducateLevel(int value)
	{
		this.value = value;
	}

	public static EEducateLevel valueOf(int value)
	{
		switch (value)
		{
			case 0:
				return infant;
			case 1:
				return notEducated;
			case 2:
				return literClass;
			case 3:
				return primaSchool;
			case 4:
				return junMidSchool;
			case 5:
				return senMidSchool;
			case 6:
				return polytechSchool;
			case 7:
				return college;
			case 8:
				return university;
			case 9:
				return graduate;
			default:
				return null;
		}
	}

	public int value()
	{
		return this.value;
	}
}
