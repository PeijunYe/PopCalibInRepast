package Attributes;

public enum ERegistType
{
	rural(1), urban(2), other(3);
	private int value = 0;

	private ERegistType(int value)
	{
		this.value = value;
	}

	public static ERegistType valueOf(int value)
	{
		switch (value)
		{
			case 1:
				return rural;
			case 2:
				return urban;
			case 3:
				return other;
			default:
				return null;
		}
	}

	public int value()
	{
		return this.value;
	}
}
