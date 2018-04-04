package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;

public class UnionConverter {
    public static String converts(Ttype myUnion, TtypeRepository rep2)
    {
        //return FieldBuilder.recoursionProcessing(rep2, myUnion, 0).toString().trim()  + ";";

        List<Tdata> tmpData = Sorter.sortByOrdinal(myUnion.getData());

        FieldBuilder fb = new FieldBuilder();
        String rec = (myUnion.getName() != null)? "union " + myUnion.getName() + "\n{" : "union " + "\n{";
        fb.setType(new StringBuilder(rec));

        //SIZE OF UNION
        fb.setType(fb.getType().append(" //0x" + Integer.toHexString(myUnion.getSizeof()) + " bytes (sizeof)" + "\n"));

        int forOffsets = 0;
        for(Tdata i: tmpData)
        {
            Ttype typeOfField = rep2.findOne(i.getId());

            FieldBuilder field = FieldBuilder.recoursionProcessing(rep2, typeOfField, 0);
            field.setName(i.getName() + ";");

            //OFFSET
            forOffsets += i.getOffset();
            fb.setType(new StringBuilder(fb.getType() + "\t" + field.toString() + "      //0x" + Integer.toHexString(forOffsets) +"\n"));
        }
        fb.setType(fb.getType().append("};"));

        return fb.toString();
    }
}
