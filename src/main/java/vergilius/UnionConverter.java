package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;

public class UnionConverter {
    public static String converts(Ttype myUnion, TtypeRepository rep2)
    {
        List<Tdata> tmpData = Sorter.sortByOrdinal(myUnion.getData());

        FieldBuilder fb = new FieldBuilder();

        fb.setType(new StringBuilder("//0x" + Integer.toHexString(myUnion.getSizeof()) + " bytes (sizeof)\n"));

        String rec = (myUnion.getName() != null)? "union " + myUnion.getName() + "\n{\n" : "union " + "\n{\n";
        fb.setType(fb.getType().append(rec));

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
