package vergilius;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Sorter {
    public static List<Tdata> sortByOrdinal(Set<Tdata> set)
    {
        //??
        Comparator<Tdata> byOrdinal = (e1, e2) -> Integer.compare(e1.getOrdinal(), e2.getOrdinal());
        return set.stream().sorted(byOrdinal).collect(Collectors.toList());
    }
}
