package vergilius;

import vergilius.repos.TtypeRepository;

public class StructConverter {

    public static String converts(Ttype Struct, TtypeRepository rep2, String keyWord)
    {
        return FieldBuilder.recoursionProcessing(rep2, Struct, 0).toString();
    }
}
