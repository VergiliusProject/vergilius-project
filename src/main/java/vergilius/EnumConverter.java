package vergilius;

import java.util.*;
import java.util.stream.Collectors;

public class EnumConverter {

    public static String converts(Ttype myEnum)
    {
        List<Tdata> tmpData = Sorter.sortByOrdinal(myEnum.getData());

        StringBuilder result = new StringBuilder("");
        result.append("//0x" + Integer.toHexString(myEnum.getSizeof()) + " bytes (sizeof)" + "\n");
        result.append((myEnum.getName() != null)? "enum " + myEnum.getName() + "\n{" : "enum " + "\n{");

        StringBuilder strdata = new StringBuilder("");
        int i = 0;
        int forOffsets = 0;
        while(i < tmpData.size() - 1 )
        {
            strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset() + ",");

            //OFFSET
            forOffsets += tmpData.get(i).getOffset();
            strdata.append("      //0x" + Integer.toHexString(forOffsets) + "\n");
            i++;
        }
        if(i == tmpData.size() - 1)
        {
            strdata.append("\t" + tmpData.get(i).getName() + " = " + tmpData.get(i).getOffset());

            //OFFSET
            forOffsets += tmpData.get(i).getOffset();
            strdata.append("      //0x" + Integer.toHexString(forOffsets) + "\n");
        }

        result.append(strdata + "};");

        return result.toString();
    }

}
