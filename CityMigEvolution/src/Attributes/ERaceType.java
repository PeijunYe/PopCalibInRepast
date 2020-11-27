package Attributes;

public enum ERaceType
{
	Han(1), MengGu(2), Hui(3), Zang(4), WeiWuEr(5), Miao(6), Yi(7), Zhuang(8), BuYi(9), ChaoXian(10), Man(11), Dong(12), Yao(13), Bai(14), TuJia(15), HaNi(16),
	HaSaKe(17), Dai(18), Li(19), LiSu(20), Wa(21), She(22), GaoShan(23), LaHu(24), Shui(25), DongXiang(26), NaXi(27), JingPo(28), KeErKeZi(29), Tu(30),
	DaWoEr(31), MuLao(32), Qiang(33), BuLang(34), SaLa(35), MaoNan(36), GeLao(37), XiBo(38), AChang(39), PuMi(40), TaJiKe(41), Nu(42), WuZiBieKe(43),
	ELuoSi(44), EWenKe(45), DeAng(46), BaoAn(47), YuGu(48), Jing(49), TaTaEr(50), DuLong(51), ELunChun(52), HeZhe(53), MenBa(54), LuoBa(55), JiNuo(56),
	Other(57), Foreign(58);
	private int value = 0;

	private ERaceType(int value)
	{
		this.value = value;
	}

	public static ERaceType valueOf(int value)
	{
		switch (value)
		{
			case 1:
				return Han;
			case 2:
				return MengGu;
			case 3:
				return Hui;
			case 4:
				return Zang;
			case 5:
				return WeiWuEr;
			case 6:
				return Miao;
			case 7:
				return Yi;
			case 8:
				return Zhuang;
			case 9:
				return BuYi;
			case 10:
				return ChaoXian;
			case 11:
				return Man;
			case 12:
				return Dong;
			case 13:
				return Yao;
			case 14:
				return Bai;
			case 15:
				return TuJia;
			case 16:
				return HaNi;
			case 17:
				return HaSaKe;
			case 18:
				return Dai;
			case 19:
				return Li;
			case 20:
				return LiSu;
			case 21:
				return Wa;
			case 22:
				return She;
			case 23:
				return GaoShan;
			case 24:
				return LaHu;
			case 25:
				return Shui;
			case 26:
				return DongXiang;
			case 27:
				return NaXi;
			case 28:
				return JingPo;
			case 29:
				return KeErKeZi;
			case 30:
				return Tu;
			case 31:
				return DaWoEr;
			case 32:
				return MuLao;
			case 33:
				return Qiang;
			case 34:
				return BuLang;
			case 35:
				return SaLa;
			case 36:
				return MaoNan;
			case 37:
				return GeLao;
			case 38:
				return XiBo;
			case 39:
				return AChang;
			case 40:
				return PuMi;
			case 41:
				return TaJiKe;
			case 42:
				return Nu;
			case 43:
				return WuZiBieKe;
			case 44:
				return ELuoSi;
			case 45:
				return EWenKe;
			case 46:
				return DeAng;
			case 47:
				return BaoAn;
			case 48:
				return YuGu;
			case 49:
				return Jing;
			case 50:
				return TaTaEr;
			case 51:
				return DuLong;
			case 52:
				return ELunChun;
			case 53:
				return HeZhe;
			case 54:
				return MenBa;
			case 55:
				return LuoBa;
			case 56:
				return JiNuo;
			case 57:
				return Other;
			case 58:
				return Foreign;
			default:
				return null;
		}
	}

	public int value()
	{
		return this.value;
	}
}
