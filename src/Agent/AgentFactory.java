package Agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.csvreader.CsvReader;

import Global.GlobalAttr;
import repast.simphony.context.Context;

public class AgentFactory
{
	public void InitPop(Context<Agent> context, String popFileDir) throws IOException
	{
		for (int i = 11; i <= 65; i++)
		{
			Path fileName = Paths.get(popFileDir, String.valueOf(i) + ".csv");
			File popFile = fileName.toFile( );
			if (!popFile.exists( ))
				continue;
			CsvReader csvReader = new CsvReader(fileName.toString( ));
			// 读表头
			csvReader.readHeaders( );
			while (csvReader.readRecord( ))
			{
				int hhID = Integer.parseInt(csvReader.get("HouseholdID"));
				// int hhProv = Integer.parseInt(csvReader.get("HHProvince"));
				int hhCity = Integer.parseInt(csvReader.get("HHCity"));
				// int memNum = Integer.parseInt(csvReader.get("MemberNumber"));
				int indID = Integer.parseInt(csvReader.get("IndividualID"));
				int gender = Integer.parseInt(csvReader.get("Gender"));
				int resideProv = Integer.parseInt(csvReader.get("ResideProvince"));
				int resideCity = Integer.parseInt(csvReader.get("ResideCity"));
				// int resideType =
				// Integer.parseInt(csvReader.get("ResidenceType"));
				int age = Integer.parseInt(csvReader.get("Age"));
				int race = Integer.parseInt(csvReader.get("Race"));
				// int eduLv =
				// Integer.parseInt(csvReader.get("EducationalLevel"));
				// int registType =
				// Integer.parseInt(csvReader.get("RegistType"));
				int registProv = Integer.parseInt(csvReader.get("RegistProvince"));
				int maritalStatus = Integer.parseInt(csvReader.get("MaritalStatus"));
				if (GlobalAttr.globalMaxIndID.get(resideCity) < indID)
				{
					GlobalAttr.globalMaxIndID.put(resideCity, indID);
				}
				int maxHHID = hhID / 1000;
				if (GlobalAttr.globalMaxHHID.get(hhCity) < maxHHID)
				{
					GlobalAttr.globalMaxHHID.put(hhCity, maxHHID);
				}
				indID = indID * 1000 + resideCity;
				Agent individual = new Agent(indID, hhID, resideProv, resideCity, gender, race, age, registProv, maritalStatus);
				context.add(individual);
			}
			csvReader.close( );
		}
	}

	public void InitHHMember(Context<Agent> context)
	{
		Iterable<Agent> population = context.getObjects(Agent.class);
		Iterable<Agent> candidates = context.getObjects(Agent.class);
		int index = 0;
		for (Iterator<Agent> iterator = population.iterator( ); iterator.hasNext( );)
		{
			Agent ind = iterator.next( );
			for (Iterator<Agent> candIter = candidates.iterator( ); candIter.hasNext( );)
			{
				Agent parAgent = candIter.next( );
				if (parAgent.resideCity != ind.resideCity)
					continue;
				if (parAgent.hhID == ind.hhID && parAgent.indID != ind.indID)
				{
					ind.hhMembers.add(parAgent);
				}
			}
			index++;
			if (index % 10000 == 0)
			{
				System.out.println(index + " / " + context.size( ) + " hh mem added...");
			}
		}
	}

	public void InitSocNetwork(Context<Agent> context, String socNetFileDir) throws IOException
	{
		for (int i = 11; i <= 65; i++)
		{
			Path fileName = Paths.get(socNetFileDir, String.valueOf(i) + ".csv");
			File popFile = fileName.toFile( );
			if (!popFile.exists( ))
				continue;
			FileReader reader = null;
			BufferedReader br = null;
			List<SocRelation> socNet = new ArrayList<SocRelation>( );
			try
			{
				reader = new FileReader(popFile);
				br = new BufferedReader(reader);
				String line = br.readLine( );
				while ((line = br.readLine( )) != null)
				{
					String[ ] indIDs = line.split(",");
					SocRelation socRel = new SocRelation( );
					socRel.indID = Integer.parseInt(indIDs[0]);// 考察本人ID
					for (int j = 1; j < indIDs.length; j++)
					{
						int friendID = Integer.parseInt(indIDs[j]);
						if (friendID == socRel.indID)
							continue;// 过滤掉本人
						socRel.friendsID.add(friendID);
					}
					socNet.add(socRel);
				}
				if (reader != null)
					reader.close( );
				if (br != null)
					br.close( );
			} catch (IOException e)
			{
				e.printStackTrace( );
			}
			// 查找个体及朋友
			Iterable<Agent> population = context.getObjects(Agent.class);
			for (Iterator<Agent> iterator = population.iterator( ); iterator.hasNext( );)
			{
				Agent ind = iterator.next( );
				for (SocRelation socRel : socNet)
				{
					if (socRel.indID == ind.indID)
					{
						socRel.ind = ind;
					}
					if (socRel.friendsID.contains(ind.indID))
					{
						socRel.friends.add(ind);
					}
				}
			}
			// 添加社交关系
			for (SocRelation socRel : socNet)
			{
				for (Agent friend : socRel.friends)
				{
					socRel.ind.friends.add(friend);
				}
				socRel.ind.friendsID.clear( );
			}
			System.out.println("Social Network " + i + " generated...");
		}
	}

	class SocRelation
	{
		public int indID = -1;
		public HashSet<Integer> friendsID = new HashSet<Integer>( );
		public Agent ind = null;
		public HashSet<Agent> friends = new HashSet<Agent>( );
	}

	public void InitSatisfaction(Context<Agent> context)
	{
		Iterable<Agent> population = context.getObjects(Agent.class);
		for (Iterator<Agent> iterator = population.iterator( ); iterator.hasNext( );)
		{
			Agent ind = iterator.next( );
			ind.UpdateSatisfaction( );
		}
	}
}
