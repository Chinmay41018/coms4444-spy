package spy.g5;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class valid
{
	private final static int SIZE = 100;
	private ArrayList<ArrayList<Record>> truth_table = null;
	
	public valid()
	{
		
	}
	
	public valid(ArrayList<ArrayList<Record>> truth)
	{
		this.truth_table = truth;
	}
	
	public void update_truth(ArrayList<ArrayList<Record>> truth)
	{
		this.truth_table = truth;
	}
	
	// Vaibhav patch: Is each step valid?
	public boolean is_valid_path(List<Point> path)
	{
		// Obviously empty path is bad!
		if(path.isEmpty())
		{
			return false;
		}

		for (int i = 0; i < path.size(); i++)
		{
			Point test = path.get(i);
			Record verify = truth_table.get(test.x).get(test.y);
			if(verify == null)
			{
				// TODO: Continue, check regardless?
				// Or check the step!
				//return false;
				continue;
			}
			
			if (i == 0)
			{
				// Not Location of Package!
				if(verify.getPT() != 1)
				{
					return false;
				}
				// Check if next step is valid!
				Point test_next = path.get(i + 1);
				if (!valid_next_step(test, test_next))
				{
					return false;
				}
				
			}
			else if(i == path.size() - 1)
			{
				// Not Location of Target!
				if(verify.getPT() != 2)
				{
					return false;
				}
			}
			else
			{
				// Check if next step is valid!
				Point test_next = path.get(i + 1);
				if (!valid_next_step(test, test_next))
				{
					return false;
				}
				// Are any of these tiles muddy?
				// Conidtion 1 -> Muddy and 2 -> Water!
				if(verify.getC() != 0)
				{
					return false;
				}	
			}
		}
		// Well the path looks good according to the truth table
		return true;
	}
	
	private boolean valid_next_step(Point from, Point to)
	{
		int x = from.x - to.x;
		int y = from.y - to.y;
		return Math.abs(x) <= 1 && Math.abs(y) <= 1;		
	}
	
	// Given a time, get all tiles the person can see!
	public ArrayList<Record> sight_t(ArrayList<ArrayList<Record>> record, int t)
	{
		ArrayList<Record> answer = new ArrayList<Record>();
		for (int r = 0; r < SIZE; r++)
		{
			ArrayList<Record> row = record.get(r);
			for (int column = 0; column < SIZE; column++)
			{
				Record p = row.get(column);
				ArrayList<Observation> obs= p.getObservations();
				for (Observation o: obs)
				{
					if (o.getT() == t)
					{
						answer.add(p);
					}
				}
			}
		}
		return answer;
	}
	// Get the Composition of Map (Percentage of Clear, Mud and Water). Use for Report to compare Maps!
	// Essentially re-use Map generator code
	public void print_map_composition(String PATH)
	{
		int muddy = 0;
		int clean = 0;
		int water = 0;
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(PATH));
			String line = br.readLine();
			while (line != null) 
			{
				for (int j = 0; j < line.length(); ++j) 
				{
					switch (line.charAt(j)) 
					{
						case 'n': 
							++clean;
							break;
						case 'm':
							++muddy;
							//muddyCells.add(new Point(j, i)); 
							break;
						case 'w': 
							++water;
							//waterCells.add(new Point(j, i)); 
							break;
						/*
						case 'p': 
							packageCell = new Point(j, i); 
							break;
						case 't': 
							targetCell = new Point(j, i); 
							break;
						*/
						default: 
							throw new IOException("Invalid map token");
					}
				}
				line = br.readLine();
			}
			br.close();
			
			// Print Percentages!
			System.out.println("Muddy: " + (double) muddy/(muddy + clean  + water) + "%, "
					+ "Clean: " + (double) clean/(muddy + clean + water) + " %, "
					+ "Water: " + (double) water/(muddy + clean + water)  + "%");
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
//===================================METHODS BELOW WORK BUT ARE NOT USED==================================================================	
	private ArrayList<Point> all_valid_moves(Point current)
	{
		ArrayList<Point> valid = new ArrayList<Point>();
		/*
		 * All moves are: X is current
		 * (-1,  1) , (0, 1)	, (1, 1)
		 * (-1,  0) , X			, (1, 0)
		 * (-1, -1) , (0. -1)	, (1, -1)
		 * 
		 * The for loops should force the absolute value constraint easily.
		 */

		for (int x = -1; x < 2; x++)
		{
			for(int y = -1; y < 2; y++)
			{
				if(valid_step(current, x, y))
				{
					valid.add(new Point(x, y));
				}
			}
		}
		// Probably should remove the point 0, 0?
		valid.remove(new Point(0, 0));
		return valid;
	}

	private boolean valid_step(Point loc, int x, int y)
	{
		if (loc.x + x <= SIZE - 1 && loc.x + x >= 0 && loc.y + y <= SIZE - 1 && loc.y + y >= 0)
		{
			// Check if in water using the truth_table
			Record r = truth_table.get(loc.x + x).get(loc.y + y);
			// Can't be water here since no entry exists!
			if(r == null)
			{
				return true;
			}
			else
			{
				// It is a water tile! Not Valid!
				if(r.getC() == 2)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		else
		{
			return false;
		}
	}
}