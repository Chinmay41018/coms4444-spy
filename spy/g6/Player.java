package spy.g6;

import java.util.List;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

public class Player implements spy.sim.Player {
    
    private ArrayList<ArrayList<Record>> observations;
    private int id;
    private Point loc;
    private HashMap<Integer,ArrayList<Record>> recordsToldBy;
    private HashMap<Point,ArrayList<Record>> pointsToldBy;
    private List<Point> waterCells;
    private List<Point> path;
    private Boolean pathFound;
    private Boolean packageFound;
    private Boolean targetFound;
    
    private static final int EVANS_CONVENTION = 15;
    
    // Keeping track of when was a specific player last seen by us.
    private int[] lastPlayerSeen;
    
    // Ongoing tasks for movements
    private LinkedList<MovementTask> tasks;
    
    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.observations = new ArrayList<ArrayList<Record>>(100);
        this.recordsToldBy = new HashMap<>();
        this.pointsToldBy = new HashMap<>();
        this.waterCells = waterCells;
        this.path = new ArrayList<Point>();
        this.pathFound = false;
        this.packageFound = false;
        this.targetFound = false;
        
        for (int i = 0; i < 100; i++){
        	ArrayList<Record> row = new ArrayList<Record>(100);
            for (int j = 0; j < 100; j++){
                row.add(null);
            }
            observations.add(row);
        }
        
        lastPlayerSeen = new int[n];
        tasks = new LinkedList<MovementTask>();
        tasks.add(new BasicMovement());
        
        // System.out.println(this.records);
    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        int time = Simulator.getElapsedT();
      
        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();
            
            Record record = new Record(p, status.getC(), status.getPT(), new ArrayList<Observation>());
            record.getObservations().add(new Observation(this.id, time));
            observations.get(p.x).set(p.y, record);
            
