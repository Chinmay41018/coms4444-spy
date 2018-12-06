package spy.g6;

import java.util.ArrayList;
import java.util.LinkedList;

import spy.sim.Point;
import spy.sim.Record;

public class MeetPlayer extends MovementTask {

	private int player;
	private ArrayList<ArrayList<Record>> playerRecords;

	public MeetPlayer(int player, ArrayList<ArrayList<Record>> playerRecords) {
        this.player = player;
        this.playerRecords = playerRecords;
    }
	@Override
	public boolean isCompleted() {
		// TODO
		return false;
	}
	
	public MeetPlayer(int player) {
	}
}
