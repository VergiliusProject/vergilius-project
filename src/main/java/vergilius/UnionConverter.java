package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;

public class UnionConverter {
    public static String converts(Ttype currentUnion, TtypeRepository rep2)
    {
        return FieldBuilder.recoursionProcessing(rep2,currentUnion, 0).toString();
    }
}