            for (Integer player: status.getPresentSoldiers()) {
            	if (lastPlayerSeen[player] + EVANS_CONVENTION < time) {
            		tasks.add(new MeetPlayer(player, observations));
            		//tasks.addFirst(new MeetPlayer(player));
            		lastPlayerSeen[player] = time;
            	}
            }
            /*
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
                
                // Find nearby players

            }*/

        }
        // System.out.println(records);
    }
    
    public List<Record> sendRecords(int id)
    {
        ArrayList<Record> toSend = new ArrayList<Record>();
        for (ArrayList<Record> row : observations)
        	for (Record record : row)
        		if (record != null){
        			toSend.add(record);
        		}
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> records){
        ArrayList<Record> receivedRecs = new ArrayList<Record>();

        for (Record record : records){
            //only add the record if not null
            if (record != null){
                receivedRecs.add(record);
                Point p = record.getLoc();
                //keep track of all the points
                ArrayList<Record> list = pointsToldBy.getOrDefault(p, new ArrayList<Record>());
                list.add(record);
                pointsToldBy.putIfAbsent(p, list);
            }
        }

        //keep track of records told by a specific player
        if (recordsToldBy.containsKey(id)){
            // concatenate receivedRecs
            recordsToldBy.get(id).addAll(receivedRecs);
        }else{
            recordsToldBy.put(id,receivedRecs);

        }

        lastPlayerSeen[id] = Simulator.getElapsedT();
    }
    
    public List<Point> proposePath()
    {


        if (targetFound && packageFound){

            if (pathFound){

                return path;
            } 
            else{
                //find shortest path
                //bfs with all weights as 1
                //PathFinder.search()
                return null;
            }
        }

        else{
            return null;
        }
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            // return entry.getKey();
            return new ArrayList<Integer>(entry.getKey());
        }
        return null;
    }
    
    // Recieves the results (in the event that no path succeeds).
    public void receiveResults(HashMap<Integer, Integer> results){

        //analyze why we did not win
        //was a the spys doing or a bug in someones code
        for (Map.Entry<Integer, Integer> result : results.entrySet())
        {

            ArrayList<Integer> resultKeys = new ArrayList<Integer>();
            resultKeys.add(result.getKey());
            // result.getKey()

        }

        
    }
    
    public Point getMove(){
    	while (tasks.peek().isCompleted()) {
    		tasks.removeFirst();
    	}
        return tasks.peek().nextMove();
    }
    
    public class BasicMovement extends MovementTask {
    	private double moveFactor = 1.0D;
    	private double exploreFactor = 1.0D;
    	private double verifyFactor = 0.0D;
    	private double proximityFactor = 0.00D; // TODO
    	
    	private Point lastMove = new Point(0, 0);
    	
    	@Override
    	public boolean isCompleted() {
    		return false;
    	}
    	
    	protected double getScore(int deltax, int deltay) {
    		if (waterCells.contains(new Point(loc.x + deltax, loc.y + deltay)))
    			return -1.0D;
            //return negative score for location outside the map
            if (loc.x + deltax < 0 || loc.x + deltax > 99 || loc.y + deltay < 0 || loc.y + deltay > 99 )
                return -1.0D;
    		if (deltax == 0 && deltay == 0)
    			return -1.0D;

    		HashSet<Point> points = new HashSet<Point>();
    		if (deltax != 0) {
    			points.add(new Point(loc.x + 3 * deltax, loc.y - 1));
    			points.add(new Point(loc.x + 3 * deltax, loc.y + 1));
    			points.add(new Point(loc.x + 4 * deltax, loc.y + deltay));
    			points.add(new Point(loc.x + 3 * deltax, loc.y - 2 + deltay));
    			points.add(new Point(loc.x + 3 * deltax, loc.y + 2 + deltay));
    		}
    		else {
    			points.add(new Point(loc.x + 3, loc.y + deltay));
    			points.add(new Point(loc.x - 3, loc.y + deltay));
    		}
    		
    		if (deltay != 0) {
    			points.add(new Point(loc.x - 1, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + 1, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + deltax, loc.y + 4 * deltay));
    			points.add(new Point(loc.x - 2 + deltax, loc.y + 3 * deltay));
    			points.add(new Point(loc.x + 2 + deltax, loc.y + 3 * deltay));
    		}
    		else {
    			points.add(new Point(loc.x + deltax, loc.y + 3));
    			points.add(new Point(loc.x + deltax, loc.y - 3));
    		}
    		
    		int exploreCount = 0, verifyCount = 0;
    		for (Point p : points) {
    			try {
    				if (!waterCells.contains(p) && observations.get(p.x).get(p.y) == null) {
    					if (pointsToldBy.containsKey(new Point(p.x, p.y)))
    						++verifyCount;
    					else
    						exploreCount += 2;
    				}
                    else{
                        // --exploreCount;
                    }
    			}
    			catch(Exception e) {
    				continue;
    			}
    		}
    		
    		int cost = (Math.abs(deltax) + Math.abs(deltay) == 2) ? 3 : 2;
    		if (observations.get(loc.x).get(loc.y).getC() != 1 &&
    				observations.get(loc.x + deltax).get(loc.y + deltay).getC() == 1)
    			cost *= 2;
    		// return (exploreFactor * exploreCount + verifyFactor * verifyCount) / cost;
            return (exploreFactor * exploreCount ) / cost;
    	}
    	
    	@Override
    	public Point nextMove(){
    		int bestMoveX = 0, bestMoveY = 0;
    		double bestScore = -1.0D;
			for (int deltax = -1; deltax <= 1; ++deltax)
				for (int deltay = -1; deltay <= 1; ++deltay) {
					double localScore = getScore(deltax, deltay);
					// TODO Temporary hack for not turning back
					if ((deltax + lastMove.x == 0) && (deltay + lastMove.y == 0)){
						localScore -= 0.5D;
					}
					if (localScore > bestScore) {
						bestMoveX = deltax;
						bestMoveY = deltay;
						bestScore = localScore;
					}
				}
			lastMove = new Point(bestMoveX, bestMoveY);
			return new Point(bestMoveX, bestMoveY);
    	}
    }

}
