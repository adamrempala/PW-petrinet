package multiplicator;

import petrinet.PetriNet;
import petrinet.Transition;

import java.util.*;

public class Main {
    private static PetriNet<String> multiplicate(int a, int b) {
        Map<String, Integer> initial = new HashMap<>();
        initial.put("starterA", a);
        initial.put("starterB", b);
        return new PetriNet<>(initial, false);
    }
    private static Transition<String> addToFinal() {
        Map<String, Integer> addToFinalInput = new HashMap<>();
        addToFinalInput.put("help1", 1);

        Map<String, Integer> addToFinalOutput = new HashMap<>();
        addToFinalOutput.put("help2", 1);
        addToFinalOutput.put("final", 1);

        Set<String> addToFinalInhibitor = new HashSet<>();
        addToFinalInhibitor.add("starterA");

        return new Transition<>(addToFinalInput, new HashSet<>(), addToFinalInhibitor, addToFinalOutput);
    }
    private static Transition<String> returnToStarter() {
        Map<String, Integer> returnToStarterInput = new HashMap<>();
        returnToStarterInput.put("help2", 1);


        Collection<String> returnToStarterInhibitor = new HashSet<>();
        returnToStarterInhibitor.add("help1");

        Map<String, Integer> returnToStarterOutput = new HashMap<>();
        returnToStarterOutput.put("starterA", 1);

        return new Transition<>(returnToStarterInput, new HashSet<>(), returnToStarterInhibitor, returnToStarterOutput);
    }

    private static Transition<String> startMultiply() {
        Map<String, Integer> startMultiplyInput = new HashMap<>();
        startMultiplyInput.put("starterA", 1);
        startMultiplyInput.put("starterB", 1);


        Collection<String> startMultiplyInhibitor = new HashSet<>();
        startMultiplyInhibitor.add("help1");
        startMultiplyInhibitor.add("help2");


        Map<String, Integer> startMultiplyOutput = new HashMap<>();
        startMultiplyOutput.put("help1", 1);

        return new Transition<>(startMultiplyInput, new HashSet<>(), startMultiplyInhibitor, startMultiplyOutput);
    }

    private static Transition<String> continueMultiply() {
        Map<String, Integer> continueMultiplyInput = new HashMap<>();
        continueMultiplyInput.put("starterA", 1);
        continueMultiplyInput.put("help1", 1);


        Collection<String> continueMultiplyInhibitor = new HashSet<>();
        continueMultiplyInhibitor.add("help2");


        Map<String, Integer> continueMultiplyOutput = new HashMap<>();
        continueMultiplyOutput.put("help1", 2);

        return new Transition<>(continueMultiplyInput, new HashSet<>(), continueMultiplyInhibitor, continueMultiplyOutput);
    }

    private static Transition<String> finalTransition() {
        Map<String, Integer> finalTransitionInput = new HashMap<>();
        finalTransitionInput.put("final", 1);


        Collection<String> finalTransitionInhibitor = new HashSet<>();
        finalTransitionInhibitor.add("help2");
        finalTransitionInhibitor.add("help1");
        finalTransitionInhibitor.add("starterB");


        Map<String, Integer> finalTransitionOutput = new HashMap<>();
        finalTransitionOutput.put("final", 1);

        return new Transition<>(finalTransitionInput, new HashSet<>(), finalTransitionInhibitor, finalTransitionOutput);
    }

    public static void main(String[] args) throws InterruptedException {
        Collection<Transition<String>> withoutFinal = new HashSet<>();
        withoutFinal.add(startMultiply());
        withoutFinal.add(continueMultiply());
        withoutFinal.add(returnToStarter());
        withoutFinal.add(addToFinal());

        Collection<Transition<String>> finalTr = new HashSet<>();
        finalTr.add(finalTransition());

        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt();
        int b = sc.nextInt();
        PetriNet<String> multiplicate = multiplicate(a, b);

        Set<Thread> threads = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            threads.add(new Thread(() -> {
                int howMuch = 0;
                try {
                    while (true) {
                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println(howMuch);
                            break;
                        }
                        multiplicate.fire(withoutFinal);
                        howMuch++;

                    }
                } catch (InterruptedException e) {
                    System.out.println(howMuch);
                }
            }));
        }
        for (Thread t: threads) {
            t.start();
        }

        multiplicate.fire(finalTr);
        System.out.println(multiplicate.getTokens("final"));
        for (Thread t: threads) {
            t.interrupt();
        }
    }
}
