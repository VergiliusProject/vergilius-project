package vergilius;

import java.util.*;
import java.util.stream.Collectors;

public class EnumConverter {

    public static String converts(Ttype enumeration)
    {
        List<Tdata> enumData = Sorter.sortByOrdinal(enumeration.getData());

        StringBuilder result = new StringBuilder().append((enumeration.getName() != null)? "enum " + enumeration.getName() + "\n{\n" : "enum\n{\n");

        for(int i = 0; i < enumData.size() - 1; i++)
        {
            result.append("    " + enumData.get(i).getName() + " = " + enumData.get(i).getOffset() + ",\n");
        }

        result.append("    " + enumData.get(enumData.size() - 1).getName() + " = " + enumData.get(enumData.size() - 1).getOffset()+ "\n};");

        return result.toString();
    }

}
