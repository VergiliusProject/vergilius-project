package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;

public class UnionConverter {
    public static String converts(Ttype currentUnion, TtypeRepository rep2)
    {
        List<Tdata> unionData = Sorter.sortByOrdinal(currentUnion.getData());

        FieldBuilder fb = new FieldBuilder();
        fb.setType(new StringBuilder((currentUnion.getName() != null)? "union " + currentUnion.getName() + "\n{\n" : "union\n{\n"));

        for(Tdata i: unionData)
        {
            Ttype typeOfField = rep2.findOne(i.getId());
            FieldBuilder field = FieldBuilder.recoursionProcessing(rep2, typeOfField, 0);
            field.setName(i.getName() + ";");

            fb.setType(new StringBuilder(fb.getType() + "    " + field.toString() + "\n"));
        }
        fb.setType(fb.getType().append("};"));

        return fb.toString();
    }
}
