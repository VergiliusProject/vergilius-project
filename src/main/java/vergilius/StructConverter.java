package vergilius;

import vergilius.repos.TtypeRepository;

public class StructConverter {

    public static String converts(Ttype currentStruct, TtypeRepository rep2)
    {
        //???
        //.trim() + ";";
        return FieldBuilder.recoursionProcessing(rep2, currentStruct, 0).toString();
    }
}
