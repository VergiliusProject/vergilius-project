package vergilius;

import vergilius.repos.TtypeRepository;
import java.util.List;
import java.util.Optional;


public class StructConverter {

    public static String converts(Ttype Struct, TtypeRepository rep2, String keyWord)
    {
        List<Tdata> StructFields = Sorter.sortByOrdinal(Struct.getData());

        StringBuilder result = new StringBuilder(keyWord + Struct.getName() + "\n{");


        for(Tdata i: StructFields)
        {
            FieldBuilder resObj = new FieldBuilder();
            Ttype typeOfField = rep2.findOne(i.getId());

            if(typeOfField.getKind() == Ttype.Kind.POINTER || typeOfField.getKind() == Ttype.Kind.BASE)
            {
                FieldBuilder construct = resObj.recoursionProcessing(rep2, typeOfField);
                construct.setName(i.getName());

                Optional<Tdata> fieldOfType = typeOfField.getData().stream().findFirst();
                if (fieldOfType.isPresent() && (rep2.findOne(fieldOfType.get().getId())).getKind() == Ttype.Kind.FUNCTION)
                {
                    String name = construct.getName();
                    StringBuilder retVal = construct.getRetval();
                    StringBuilder args = construct.getArgs();
                    result.append(new StringBuilder("\n\t").append(retVal).append(" ").append(name).append(" ").append("(" + args + ")").append(";").toString());
                    continue;
                }

                result.append(new StringBuilder("\n\t").append(construct.getType())).append(" ").append(construct.getName()).append(";").toString();
            }

            if(typeOfField.getKind() == Ttype.Kind.ARRAY) {
                FieldBuilder construct = resObj.recoursionProcessing(rep2, typeOfField);
                construct.setName(i.getName());
                result.append(new StringBuilder("\n\t").append(construct.getType())).append(" ").append(construct.getName()).append(construct.getDim()).append(";").toString();
            }

        }
        return result.append("\n};").toString();
    }
}
