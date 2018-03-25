package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldBuilder
{
    private String name = new String();
    private StringBuilder type = new StringBuilder();
    private StringBuilder dim = new StringBuilder();
    private StringBuilder retval = new StringBuilder();
    private StringBuilder args = new StringBuilder();

    public String toString()
    {
        if(retval.toString().isEmpty()) //isn't a function
        {
            return type + " " + name + dim; // array, base, pointer
        }
        return retval + "(" + type + " " + name + ")" + "(" + args + ")";
    }

    public void setName(String name) {
        this.name = name;
    }


    public static String getModifier(Ttype typeOfField)
    {
        return (typeOfField.isIsConst() ? "const " : "") + (typeOfField.isIsVolatile() ? "volatile " : "");
    }

    public static FieldBuilder recoursionProcessing(TtypeRepository rep2, Ttype typeOfField)
    {
        if (typeOfField.getKind() == Ttype.Kind.BASE)
        {
            FieldBuilder fb = new FieldBuilder();
            fb.type.append(getModifier(typeOfField)).append(typeOfField.getName());
            return fb;
        }

        if (typeOfField.getKind() == Ttype.Kind.POINTER)
        {
            Tdata fieldOfType = typeOfField.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
            FieldBuilder fb = recoursionProcessing(rep2, rep2.findOne(fieldOfType.getId()));
            fb.type.append("*" + (getModifier(typeOfField).isEmpty() ? "" : " " + getModifier(typeOfField)));

            return fb;
        }

        if (typeOfField.getKind() == Ttype.Kind.ARRAY)
        {
            Tdata fieldOfType = typeOfField.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
            typeOfField = rep2.findOne(fieldOfType.getId());
            FieldBuilder fb = recoursionProcessing(rep2, typeOfField);
            int offset = fieldOfType.getOffset();
            fb.dim = new StringBuilder("[" + offset + "]" + fb.dim);
            return fb;
        }

        if(typeOfField.getKind() == Ttype.Kind.FUNCTION)
        {
            FieldBuilder fb = new FieldBuilder();

            List<Tdata> fieldOfFunc = Sorter.sortByOrdinal(typeOfField.getData().stream().collect(Collectors.toSet()));

            int counter = 0;
            for (Tdata k : fieldOfFunc)
            {
                Ttype typeOfField2 = rep2.findOne(k.getId());
                FieldBuilder tmp = recoursionProcessing(rep2, typeOfField2);

                if ("return".equals(k.getName()))
                {
                    fb.retval.append(tmp.toString());
                }
                else
                {
                    tmp.name = "arg"  + counter;
                    fb.args.append(tmp.toString());
                    if(counter != (fieldOfFunc.size() - 1))
                    {
                        fb.args.append(", ");
                    }
                }
                counter++;
            }
            return fb;
        }
        if(typeOfField.getKind() == Ttype.Kind.ENUM)
        {
            FieldBuilder fb = new FieldBuilder();
            fb.type.append("enum " + getModifier(typeOfField)).append(typeOfField.getName());
            return fb;
        }

        if(typeOfField.getKind() == Ttype.Kind.UNION)
        {
            FieldBuilder fb = new FieldBuilder();
            fb.type.append("union " + getModifier(typeOfField)).append(typeOfField.getName());
            return fb;
        }

        if(typeOfField.getKind() == Ttype.Kind.STRUCT)
        {
            FieldBuilder fb = new FieldBuilder();
            fb.type.append("struct " + getModifier(typeOfField)).append(typeOfField.getName());
            return fb;
        }
        return new FieldBuilder();
    }

}
