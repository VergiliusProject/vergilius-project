package vergilius;

import vergilius.repos.TtypeRepository;

public class UnionConverter {
    public static String converts(Ttype myUnion, TtypeRepository rep2, String keyWord)
    {
        return StructConverter.converts(myUnion, rep2, "union ");
    }
}
