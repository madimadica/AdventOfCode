package org.togetherjava.aoc.internal;

import org.reflections.Reflections;
import org.togetherjava.aoc.core.Utils;
import org.togetherjava.aoc.core.puzzle.PuzzleSolution;
import org.togetherjava.aoc.internal.annotations.AdventOfCodeCalendar;
import org.togetherjava.aoc.internal.annotations.DayOverride;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class SolutionRegistry {

    public static class Entry {
        private final Class<? extends PuzzleSolution> solution;
        private final PuzzleDate date;

        public Entry(Class<? extends PuzzleSolution> solution, PuzzleDate date) {
            this.solution = solution;
            this.date = date;
        }

        public Class<? extends PuzzleSolution> getSolutionClass() {
            return solution;
        }

        public PuzzleDate getDate() {
            return date;
        }
    }

    private final Map<Integer, Map<Integer, List<Entry>>> calendarYears = new HashMap<>();
    private final Map<Class<? extends PuzzleSolution>, Entry> solutionInfoMap = new HashMap<>();

    private SolutionRegistry() {}

    private static SolutionRegistry INSTANCE = null;

    public static SolutionRegistry get() {
        if (INSTANCE == null) {
            init();
        }
        return INSTANCE;
    }

    public Map<Integer, List<Entry>> getSolutionCalendar(int year) {
        return calendarYears.computeIfAbsent(year, ignored -> new HashMap<>());
    }

    public List<Entry> getSolutions(int year, int day) {
        var yearData = getSolutionCalendar(year);
        return yearData.computeIfAbsent(day, ignored -> new ArrayList<>());
    }

    public List<Entry> getSolutions(PuzzleDate puzzleDate) {
        return getSolutions(puzzleDate.year(), puzzleDate.day());
    }

    public void register(int year, int day, Class<? extends PuzzleSolution> solution) {
        register(new PuzzleDate(year, day), solution);
    }

    public void register(PuzzleDate date, Class<? extends PuzzleSolution> solution) {
        List<Entry> solutions = getSolutions(date);
        var info = new Entry(solution, date);
        solutions.add(info);
        solutionInfoMap.put(solution, info);
    }

    public Entry getEntry(Class<? extends PuzzleSolution> solutionClass) {
        return solutionInfoMap.get(solutionClass);
    }

    private static void init() {
        INSTANCE = new SolutionRegistry();
        Reflections reflections = new Reflections("org.togetherjava.aoc");

        // Find all packages annotated with @AdventOfCodeCalendar to extract the year and solutions
        Set<Class<?>> packageInfoClasses = reflections.getTypesAnnotatedWith(AdventOfCodeCalendar.class);
        for (Class<?> packageInfoClass : packageInfoClasses) {
            Package packageInfoPackage = packageInfoClass.getPackage();
            String packageName = packageInfoPackage.getName();

            AdventOfCodeCalendar packageInfoAnnotation = packageInfoClass.getAnnotation(AdventOfCodeCalendar.class);
            int year = packageInfoAnnotation.year();

            Reflections packageReflections = new Reflections(packageName);
            var puzzleSolutions = packageReflections.getSubTypesOf(PuzzleSolution.class);
            for (var soln : puzzleSolutions) {
                Optional<Integer> solnDay = extractDay(soln.getSimpleName());
                Optional<Integer> dayOverride = getDayOverride(soln);
                if (dayOverride.isPresent()) {
                    solnDay = dayOverride;
                }
                if (solnDay.isEmpty()) {
                    throw new RuntimeException("""
                            Unable to detect a valid day value from solution type '%s'. \
                            Follow standard naming, or use @DayOverride() instead.\
                            """.formatted(soln.getCanonicalName())
                    );
                }
                INSTANCE.register(year, solnDay.get(), soln);
            }
        }
    }

    private static Optional<Integer> extractDay(String className) {
        Pattern digits = Pattern.compile("\\d+");
        return digits.matcher(className)
                .results()
                .map(MatchResult::group)
                .map(Utils::trimLeadingZeros)
                .findFirst();

    }

    private static Optional<Integer> getDayOverride(Class<? extends PuzzleSolution> clazz) {
        DayOverride dayOverride = clazz.getAnnotation(DayOverride.class);
        if (dayOverride == null) {
            return Optional.empty();
        }
        return Optional.of(dayOverride.day());
    }

}