package spy.g8;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import spy.sim.Point;
import spy.sim.Record;
import spy.sim.CellStatus;
import spy.sim.Simulator;
import spy.sim.Observation;

import java.lang.Math;
import java.util.Comparator;

public class Player implements spy.sim.Player {
    
    private ArrayList<ArrayList<Record>> records;
    private int id;
    public Point loc;
    private List<Point> waterCells;
    private List<Point> clearCells;
    private List<Point> observed;
    private List<Point> notobserved;
    private Point destination;
    private Point move;
    private Map<Integer,Integer> waitTime;
    private Map<Integer,Point> seeSoldiers; //list of soldiers we see at a certain time
    private Map<Point,Point> bfs;
    private int wait = 24;
    private boolean meeting = false;
    private List<Integer> meetSoldiers;
    private boolean trymeeting = false;
    private int trySoldier;
    private Point dest = null;
    private Point pack = null;
    private HashMap<Integer, List<Record>> receivedRecords;
    private Random rand;

    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        this.id = id;
        this.loc = startingPos;
        this.destination = startingPos;
        this.records = new ArrayList<ArrayList<Record>>();
        this.waterCells = waterCells;
        this.observed = new ArrayList<Point>();
        this.notobserved = new ArrayList<Point>();
        this.move = new Point(0,0);
        this.seeSoldiers = new HashMap<Integer,Point>();
        this.receivedRecords = new HashMap<Integer, List<Record>>();
        this.meetSoldiers = new ArrayList<Integer>();
        this.trySoldier = -1;
        this.clearCells = new ArrayList<Point>();
        this.rand = new Random();


        this.waitTime = new HashMap<Integer,Integer>();
        for(int i=0;i<n;i++) {
            if (i != id) {
                waitTime.put(i,wait);
            }
        }

