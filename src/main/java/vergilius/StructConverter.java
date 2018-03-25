package vergilius;

import vergilius.repos.TtypeRepository;
import java.util.List;


public class StructConverter {

    public static String converts(Ttype Struct, TtypeRepository rep2, String keyWord)
    {
        List<Tdata> StructFields = Sorter.sortByOrdinal(Struct.getData());

        StringBuilder result = new StringBuilder(keyWord + Struct.getName() + "\n{");

        for(Tdata i: StructFields)
        {
            Ttype typeOfField = rep2.findOne(i.getId());
            FieldBuilder fb = FieldBuilder.recoursionProcessing(rep2, typeOfField);
            fb.setName(i.getName());

            result.append("\n\t").append(fb.toString()).append(";");
        }
        return result.append("\n};").toString();
    }
}
