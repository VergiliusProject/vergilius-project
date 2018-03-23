package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldBuilder
{
    private String name;
    private StringBuilder type;
    private StringBuilder dim;
    private StringBuilder retval;
    private StringBuilder args;

    public FieldBuilder()
    {
        name = new String();
        type = new StringBuilder("");
        dim = new StringBuilder("");
        retval = new StringBuilder("");
        args = new StringBuilder("");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(StringBuilder type) {
        this.type = type;
    }

    public StringBuilder getType() {
        return type;
    }

    public void setDim(StringBuilder dim) {
        this.dim = dim;
    }

    public StringBuilder getDim() {
        return dim;
    }

    public StringBuilder getArgs() {
        return args;
    }

    public void setArgs(StringBuilder args) {
        this.args = args;
    }

    public void setRetval(StringBuilder retval) {
        this.retval = retval;
    }

    public StringBuilder getRetval() {
        return retval;
    }

    public static String modifConstructor(Ttype typeOfField)
    {
        return typeOfField.isIsConst()?"const ":"".concat(typeOfField.isIsVolatile() ? "volatile " : "");
    }

    public FieldBuilder recoursionProcessing(TtypeRepository rep2, Ttype typeOfField)
    {
        if (typeOfField.getKind() == Ttype.Kind.BASE)
        {
            setType(new StringBuilder(modifConstructor(typeOfField) + typeOfField.getName()).append(type));
            return this;
        }

        if (typeOfField.getKind() == Ttype.Kind.POINTER)
        {
            setType(new StringBuilder("* " + modifConstructor(typeOfField) + type));
            Optional<Tdata> fieldOfType = typeOfField.getData().stream().findFirst();
            if (!fieldOfType.isPresent())
            {
                type = new StringBuilder("\n\tis empty");
                return this;
            }

            typeOfField = rep2.findOne(fieldOfType.get().getId());
           return recoursionProcessing(rep2, typeOfField);
        }

        if (typeOfField.getKind() == Ttype.Kind.ARRAY)
        {
            Optional<Tdata> fieldOfType = typeOfField.getData().stream().findFirst();
            if (!fieldOfType.isPresent())
            {
                type = new StringBuilder("\n\tis empty");
                return this;
            }
            int offset = fieldOfType.get().getOffset();
            dim.append("[").append(offset).append("]");
            typeOfField = rep2.findOne(fieldOfType.get().getId());
            return recoursionProcessing(rep2, typeOfField);
        }

        if(typeOfField.getKind() == Ttype.Kind.FUNCTION)
        {
            List<Tdata> fieldOfFunc = Sorter.sortByOrdinal(typeOfField.getData().stream().collect(Collectors.toSet()));

            int counter = 0;
            for (Tdata k : fieldOfFunc)
            {
                typeOfField = rep2.findOne(k.getId());
                FieldBuilder tmp = recoursionProcessing(rep2, typeOfField);

                if (k.getName() != null && k.getName().equals("return"))
                {
                    retval = tmp.getType();
                    type = new StringBuilder("");
                }
                else
                {
                    if(counter == (fieldOfFunc.size() - 1))
                    {
                        args.append(tmp.getType() + " " + "arg" + counter);
                    }
                    else
                    {
                        args.append(tmp.getType() + " " + "arg" + counter + ", ");
                    }
                    type = new StringBuilder("");
                }
                counter++;
            }
            return recoursionProcessing(rep2, typeOfField);
        }
        return new FieldBuilder();
    }

}
