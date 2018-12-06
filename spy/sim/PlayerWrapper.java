package spy.sim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import spy.sim.CellStatus;
import spy.sim.Record;
import spy.sim.Point;
import spy.sim.Player;

public class PlayerWrapper {
    private Timer thread;
    private Player player;
    private String name;

    public PlayerWrapper(Player player, String name) {
        this.player = player;
        this.name = name;
    }

    public void init(int n, int id, int t, Point startingPos, List<Point> waterCells, boolean isSpy)
    {
        player.init(n, id, t, startingPos, waterCells, isSpy);
    }
    
    public void observe(Point loc, HashMap<Point, CellStatus> statuses)
    {
        player.observe(loc, statuses);
    }
    
    public List<Record> sendRecords(int id)
    {
        List<Record> records = player.sendRecords(id);
        return records;
    }
    
    public void receiveRecords(int id, List<Record> records)
    {
        player.receiveRecords(id, records);
    }
    
    public List<Point> proposePath()
    {
        List<Point> path = player.proposePath();
        return path;
    }
    
    public List<Integer> getVotes(HashMap<Integer, List<Point>> paths)
    {
        List<Integer> votes = player.getVotes(paths);
        return votes;
    }
    
    public void receiveResults(HashMap<Integer, Integer> results)
    {
        player.receiveResults(results);
    }
    
    public Point getMove()
    {
        Point p = player.getMove();
        return p;
    }

    public String getName() {
        return name;
    }
}
