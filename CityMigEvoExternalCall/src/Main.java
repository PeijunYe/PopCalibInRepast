import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;

public class Main
{

	public static void main(String[] args)
	{
		if (args.length == 0)
			return;
		String calType = args[0];
		String BASE_DIR = "../CityMigEvolution";
		String ERROR_FILE = "ParaAndRunTime_" + calType + ".txt";
		File outFile = new File(Paths.get(BASE_DIR, ERROR_FILE).toString());
		try
		{
			if (outFile.exists())
				outFile.delete();
			outFile.createNewFile();
			FileOutputStream outStream = new FileOutputStream(outFile);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
			out.write("IncomeWeights,FamWeights,RegWeights,TotalError,RunTime\n");
			out.close();
			outStream.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		if (calType.contains("surrogate"))
		{
			SurCalibrator surCal = new SurCalibrator();
			surCal.Caliration(calType);
		} else if (calType.contains("momca"))
		{
			MomcaCalibrator momcaCal = new MomcaCalibrator();
			momcaCal.Caliration();
		}
	}
}
