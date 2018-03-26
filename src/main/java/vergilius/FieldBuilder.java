package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.List;
import java.util.NoSuchElementException;
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
        if(retval.toString().isEmpty())
        {
            return type + " " + name + dim;
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
        switch (typeOfField.getKind())
        {
            case BASE:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append(getModifier(typeOfField)).append(typeOfField.getName());
                return fb;
            }
            case POINTER:
            {
                Tdata fieldOfType = typeOfField.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());

                String name = rep2.findOne(fieldOfType.getId()).getName();

                if(rep2.findOne(fieldOfType.getId()).getKind() == Ttype.Kind.STRUCT && name.equals("<unnamed-tag>"))
                {
                    FieldBuilder fb = FieldBuilder.recoursionProcessing(rep2,  rep2.findOne(fieldOfType.getId()));
                    fb.type.append("*" + (getModifier(typeOfField).isEmpty() ? "" : " " + getModifier(typeOfField)));
                    return fb;
                }

                if(rep2.findOne(fieldOfType.getId()).getKind() == Ttype.Kind.STRUCT)
                {
                    FieldBuilder fb = new FieldBuilder();
                    fb.type.append("struct " + name);
                    fb.type.append("*" + (getModifier(typeOfField).isEmpty() ? "" : " " + getModifier(typeOfField)));
                    return fb;
                }

                FieldBuilder fb = recoursionProcessing(rep2, rep2.findOne(fieldOfType.getId()));
                fb.type.append("*" + (getModifier(typeOfField).isEmpty() ? "" : " " + getModifier(typeOfField)));
                return fb;
            }
            case ARRAY:
            {
                Tdata fieldOfType = typeOfField.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                typeOfField = rep2.findOne(fieldOfType.getId());
                FieldBuilder fb = recoursionProcessing(rep2, typeOfField);
                int offset = fieldOfType.getOffset();
                fb.dim = new StringBuilder("[" + offset + "]" + fb.dim);
                return fb;
            }
            case FUNCTION:
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
            case STRUCT:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append("struct " + getModifier(typeOfField)).append(typeOfField.getName().equals("<unnamed-tag>") ? "" : typeOfField.getName());

                if(typeOfField.getData() != null)
                {
                    fb.type.append("\n{\n");
                    List<Tdata> StructFields = Sorter.sortByOrdinal(typeOfField.getData());

                    for(Tdata i: StructFields)
                    {
                        typeOfField = rep2.findOne(i.getId());
                        FieldBuilder field = FieldBuilder.recoursionProcessing(rep2, typeOfField);
                        field.setName(i.getName());
                        fb.type.append("\n\t").append(field.toString()).append(";");
                    }
                    fb.type.append("\n}\n");
                }

                return fb;
            }
            case ENUM:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append("enum " + getModifier(typeOfField)).append(typeOfField.getName());
                return fb;
            }
            case UNION:
            {
                FieldBuilder fb = new FieldBuilder();
                String name = typeOfField.getName().equals("<unnamed-tag>") ? "" : typeOfField.getName();
                fb.type.append("union " + getModifier(typeOfField)).append(name);
                return fb;
            }
            default:
            {
                return new FieldBuilder();
            }
        }

    }
}
