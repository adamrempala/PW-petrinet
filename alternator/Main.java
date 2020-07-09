package alternator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;

public class Main {

    private static Transition<String> runTrans(String letter, String another1, String another2) {
        Map<String, Integer> input = new HashMap<>();
        input.put("ready" + letter, 1);
        input.put("mutex", 1);

        Collection<String> zero = new HashSet<>();
        zero.add("forbidden" + another1);
        zero.add("forbidden" + another2);

        Collection<String> inhibitor = new HashSet<>();
        inhibitor.add("forbidden" + letter);

        Map<String, Integer> output = new HashMap<>();
        output.put("critical", 1);
        output.put("inCritical" + letter, 1);
        output.put("forbidden" + letter, 1);
        return new Transition<>(input, zero, inhibitor, output);
    }

    private static Transition<String> exitTrans(String letter) {
        Map<String, Integer> input = new HashMap<>();
        input.put("inCritical" + letter, 1);
        input.put("critical", 1);

        Map<String, Integer> output = new HashMap<>();
        output.put("mutex", 1);
        output.put("ready" + letter, 1);

        return new Transition<>(input, new HashSet<>(), new HashSet<>(), output);
    }

    private static PetriNet<String> alternate() {
        Map<String, Integer> initial = new HashMap<>();
        initial.put("mutex", 1);
        initial.put("readyA", 1);
        initial.put("readyB", 1);
        initial.put("readyC", 1);
        return new PetriNet<>(initial, true);
    }

    private static Transition<String> runA() {
        return runTrans("A", "B", "C");
    }

    private static Transition<String> exitA() {
        return exitTrans("A");
    }

    private static Transition<String> runB() {
        return runTrans("B", "A", "C");
    }

    private static Transition<String> exitB() {
        return exitTrans("B");
    }

    private static Transition<String> runC() {
        return runTrans("C", "B", "A");
    }

    private static Transition<String> exitC() {
        return exitTrans("C");
    }

    public static void main(String[] args) throws InterruptedException {
        PetriNet<String> alternate = alternate();

        Collection<Transition<String>> allTrans = new HashSet<>();
        allTrans.add(runA());
        allTrans.add(runB());
        allTrans.add(runC());
        allTrans.add(exitA());
        allTrans.add(exitB());
        allTrans.add(exitC());

        Set<Map<String, Integer>> reachables = alternate.reachable(allTrans);

        System.out.println(reachables.size());

        for (Map<String, Integer> t: reachables) {
            if (t.containsKey("critical") && t.get("critical") > 1) {
                System.err.println("UNSAFE");
                return;
            }
        }
        Collection<Transition<String>> runASet = new HashSet<>();
        runASet.add(runA());
        Collection<Transition<String>> exitASet = new HashSet<>();
        exitASet.add(exitA());

        Thread A = new Thread(() -> {
            try {
                while (true) {
                    alternate.fire(runASet);
                    System.out.print(Thread.currentThread().getName());
                    System.out.print(".");
                    alternate.fire(exitASet);
                }
            } catch (InterruptedException e) {
                //program nie robi w takiej sytuacji nic, bo jest to normalne przerwanie jego działania
            }
        });
        A.setName("A");

        Collection<Transition<String>> runBSet = new HashSet<>();
        runBSet.add(runB());
        Collection<Transition<String>> exitBSet = new HashSet<>();
        exitBSet.add(exitB());

        Thread B = new Thread(() -> {
            try {
                while (true) {
                    alternate.fire(runBSet);
                    System.out.print(Thread.currentThread().getName());
                    System.out.print(".");
                    alternate.fire(exitBSet);
                }
            } catch (InterruptedException e) {
                //program nie robi w takiej sytuacji nic, bo jest to normalne przerwanie jego działania
            }
        });
        B.setName("B");

        Collection<Transition<String>> runCSet = new HashSet<>();
        runCSet.add(runC());
        Collection<Transition<String>> exitCSet = new HashSet<>();
        exitCSet.add(exitC());

        Thread C = new Thread(() -> {
            try {
                while (true) {
                    alternate.fire(runCSet);
                    System.out.print(Thread.currentThread().getName());
                    System.out.print(".");
                    alternate.fire(exitCSet);
                }
            } catch (InterruptedException e) {
                //program nie robi w takiej sytuacji nic, bo jest to normalne przerwanie jego działania
            }
        });
        C.setName("C");

        Thread sleep = new Thread(() -> {
            try {
                Thread.sleep(30000);
                A.interrupt();
                B.interrupt();
                C.interrupt();
            } catch (InterruptedException ignored) {  }

        });

        sleep.start();
        A.start();
        B.start();
        C.start();
        sleep.join();
    }
}
