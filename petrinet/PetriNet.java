package petrinet;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SuppressWarnings("NonAsciiCharacters")
public class PetriNet<T> {

    private volatile Map<T, Integer> initial; //aktualne znakowanie

    //wstrzymuje wątek na locku pisarza na rzecz wątku na semaforze własnym
    private Semaphore waiter = new Semaphore(0);

    private ReentrantReadWriteLock readWrite; //semafor dla czytelników (reachable) i nowych pisarzy (fire)

    //semafor, na który czeka proces, dla którego nie udało się znaleźć dozwolonego przejścia
    private volatile ArrayList<Semaphore> semaforyWstrzymanych = new ArrayList<>();

    //tu trzymamy przejścia dla poszczególnych odpaleń firea
    private volatile ArrayList<Collection<Transition<T>>> przejściaWstrzymane = new ArrayList<>();

    //czy przejście zostało już odpalone
    private volatile ArrayList<Boolean> czyOdpalone = new ArrayList<>();

    public PetriNet(Map<T, Integer> initial, boolean fair) {
        this.initial = initial;
        readWrite = new ReentrantReadWriteLock(fair);
    }

    public Set<Map<T, Integer>> reachable(Collection<Transition<T>> transitions) {
        readWrite.readLock().lock();
        Set<Map<T, Integer>> actual = new HashSet<>();
        Map<T, Integer> act = new HashMap<>(this.initial);
        actual.add(act);
        long howMuchBefore = 0;
        while (howMuchBefore < actual.size()) { //jeśli nic ostatnio nie zostało dodane do seta, kończymy
            Set<Map<T, Integer>> newSet = new HashSet<>(); //tu zbieramy nowe znakowania
            howMuchBefore = actual.size();

            for (Map<T, Integer> j: actual) {
                for(Transition<T> t: transitions) {
                    if (enabled(j, t)) {

                        Map<T, Integer> newMap = new HashMap<>(j); //nowa mapa do seta
                        proceed(t, newMap);
                        newSet.add(newMap);
                    }
                }
            }
            actual.addAll(newSet); //przerzucenie do seta głównego
        }
        readWrite.readLock().unlock();
        return actual;
    }

    public Transition<T> fire(Collection<Transition<T>> transitions)  throws InterruptedException {
        readWrite.writeLock().lockInterruptibly();

        int nr = przejściaWstrzymane.size(); //numer odpalonego firea
        przejściaWstrzymane.add(nr, transitions);
        semaforyWstrzymanych.add(nr, new Semaphore(0));
        czyOdpalone.add(nr, false);

        for (Transition<T> t: transitions) {
            if (enabled(initial, t)) {
                //udało się znaleźć dozwolone przejście
                proceed(t, initial);
                czyOdpalone.set(nr, true);
                //odpalanie do skutku innych fireów, które mają dozwolone przejścia
                //robię to uczciwie niezależnie od uczciwości writeLocka
                int iter2 = 0;
                while (iter2 < nr) {
                    if (czyOdpalone.get(iter2) || !possible(przejściaWstrzymane.get(iter2)))
                        iter2++;
                    else {
                        semaforyWstrzymanych.get(iter2).release();
                        iter2 = 0;
                        //wstrzymuje proces do momentu, gdy proces na semaforze odpalonym odpali przejście
                        waiter.acquire();
                    }
                }
                readWrite.writeLock().unlock(); //wpuszczenie kolejnego nowego firea
                return t;
            }
        }
        //nie udało się znaleźć możliwego przejścia
        czyOdpalone.set(nr, false);
        readWrite.writeLock().unlock(); //wpuszczamy nowy fire, który może zmienić sytuację
        semaforyWstrzymanych.get(nr).acquire(); //czekamy, aż będziemy pierwsi z kolekcją z przejściem dozwolonym

        for (Transition<T> t: transitions) {
            if (enabled(initial, t)) {
                //już wiemy, że na pewno dla któregoś t tu wejdzie
                proceed(t, initial);
                czyOdpalone.set(nr, true);
                waiter.release(); //zwalniamy głównego firea
                return t;
            }
        }
        return null; //dodane tylko po to, żeby nie wywalało błędu – w rzeczywistości program nigdy tu nie dojdzie
    }

    //sprawdza, czy jakieś przejście z kolekcji jest dozwolone
    private boolean possible (Collection<Transition<T>> transitions) {
        for (Transition<T> t: transitions) {
            if (enabled(initial, t)) {
                return true;
            }
        }
        return false;
    }

    //sprawdza, czy konkretne przejście jest dozwolone
    private boolean enabled(Map<T, Integer> map, Transition<T> transition) {
        for (T i: transition.input.keySet())
            if (!map.containsKey(i) || map.get(i) < transition.input.get(i))
                return false;

        for (T i: transition.inhibitor)
            if (map.containsKey(i))
                if (map.get(i) != 0)
                    return false;

        return true;
    }

    //zmiana znakowania mapy na podstawie przejścia
    private void proceed(Transition<T> t, Map<T, Integer> newMap) {
        for (T i: t.input.keySet()) {
            if (!newMap.containsKey(i)) newMap.put(i, 0);
            newMap.put(i, newMap.get(i) - t.input.get(i));
        }

        for (T i: t.reset) {
            newMap.put(i, 0);
        }

        for (T i: t.output.keySet()) {
            if (!newMap.containsKey(i)) newMap.put(i, 0);
            newMap.put(i, t.output.get(i) + newMap.get(i));
        }
        for (Iterator<Map.Entry<T, Integer>> i = newMap.entrySet().iterator(); i.hasNext();) {
            Map.Entry<T, Integer> e = i.next();
            int v = e.getValue();
            if (v == 0)
                i.remove();
        }
    }

    // zwraca liczbę żetonów w danym miejscu
    public Integer getTokens(T key) {
        if (!initial.containsKey(key)) return 0;
        return initial.get(key);
    }

}