import java.util.Arrays;
import java.util.List;

public final class RouteHelperTest {
    private static void check(List<Integer> actual, List<Integer> expected, String label) {
        if (!actual.equals(expected)) throw new AssertionError(label + ": " + actual + " != " + expected);
        System.out.println(label + ": " + actual);
    }
    public static void main(String[] args) {
        int[][] edges = {
            {0,1,2}, {0,1,2}, {1,2,3}, {0,2,5}, {3,4,1}
        };
        int[] parent = {-1,0,2,-1,-1};
        check(Algorithms.reconstructRoute(0,2,parent,edges), Arrays.asList(0,1,2), "reachable target (selected parallel edge id 0, then id 2)");
        check(Algorithms.reconstructRoute(0,0,parent,edges), Arrays.asList(0), "source target");
        check(Algorithms.reconstructRoute(0,4,parent,edges), List.of(), "unreachable target");
        int[] tied = {-1,1,-1,-1,-1};
        check(Algorithms.reconstructRoute(0,1,tied,edges), Arrays.asList(0,1), "tied/parallel alternative using edge id 1");
    }
}
