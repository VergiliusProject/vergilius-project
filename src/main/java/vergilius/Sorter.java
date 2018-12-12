package vergilius;

import java.util.Collections;
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
    public static List<Ttype> sortByName(List<Ttype> list)
    {
        Comparator<Ttype> byName = Comparator.comparing(Ttype::getName);
        return list.stream().sorted(byName).collect(Collectors.toList());
    }
    public static List<Os> sortByBuildnumber(List<Os> list, boolean natural)
    {
        Comparator<Os> comp = new Os();
        if(natural)
        {
            list.sort(comp);
        }
        else
        {
            list.sort(comp.reversed());
        }
        return list;
    }
}
