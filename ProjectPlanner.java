import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import javax.sql.rowset.serial.SerialArray;
// import jdk.nashorn.internal.runtime.regexp.joni.constants.AnchorType;

class ProjectPlanner {

    static Task[] tasks = null;                                         // array for oppgavene som leses inn fra fil
    static String filename = null;                                        // filnavn som skal leses inn
    static List<Task> orderedTasks = new ArrayList<Task>();             // liste for de tasks som blir topologisk sortert
    static int[] shortestList = null;
    static List<Task> criticalList = new ArrayList<Task>();
    static int tempteller = 0;
    static int sykelFunnet = 0;                                         // holde orden på om en sykel er funnet
    static Task maxNode = null;
    static int maxVarighet = 0;

    public static void main(String[] args) throws IOException {

        // lagre filnavn fra command line-argument
        filename = args[0];
        // filename = "buildhouse1.txt";
        // opprett oppgaver fra fil og plassere dem i listen tasks
        opprettOppgaverFraFil();

        // sjekke først om prosjektet er mulig å gjennomføre, altså om det ikke har loops
        // hvis ikke, printe en funnet loop og avslutte
        letEtterSykel();

        // hvis det ikke er noen sykel i prosjektet
        if (sykelFunnet == 0) {

            System.out.println("Fant ingen sykler");

            // gjøre en topologisk sortering, altså lage rekkefølge etter hvilke noder som er avhengige av andre noder, for å være klar til å lage paths
            orderedTasks = topologiskSortering();

            /*
            // skrive ut orden:
            System.out.println("\nTopologisk rekkefølge:");
            for (int i = 0; i < orderedTasks.size(); i++) {

                System.out.println("    " + orderedTasks.get(i).name);
            }
            */

            // regne ut hvilke stier som er lengst, altså ikke kan forsinkes
            findLongestPaths();

            // finne DEN noden av lengste stier som tar lengst tid å fullføre, som er starten på en utregning av critical
            findLongestPath();

            // legg maxnode til liste
            criticalList.add(maxNode);
            // markere som critical
            maxNode.critical = 1;
            // finn flere critical
            findCriticalPath(maxNode);

            /*
            // skrive ut critical
            System.out.println("\nCritical tasks:");

            for (int i = criticalList.size() - 1; i >= 0; i--) {

                System.out.println("    " + criticalList.get(i).name);
            }
            */

            // finne slack for alle oppgavene
            findSlack();

            // skrive ut prosessen til konsoll
            listeOverOppgavene();
            buildProject();
        }
    }


    // kjøre slackrecurse på alle løvnoder
    private static void findSlack() {


        // for alle tasks
        for (int i = orderedTasks.size() - 1; i >= 0; i--) {

            // hvis denne ikke har noen etterkommere, trenger vi bare å sammenlikne med siste tid for treet
            if (orderedTasks.get(i).outEdges.size() == 0) {

                // slack for denne er da totaltid minus tidligste sluttid for denne node
                orderedTasks.get(i).slack = maxVarighet - orderedTasks.get(i).earliestFinish;  

                // og så beregne slack fra denne løvnoden
                slackRecurse(orderedTasks.get(i));
            }
        }

        /*

        // skrive ut slack

        System.out.println("\nSlack:");

        for (int i = 0; i < orderedTasks.size(); i++) {

            if (orderedTasks.get(i).slack > 0) {
                System.out.println("    Oppgaven " + orderedTasks.get(i).name + " har slack " + orderedTasks.get(i).slack);
            }
        }
        */
    }

    private static void slackRecurse(Task t) {
        
        // for alle in-nodene
        for (int i = 0; i < t.inEdges.size(); i++) {

            // hvis ikke critical
            if (t.inEdges.get(i).critical == 0) {

                // hvis denne slack er 0
                if (t.inEdges.get(i).slack == 0) {  

                    // sette slack
                    t.inEdges.get(i).slack = (t.longestPath + t.slack) - (t.inEdges.get(i).longestPath + t.inEdges.get(i).time);
                }

                // hvis denne slack ikke er 0, hvis den da er større
                else if (t.inEdges.get(i).slack >= (t.longestPath + t.slack) - (t.inEdges.get(i).longestPath + t.inEdges.get(i).time)) { 

                    // sette slack
                    t.inEdges.get(i).slack = (t.longestPath + t.slack) - (t.inEdges.get(i).longestPath + t.inEdges.get(i).time);
                }    
            }

            // rekursere videre bakover
            slackRecurse( t.inEdges.get(i));
        }
    }

