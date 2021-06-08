
import java.util.*; 









class Task {

    public int id, time, staff;
    String name;
    int earliestStart, latestStart;
    List <Task> inEdges = new ArrayList<Task>();  
    List <Task> outEdges = new ArrayList<Task>();  
    int cntPredecessors;
    int explored = 0;            // flagg for om denne har blitt sett på av en søke-algoritme
    int topoNumber = 0;
    int started = 0;
    int startedTime = 0;
    int finished = 0;
    int shortestPath = 100;
    int longestPath = 0;
    int earliestFinish = 0;
    int critical = 0;
    int slack = 0;

    public Task (int i) {
       
        id = i; 

    }

    public void addInEdge(Task t) {

        // legg til kant
        inEdges.add(t);
    }

    public void addOutEdge(Task t) {

        // legg til kant
        outEdges.add(t);
    }
} 