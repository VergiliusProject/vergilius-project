package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.*;
import java.util.stream.Collectors;

public class EnumConverter {

    public static String converts(Ttype enumeration, TtypeRepository rep2)
    {
        return FieldBuilder.recoursionProcessing(rep2, enumeration ,0).toString();
    }

}