    // lage longest path ?deep first? search
    private static void findLongestPaths() {

        // node 0 er 0 fordi den har ikke noe tidligere sti
        orderedTasks.get(0).longestPath = 0;

        // for hver node
        for (int i = 0; i < orderedTasks.size(); i++) {

            Task currentTask = orderedTasks.get(i);

            // for hver kant j i node i
            for (int j = 0; j < orderedTasks.get(i).inEdges.size(); j++) {

                Task currentInEdge = orderedTasks.get(i).inEdges.get(j);

                // hvis korteste sti fra nåværende node pluss tiden til kantnoden vi ser på er mindre enn kantnodens nåværende minste tid
                if (currentInEdge.longestPath + currentInEdge.time > currentTask.longestPath) {
                   
                    // sett noden til ny longest path
                    currentTask.longestPath = currentInEdge.longestPath + currentInEdge.time;
                }
            }
            
            // sett nodens earliest end til longest path + tid
            currentTask.earliestFinish = currentTask.longestPath + currentTask.time;  
        }
    }

    // findLongestSubTree
    // rekursivt bakover fra den noden med størst end time,
    // sjekke hvilken av indegrees som har størst start time, og ta den med i en liste, og markere som critical
    // og så videre, helt til man kommer til roten.
    // så har vi en god liste
    private static void findLongestPath() {

        // for alle tasks
        for (int i = 0; i < orderedTasks.size(); i++) {
            
            if ((orderedTasks.get(i).longestPath + orderedTasks.get(i).time) > maxVarighet) {
                maxVarighet = orderedTasks.get(i).longestPath + orderedTasks.get(i).time;
                maxNode = orderedTasks.get(i);
            }
        }
    }

    private static void findCriticalPath(Task t) {

        int maxInEdge = 0;
        Task maxNode = null;

        // hvis det finnes in-kanter å sjekke
        if (t.inEdges.size() > 0) {

            // hvor hver in-kant i t
            for (int i = 0; i < t.inEdges.size(); i++) {

                if (t.inEdges.get(i).earliestFinish > maxInEdge) {
                    maxInEdge = t.inEdges.get(i).earliestFinish;
                    maxNode = t.inEdges.get(i);
                }
            }
            
            t = maxNode;

            // legg kanten til liste
            criticalList.add(t);
            t.critical = 1;
            findCriticalPath(t);
        }
    }

    // metode for å finne sykel
    static void letEtterSykel() {

        // først ta for seg alle nodene og kjøre dfs på dem for å jobbe seg gjennom en sti
        // grunnen til at man ikke kjører dfs på bare første node,
        // er at det kan være flere noder uten forgjengere, og de vil bare plukkes opp av dfs hvis de er startpunktet
        for (int i = 0; i < tasks.length; i++) {

            // hvis noden [i] som vi ser på nå er unexplored
            if (tasks[i].explored == 0) {

                // gjøre dfs på den for å lete etter sykler
                finnAlleredeBesokt(tasks[i]);
            }
        }
    }
    
    // metode for å rekursivt søke gjennom en graf 
    // med mål å sjekke om det er sykel
    static void finnAlleredeBesokt(Task n) {

        // markere at denne noden har blitt sett på, for det har den jo nå
        // dette brukes for at man skal ikke lage sti til en sti som allerede finnes
        n.explored = 1;

        // for alle kantene i nåværende node 
        for (int i = 0; i < n.outEdges.size(); i++) {

            // sett at kanten ikke allerede er besøkt
            if (n.outEdges.get(i).explored == 0) {

                // søk videre utover
                finnAlleredeBesokt(n.outEdges.get(i));
            }

            // hvis kanten allerede er besøkt
            else {

                // hva med: rekursivt for alle indegrees, hvis n.outEdges.get(i) er i indegrees
                skrivUtSykel(n, n.outEdges.get(i), n.name);
            }
        }
    }

