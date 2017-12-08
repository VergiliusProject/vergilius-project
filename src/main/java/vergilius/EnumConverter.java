package vergilius;

import java.util.*;
import java.util.stream.Collectors;

public class EnumConverter {

    public static List<Tdata> sortByOrdinal(Set<Tdata> set)
    {
        List<Tdata> arr = new ArrayList<>();
        List<Tdata> arr2 = new ArrayList<>();
        Iterator<Tdata> iter = set.iterator();

        while(iter.hasNext())
        {
            arr.add(iter.next());
        }

        Comparator<Tdata> byOrdinal = (e1, e2) -> Integer.compare(e1.getOrdinal(), e2.getOrdinal());
        return arr.stream().sorted(byOrdinal).collect(Collectors.toList());
    }

    public static String converts(Ttype myEnum)
    {
        List<Tdata> tmpData = EnumConverter.sortByOrdinal(myEnum.getData());

        StringBuilder strdata = new StringBuilder("");
        String result;
        int i = 0;
        while(i < tmpData.size() - 1 )
        {
            strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset() + ", " + "\n");
            i++;
        }
        if(i == tmpData.size() - 1) strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset());

        result = (myEnum.getName() != null)? "enum " + myEnum.getName()+ "\n{\n" + strdata + "\n};\n": "enum " + "\n{\n" + strdata + "\n{\n";

        return result;
    }

}
