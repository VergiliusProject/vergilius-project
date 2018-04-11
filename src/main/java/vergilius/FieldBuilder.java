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

    public static String retIndent(int indent)
    {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < indent; i++)
        {
            str.append("    ");
        }
        return str.toString();
    }

    public String toString()
    {
        if(retval.toString().isEmpty())
        {
            return type + " " + name + dim;
        }
        return retval + "(" + type + " " + name + dim + ")" + "(" + args + ")";
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setType(StringBuilder type) {
        this.type = type;
    }

    public StringBuilder getType() {
        return type;
    }

    public static String getModifier(Ttype typeOfField)
    {
        return (typeOfField.isIsConst() ? "const " : "") + (typeOfField.isIsVolatile() ? "volatile " : "");
    }

    public static FieldBuilder recoursionProcessing(TtypeRepository rep2, Ttype typeOfField, int indent)
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
                    FieldBuilder fb = FieldBuilder.recoursionProcessing(rep2,  rep2.findOne(fieldOfType.getId()), indent);
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

                FieldBuilder fb = recoursionProcessing(rep2, rep2.findOne(fieldOfType.getId()), indent);
                fb.type.append("*" + (getModifier(typeOfField).isEmpty() ? "" : " " + getModifier(typeOfField)));
                return fb;
            }
            case ARRAY:
            {
                Tdata fieldOfType = typeOfField.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                typeOfField = rep2.findOne(fieldOfType.getId());
                FieldBuilder fb = recoursionProcessing(rep2, typeOfField, indent);
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
                    FieldBuilder tmp = recoursionProcessing(rep2, typeOfField2, indent);

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

                if(typeOfField.getData() != null && typeOfField.getSizeof() != 0)
                {
                    fb.type.append("\n").append(retIndent(indent)).append("{");

                    indent++;
                    List<Tdata> structFields = Sorter.sortByOrdinal(typeOfField.getData());

                    for(Tdata i: structFields)
                    {
                        typeOfField = rep2.findOne(i.getId());
                        FieldBuilder field = FieldBuilder.recoursionProcessing(rep2, typeOfField, indent);
                        field.setName(i.getName());
                        fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");
                    }

                    //WHITESPACE
                    fb.type.append("\n").append(retIndent(--indent)).append("}");
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
                if(typeOfField.getName().equals("<unnamed-tag>"))
                {
                    fb.type.append("union " + getModifier(typeOfField));

                    if(typeOfField.getData() != null)
                    {
                        fb.type.append("\n").append(retIndent(indent)).append("{");

                        List<Tdata> StructFields = Sorter.sortByOrdinal(typeOfField.getData());
                        indent++;

                        boolean begin = false;
                        int last = StructFields.size() - 1;
                        for(int i = 0; i < StructFields.size(); i++)
                        {
                            typeOfField = rep2.findOne(StructFields.get(i).getId());

                            FieldBuilder field = FieldBuilder.recoursionProcessing(rep2, typeOfField, indent);
                            field.setName(StructFields.get(i).getName());

                            if(i == last)
                            {
                                if(begin) fb.type.append("\n").append(retIndent(indent)).append("};");
                                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");

                                break;
                            }

                            if(StructFields.get(i).getOffset() == StructFields.get(i + 1).getOffset())
                            {
                                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");

                            }

                            if(StructFields.get(i).getOffset() != StructFields.get(i + 1).getOffset() && !begin)
                            {
                                field.type = new StringBuilder("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1) + field.type);

                                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");

                                begin = true;
                            }
                            else if(StructFields.get(i).getOffset() != StructFields.get(i + 1).getOffset() && begin)
                            {
                                begin = false;
                                fb.type.append("\n").append(retIndent(indent + 1)).append(field.toString()).append(";");
                                fb.type.append("\n").append(retIndent(indent)).append("};");
                            }

                        }
                        fb.type.append("\n").append(retIndent(--indent)).append("}");
                    }
                }
                else
                {
                    fb.type.append("union " + getModifier(typeOfField)).append(typeOfField.getName());
                }
                return fb;
            }
            default:
            {
                return new FieldBuilder();
            }
        }

    }
}