    // metode for å gå tilbake for å se om man treffer å seg selv igjen
    // argumenter nåværende node, den noden man prøver å treffe, streng liste over noder man har gått tilbake så langt
    static void skrivUtSykel(Task t, Task u, String s) {

        while (tempteller < tasks.length && sykelFunnet == 0) {

            tempteller++;

            // legg navnet på noden til listen over noder i en potensiell sykel

            // for alle indegrees av nåværende node
            for (int i = 0; i < t.inEdges.size(); i++) {

                // hvis denne indegree er samme som noden vi ser etter
                if (t.inEdges.get(i).id == u.id) {

                    // legg navnet på noden til listen over noder i en potensiell sykel
                    s += " " + t.inEdges.get(i).name;

                    // si at vi fant en sykel
                    System.out.println("Fant følgende sykel:");

                    // strengen er baklengs. for å kunne sette den rett vei, splitte på navn
                    String[] sykelListe = s.split(" ");

                    // for hvert navn, fra bak til fremme
                    for (i = sykelListe.length - 1; i >= 0; i--) {

                        // skriv ut navn
                        System.out.println("    " + sykelListe[i]);
                    }

                    // marker at loop er funnet
                    sykelFunnet = 1;

                    // gå ut av loopen
                    break;
                }

                // hvis noden ikke er u
                else {

                    // prøv å gå enda lenger tilbake, og legg navnet på noden til listen over noder i en potensiell sykel
                    skrivUtSykel(t.inEdges.get(i), u, s + " " + t.inEdges.get(i).name);
                }
            }
        }
    }

    private static void listeOverOppgavene() {

        // liste over nodene
        // for alle nodene
        System.out.println();              
        System.out.println("Liste over oppgavene:");
        System.out.println();

        for (int i = 0; i < tasks.length; i++) {

            System.out.println("    ID:             " + tasks[i].id);
            System.out.println("    navn:           " + tasks[i].name);
            System.out.println("    tid:            " + tasks[i].time);
            System.out.println("    folk:           " + tasks[i].staff);
            System.out.println("    tidligst start: " + tasks[i].longestPath);
            System.out.println("    slack:          " + tasks[i].slack);
            System.out.println("    oppgaver som avhenger av denne oppgaven:");

            // hvis det ikke er noen ut-kanter
            if (tasks[i].outEdges.size() == 0) {
                System.out.println("        ingen");
            }
              
            else {
                for (int j = 0; j < tasks[i].outEdges.size(); j++) {
                    System.out.println("        " + tasks[i].outEdges.get(j).id);
                }
            }

            System.out.println();
        }
    }

    // skrive ut prosessen til konsoll
    private static void buildProject() {

        int tid = 0;                    // teller for hvor man er i tiden
        int printFlag = 0;              // om det er noe å skrive ut eller ikke
        ArrayList<String> utskrift;     // samle utskriftene for nåværende tidspunkt
        int aktiveArbeidere = 0;        // teller for hvor mange arbeidere som er aktive nå

        // så lenge prosjektet enda ikke er ferdig
        while (tid <= maxVarighet) {

            printFlag = 0;                              // markør for om noen har bedt om å få skrive ut noe på denne tiden
            utskrift = new ArrayList<String>();         // liste over elementer som skal skrives ut

            // for alle nodene
            for (int i = 0; i < orderedTasks.size(); i++) {

                Task currentTask = orderedTasks.get(i);

                // hvis det er kommet til denne oppgavens starttid
                if (currentTask.longestPath + currentTask.slack == tid) {
                    utskrift.add("    Starter oppgave " + currentTask.name);
                    printFlag = 1;
                    aktiveArbeidere += currentTask.staff;
                }

                // hvis denne oppgaven er ferdig
                if (currentTask.earliestFinish + currentTask.slack == tid) {
                    utskrift.add("    Oppgave " + currentTask.name + " er ferdig");

                    // markere at noe skal skrives ut for dette tidspunktet
                    printFlag = 1;

                    // trekke fra arbeidere som er ferdige
                    aktiveArbeidere -= currentTask.staff;
                }
            }

            // hvis det er noen som har bedt om å få skrive ut noe på denne tiden
            if (printFlag == 1) {

                utskrift.add(0, "Tid: " + tid);

                // hvis alle arbeiderne er ferdige, legg det til utskrift
                if (aktiveArbeidere == 0) {utskrift.add("    Alle arbeiderne er ferdige");}

                // hvis arbeidere fortsatt er i gang, legg til utskrift hvor mange
                else {utskrift.add("    Det er " + aktiveArbeidere + " arbeidere i gang");}

                // skriv ut alle elementene u som ligger til utskrift i listen utskrift
                for (String u : utskrift) {System.out.println(u);}
            }

            // øk tiden foran neste loop
            tid++;
        }
    }
   
