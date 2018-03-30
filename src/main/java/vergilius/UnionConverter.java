package vergilius;

import vergilius.repos.TtypeRepository;

public class UnionConverter {
    public static String converts(Ttype myUnion, TtypeRepository rep2)
    {
        return FieldBuilder.recoursionProcessing(rep2, myUnion, 0).toString() + ";";
    }
}