        //System.out.println("watersize:"+waterCells.size());
        for (int i = 0; i < 100; i++)
        {
            ArrayList<Record> row = new ArrayList<Record>();
            for (int j = 0; j < 100; j++)
            {
                Point cell = new Point(i,j);
                if (!waterCells.contains(cell)) {
                    notobserved.add(new Point(i,j));
                }
                row.add(null);
            }
            this.records.add(row);
        }
        //System.out.println("INIT"+records.get(loc.x).get(loc.y));
        //System.out.println("CHECK" + notobserved.size());
    }

    private double distance(Point p1, Point p2) 
    {
        return Math.sqrt(Math.pow(p1.x-p2.x,2)+Math.pow(p1.y-p2.y,2));
    }

    public Comparator<Point> pointComparator = new Comparator<Point>() 
    {   
            public int compare(Point p1, Point p2)  
            {
                double d1 = distance(loc, p1);
                double d2 = distance(loc, p2);
                if (d1 < d2) 
                {
                    return -1;
                } 
                else if (d1 > d2) 
                {
                    return 1;
                } 
                else 
                {
                    return 0;
                }
            }
    };


    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        this.loc = loc;
        seeSoldiers = new HashMap<Integer,Point>();
        meetSoldiers = new ArrayList<Integer>();

        for (Map.Entry<Point, CellStatus> entry : statuses.entrySet())
        {
            Point p = entry.getKey();
            CellStatus status = entry.getValue();

            List<Integer> see = status.getPresentSoldiers();
            see.remove(Integer.valueOf(this.id));
            // look at case when we observe the cell we are occupying
            if(see.size() > 0 && p.equals(loc)) { //do not count when it is ourselves
                //System.out.println("AT SAME LOCATION");
                for (int i =0;i<see.size();i++) {
                    if (waitTime.get(see.get(i)) == 0) {
                        meeting = true;
                    }
                }
                for(int i=0;i<see.size();i++) {
                   // if(see.get(i) != this.id) {
                    meetSoldiers.add(see.get(i));//which soldier(s) we are meeting
                    //}
                }
            }/* else {
                //meetSoldiers = new ArrayList<Integer>();
                /meeting = false;
            }*/

            for(int i=0;i<see.size();i++) {
                if(see.get(i) != this.id) {
                    seeSoldiers.put(see.get(i),p);
                    //System.out.println("debug");
                    System.out.println("Player " + this.id + " and Player " + see.get(i) + " waitTime left: "+waitTime.get(see.get(i)));
                }
            }

            // record directly observed clear cells
            if (status.getC()!=1 && !waterCells.contains(p)) clearCells.add(p);
            if (status.getPT()==1) pack = p;
            if (status.getPT()!=0 && status.getPT()!=1) dest = p;

            Record record = records.get(p.x).get(p.y);
            if (record == null || record.getC() != status.getC() || record.getPT() != status.getPT())
            {
                ArrayList<Observation> observations = new ArrayList<Observation>();
                record = new Record(p, status.getC(), status.getPT(), observations);
                records.get(p.x).set(p.y, record);
            }
            record.getObservations().add(new Observation(this.id, Simulator.getElapsedT()));
        }
    }
    
    public List<Record> sendRecords(int id)
    {
        ArrayList<Record> toSend = new ArrayList<Record>();
        for (ArrayList<Record> row : records)
        {
            for (Record record : row)
            {
                if (record != null)
                {
                    toSend.add(record);
                }
            }
        }
        return toSend;
    }
    
    public void receiveRecords(int id, List<Record> records)
    {
        receivedRecords.put(id, records);
        updateRecords();
    }

    public void updateRecords(){
        for(int id:receivedRecords.keySet()){
            List<Record> rec = receivedRecords.get(id);
            for (Record r: rec){
                if(!waterCells.contains(r.getLoc()) && r.getC() != 1 && !observed.contains(r.getLoc())){

                    clearCells.add(r.getLoc());
                    keepUnique(clearCells);

                    // trust the other agent
                    observed.add(r.getLoc());
                    keepUnique(observed);

                    notobserved.remove(r.getLoc());
                    keepUnique(notobserved);
                }
                if (dest == null && r.getPT() != 0 && r.getPT() != 1){
                    dest = r.getLoc();
                }
                if (pack == null && r.getPT() == 1){
                    pack = r.getLoc();
                }
            }
        }
    }

    public void keepUnique(List<Point> temp){
        Set<Point> hs = new HashSet<>();
        hs.addAll(temp);
        temp.clear();
        temp.addAll(hs);
    }
    
    public List<Point> proposePath()
    {
        if (pack == null || dest == null) return null;
 
        return BFS(pack,dest);
    }

    public List<Point> BFS(Point source, Point target){
        bfs = new HashMap<Point,Point>();
        List<Point> nextPoints = new ArrayList<>();
        List<Point> settledPoints = new ArrayList<>();
        nextPoints.add(source);
        Point currentPoint;
        boolean found = false;
        while (nextPoints.size() > 0 && !found){
            currentPoint = nextPoints.remove(0);
            settledPoints.add(currentPoint);
            //System.out.println("size of list is "+nextPoints.size()+", currentPoint is "+currentPoint.x+","+currentPoint.y);
            Point[] ps = {new Point(currentPoint.x+1,currentPoint.y),
                          new Point(currentPoint.x,currentPoint.y+1),
                          new Point(currentPoint.x,currentPoint.y-1),   
                          new Point(currentPoint.x-1,currentPoint.y),            
                          new Point(currentPoint.x+1,currentPoint.y+1),
                          new Point(currentPoint.x+1,currentPoint.y-1),
                          new Point(currentPoint.x-1,currentPoint.y+1),
                          new Point(currentPoint.x-1,currentPoint.y-1)};
            for (Point p : ps){
                if (isValidPoint(p)&&!settledPoints.contains(p)&&!nextPoints.contains(p)&&clearCells.contains(p)){
                    if (!bfs.containsKey(p)){
                        bfs.put(p,currentPoint);
                    }
                    nextPoints.add(p);
                    if (p.equals(target)){
                        found = true;
                        break;
                    }
                }
            }         
        }
        List<Point> toReturn = new ArrayList<>();
        currentPoint = target;
        toReturn.add(target);
        int count = 0;
        while (!currentPoint.equals(source)){
            if (count >= 2*bfs.size()){
                return null;
            }
            Point temp = bfs.get(currentPoint);
            toReturn.add(0,temp);
            currentPoint = temp;
            count +=1;
        }
        return toReturn;

    } 


    public List<Point> BFS_Naive(Point source, Point target){
        bfs = new HashMap<Point,Point>();
        List<Point> nextPoints = new ArrayList<>();
        List<Point> settledPoints = new ArrayList<>();
        nextPoints.add(source);
        Point currentPoint;
        boolean found = false;
        while (nextPoints.size() > 0 && !found){
            currentPoint = nextPoints.remove(0);
            settledPoints.add(currentPoint);
            //System.out.println("size of list is "+nextPoints.size()+", currentPoint is "+currentPoint.x+","+currentPoint.y);
            Point[] ps = {new Point(currentPoint.x+1,currentPoint.y),
                          new Point(currentPoint.x,currentPoint.y+1),
                          new Point(currentPoint.x,currentPoint.y-1),   
                          new Point(currentPoint.x-1,currentPoint.y),            
                          new Point(currentPoint.x+1,currentPoint.y+1),
                          new Point(currentPoint.x+1,currentPoint.y-1),
                          new Point(currentPoint.x-1,currentPoint.y+1),
                          new Point(currentPoint.x-1,currentPoint.y-1)};
            for (Point p : ps){
                if (isValidPoint(p)&&!settledPoints.contains(p)&&!nextPoints.contains(p)&&!waterCells.contains(p)){
                    if (!bfs.containsKey(p)){
                        bfs.put(p,currentPoint);
                    }
                    nextPoints.add(p);
                    if (p.equals(target)){
                        found = true;
                        break;
                    }
                }
            }         
        }
        List<Point> toReturn = new ArrayList<>();
        currentPoint = target;
        toReturn.add(target);
        int count = 0;
        while (!currentPoint.equals(source)){
            if (count >= 2*bfs.size()){
                return null;
            }
            Point temp = bfs.get(currentPoint);
            toReturn.add(0,temp);
            currentPoint = temp;
            count +=1;
        }
        return toReturn;

    } 

    public boolean isValidPoint(Point p){
        if (p.x < 0 || p.x > 99){
            return false;
        }
        if (p.y < 0 || p.y > 99){
            return false;
        }
        return true;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet())
        {
            ArrayList<Integer> toReturn = new ArrayList<Integer>();
            toReturn.add(entry.getKey());
            //return entry.getKey();
            return toReturn;
        }
        return null;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        
    }
    
    private List<Point> getSurrounding(Point location) {
        //return list of points of surrounding
        List<Point> neighbors = new ArrayList<Point>();
        for (int x=-3;x<=3;x++) {
            if (x == -3 || x == 3) {
                Point temp = new Point(location.x+x, location.y+0);
                if (temp.x >= 0 && temp.x < 100 && temp.y >= 0 && temp.y < 100) {
                    neighbors.add(temp);
                }
            } else if (x == -2 || x == 2 || x == -1 || x == 1) {
                for(int y=-2;y<=2;y++) {
                    Point temp = new Point(location.x+x, location.y+y);
                    if (temp.x >= 0 && temp.x < 100 && temp.y >= 0 && temp.y < 100) {
                        neighbors.add(temp);
                    }
                }
            } else { //x == 0
                for(int y=-3;y<=3;y++) {
                    Point temp = new Point(location.x+x, location.y+y);
                    if (temp.x >= 0 && temp.x < 100 && temp.y >= 0 && temp.y < 100) {
                        neighbors.add(temp);
                    }
                }
            }
        }
        return neighbors;
    }

    public void whatISee(List<Point> neighbors){

        int min_x = 200;
        int min_y = 200;
        for(Point n:neighbors){
            if(n.x < min_x)
                min_x = n.x;
            if(n.y < min_y)
                min_y = n.y;
        }

        //       o      
        //   o o o o o  
        //   o o o o o  
        // o o o p o o o
        //   o o o o o  
        //   o o o o o  
        //       o      

        // p = Player
        // o = normal cells
        // x = water (death)
        // m = mud
        // S = source
        // T = target

        char[][] vision = new char[7][7];
        for(int i=0; i<7; i++){
            for(int j=0; j<7; j++){
                vision[i][j] = ' ';
            }
        }

        for(Point n:neighbors){
            // default symbol for what we can see
            char symbol = 'o';

            // if we see water
            if(waterCells.contains(n))
                symbol = 'x';

            //System.out.println(records.get(n.x));
            System.out.println(n.x+","+n.y);
            System.out.println(records.get(n.x).get(n.y));
            if(records.get(n.x).get(n.y) != null){
                Record cur_record = records.get(n.x).get(n.y);
                
                if(cur_record.getPT() == 0)
                    symbol = symbol;                   //normal cell
                else if(cur_record.getPT() == 1)
                    symbol = 'S';                   // source
                else
                    symbol = 'T';                   // target

                if(cur_record.getC() == 1)
                    symbol = 'm';

            }

            vision[n.x - min_x][n.y - min_y] = symbol;
        }
        vision[3][3] = 'p';

        for(int i=0; i<7; i++){
            for(int j=0; j<7; j++){
                System.out.print(vision[i][j] + " ");
            }
            System.out.println("");
        }
    }

    public void printRecords(){
        for(ArrayList<Record> row:records){
            for(Record record:row){
                if(record != null){
                    System.out.println(record.toString());
                }
            }
        }
    }

    public Point getMove()
    {
        //System.out.println("GETMOVE"+records.get(loc.x).get(loc.y));

        if (pack != null)
            System.out.println(this.id + " Package Located at: " + pack);
        if (dest != null)
            System.out.println(this.id + " Target Located at: " + dest);

        //System.out.println(this.id + " Unseen cells: " + notobserved.size());


        // once target and packet both found, move to packet
        List<Point> neighbors = getSurrounding(loc);
        //whatISee(neighbors);

        if (pack != null && dest != null){
            if (loc.x==pack.x && loc.y==pack.y){

                // even at package, when both players together, reset waittime to help run simulator fast
                for(int id:waitTime.keySet()){
                    if (waitTime.get(id) == 0)
                        waitTime.put(id, wait);
                }

                return new Point(loc.x + 0, loc.y + 0);
            }

            List<Point> path = BFS(loc,pack);
            if (path.size() > 0){
                //System.out.println(this.id + " found package at: " + pack.x + "," + pack.y + " and target at: " + dest.x + "," + dest.y);
                //System.out.println(path);
                Point next = path.get(1);
                // System.out.println("Currently at: " + loc.x+","+loc.y);
                // System.out.println("Package at: " + pack.x+","+pack.y);
                // System.out.println("Next Move: " + next.x+","+next.y);
                // System.out.println("Is valid point?: " + clearCells.contains(next));
                move = new Point(next.x-loc.x, next.y-loc.y);
                loc = new Point(next.x, next.y);
                return move;
            }
        }

        // DO observation stuff here
        //System.out.println("CURR LOC:"+loc.x + " " + loc.y);
        
        //whatISee(neighbors);
        //printRecords();
        for(Point n:neighbors) {
            // check if any neighbors contain mud, pacakge, target, or any players
            // if other player detected, move towards that player

            if(notobserved.contains(n)) {
                int i = notobserved.indexOf(n);
                notobserved.remove(i);
                observed.add(n);
            }
        }

        for (Map.Entry<Integer, Integer> entry : waitTime.entrySet()) {
            Integer player = entry.getKey();
            Integer time = entry.getValue();
            if (time > 0) {
                time -= 1;
                waitTime.put(player,time);
            }
            //System.out.println("player id: " + player +" , waittime: " + time);
        }

        //we are meeting someone right now
        if (meeting == true) {
            trymeeting = false;
            meeting = false;
            // System.out.println("MEETING MEETING MEETING");
            for (int i=0;i<meetSoldiers.size();i++) {
                //reset waittime
                waitTime.put(meetSoldiers.get(i),wait);
            }
        }

        if (seeSoldiers.size() > 0) { //we observe someone
            // System.out.println("We See Someone!");
            for (Integer i:seeSoldiers.keySet()) { // map of solider id to their location that we see
                //check if their waittime is 0
                if (waitTime.get(i) == 0) {
                    trymeeting = true; //go towards the first one we see
                    trySoldier = i; //location of where we wanna go
                    break;
                }
                else {
                    trymeeting = false;
                    trySoldier = -1; 
                }
            }

        } else {
            trymeeting = false;
            trySoldier = -1;
        }

    
        if (trymeeting == true) { // we already have a target 
            //if (trySoldier >= 0) {
            destination = seeSoldiers.get(trySoldier);

            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;

            //edge case where destination is also package, so other soldier *might not* move towards us
            if (pack != null)
                if (destination.x==pack.x && destination.y==pack.y){
                    List<Point> path = BFS(loc,pack);
                    if (path.size() > 1){
                        Point next = path.get(1);
                        move = new Point(next.x-loc.x, next.y-loc.y);
                        loc = new Point(next.x, next.y);
                        return move;
                    } else {
                        // well i dont think this should ever come
                        System.out.println("WTF HOW DID THIS REACH HERE?!");
                        move = new Point(x, y);
                        loc = new Point(loc.x + move.x, loc.y + move.y);
                        return move;
                    }
                }
            

            

            if (destination.y > loc.y)
                move = new Point(0, 0);
            else if (destination.y==loc.y && destination.x>loc.x)
                move = new Point(0, 0);
            else if (destination.y < loc.y)
                move = new Point(0, -1);
            else if (destination.y==loc.y && destination.x<loc.x)
                move = new Point(-1, 0);
            else
                move = new Point(x, y);
            // else{
            //     List<Point> path = BFS_Naive(loc,destination);
            //     if (path.size() > 1){
            //         Point next = path.get(1);
            //         move = new Point(next.x-loc.x, next.y-loc.y);
            //         loc = new Point(next.x, next.y);
            //         return move;
            //     }
            // }

            //4 cases first
            // if (destination.x == loc.x +1 && destination.y == loc.y) {
            //     move = new Point(0,0);
            // } else if (destination.x == loc.x-1 && destination.y == loc.y ) {
            //     move = new Point(-1,0);
            // } else if (destination.x == loc.x && destination.y == loc.y+1) {
            //     move = new Point(0,0);
            // } else if (destination.x == loc.x && destination.y == loc.y-1) {
            //     move = new Point(0,1);
            // } else if (destination.x == loc.x+1 && destination.y == loc.y-1) {
            //     move = new Point(1,-1);
            // } else if (destination.x == loc.x-1 && destination.y == loc.y+1) {
            //     move = new Point(0,0);
            // } else if (destination.x == loc.x+1 && destination.y == loc.y+1) {
            //     move = new Point(0,0);
            // } else if (destination.x == loc.x-1 && destination.y == loc.y-1) {
            //     move = new Point(-1,-1);
            // } 
            // else if(destination.x > loc.x) { 
            //     move = new Point(1,0);
            // } else if (destination.x < loc.x) {
            //     move = new Point(-1,0);
            // } else {
            //     if (destination.y > loc.y) {
            //         move = new Point(0,1);
            //     } else {
            //         move = new Point(0,-1);
            //     } 
            // }

            loc = new Point(loc.x + move.x, loc.y + move.y);
            if (waterCells.contains(loc)) {
                move = new Point(x, y);
                loc = new Point(loc.x + move.x, loc.y + move.y);
            }
            return move;
            //}
            /*else { //cannot see that soldier anymore
                trymeeting = false;
                trySoldier = -1;
            }*/
        }

        //System.out.println("curr n_obs size: "+notobserved.size());
        if(notobserved.size() > 0) {
            Collections.sort(notobserved, pointComparator);

            int total_unobserved = Math.min(notobserved.size(), 3);

            destination = notobserved.get(rand.nextInt(total_unobserved));

            List<Point> path = BFS_Naive(loc,destination);
            if (path.size() > 1){
                Point next = path.get(1);
                move = new Point(next.x-loc.x, next.y-loc.y);
                loc = new Point(next.x, next.y);
                return move;
            }

            //destination = notobserved.get(0);


            //System.out.println("DEST:"+destination.x + " " + destination.y);
            if(destination.x > loc.x) { 
                move = new Point(1,0);
            } else if (destination.x < loc.x) {
                move = new Point(-1,0);
            } else {
                if (destination.y > loc.y) {
                    move = new Point(0,1);
                } else {
                    move = new Point(0,-1);
                } 
            }
        }
        else { // when we have finished observing
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            move = new Point(x, y);
        }

        loc = new Point(loc.x + move.x, loc.y + move.y);
        if (waterCells.contains(loc)) {
            int x = rand.nextInt(2) * 2 - 1;
            int y = rand.nextInt(2 + Math.abs(x)) * (2 - Math.abs(x)) - 1;
            move = new Point(x, y);
            loc = new Point(loc.x + move.x, loc.y + move.y);
        }

        //System.out.println("NEW LOC:"+loc.x + " " + loc.y);
        return move;

    }
}