    // topologisk sortering
    private static List<Task> topologiskSortering() {

        // stack for mellomlagring
        Stack<Task> stack = new Stack<Task>();  

        // output-liste
        List<Task> outTasks = new ArrayList<Task>();    
        
        // søke gjennom alle tasks for en node med 0 predecessors, med andre ord indegrees eller avhengigheter, å sette dem i stacken
        for (int i = 0; i < tasks.length; i++) {

            // hvis en task ikke har in-grader, gitt i feltet cntPredecessors, sett den på stacken
            if (tasks[i].cntPredecessors == 0) {stack.push(tasks[i]);}
        }

        // teller for hvor mange kanter man har funnet? eller hva gjøres egentlig her?
        int i = 1;

        // as long as there are still elements in the stack uten dependencies
        while (!stack.empty()) {

            // hent en task fra stacken
            Task currentTask = stack.pop();

            // lagre den i output-listen
            outTasks.add(currentTask);
            
            // gi denne tasken en topologisk rekkefølge-nummer i
            currentTask.topoNumber = i;

            // inkrementer i fordi man har sett på en til kant?
            i++;

            // for hver kant i i
            for (int j = 0; j < currentTask.outEdges.size(); j++) {

                // sett nåværende kant
                Task currentoutEdgeTask = currentTask.outEdges.get(j);

                // trekk 1 fra cntPre fordi vi har sett på denne pre
                currentoutEdgeTask.cntPredecessors--;

                // hvis det ikke er flere ubehandlede, sett på stacken
                if (currentoutEdgeTask.cntPredecessors == 0) {stack.push(currentoutEdgeTask);}
            }
        }

        // her sjekkes om hvis det er færre kanter enn noder (og alle noder er sammenkoblet) så er det ingen sykel
        // i er tallet på hvor mange kanter man har sett på?
        if (i > tasks.length) {return outTasks;}
            
        // hvis det er en sykel, returner 0
        return null;
    }

    // les fil inn
    private static void opprettOppgaverFraFil() {

        // fange input-feil
        try {

            // for å lese linjer fra filen
            Scanner in = new Scanner(new File(filename));

            // lese antall oppgaver først
            int n = in.nextInt();

            // array av tasks som skal lages fra hver linje i filen
            tasks = new Task[n];

            // for hver linje i filen, fra første til siste linje
            for (int i = 0; i < n; i++) {

                // lag en ny task med inkrementerende id (starter på 1)
                tasks[i] = new Task(i + 1);
            }

            // for hver linje i filen
            for (int i = 0; i < n; i++) {

                int id = in.nextInt();                  // les id
                Task task = tasks[id - 1];              // sett nåværende task til nåværende lest id (-1 fordi array begynner fra 0)
                task.name = in.next();                  // sett nåværende tasks navn til lest navn
                task.time = in.nextInt();               // sett nåværende tasks tid til lest tid    
                task.staff = in.nextInt();              // sett nåværende tasks staff til lest staff

                // for noen oppgaver finnes det flere argumenter som er dependencies gitt i form av id til annen task
                // inkommende kant fra en annen task, altså
                // les inn alle disse i en while-loop
                while (true) {

                    int dep = in.nextInt();             // les dependencies fra linjen 
                    if (dep == 0) {                     // hvis dette er 0
                        break;                          // ikke gå videre
                    }

                    task.addInEdge(tasks[dep - 1]);     // legg til inedge i denne tasken som er dep
                    tasks[id - 1].cntPredecessors++;    // øk nåværende tasks teller for antall forgjengere
                    tasks[dep - 1].addOutEdge(task);    // sett nåværende task til kant i task gitt i dep
                }
            }
        }

        catch (Exception e) {
        }
    }
}