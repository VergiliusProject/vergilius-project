package vergilius;

import java.util.*;
import java.util.stream.Collectors;

public class EnumConverter {

    public static String converts(Ttype myEnum)
    {
        List<Tdata> tmpData = Sorter.sortByOrdinal(myEnum.getData());

        StringBuilder result = new StringBuilder("");
        result.append((myEnum.getName() != null)? "enum " + myEnum.getName() + "\n{\n" : "enum " + "\n{\n");

        StringBuilder strdata = new StringBuilder("");
        int i = 0;
        while(i < tmpData.size() - 1 )
        {
            strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset() + ", " + "\n");
            i++;
        }
        if(i == tmpData.size() - 1) strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset());

        result.append(strdata + "\n};");

        return result.toString();
    }

}
