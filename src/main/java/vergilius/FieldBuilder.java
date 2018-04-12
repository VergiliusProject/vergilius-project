package vergilius;

import vergilius.repos.TtypeRepository;

import java.net.URL;
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

    //The method returns a string indent for a type, which depends on nesting level of this type
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

    //The method checks type's modifier (const or volatile)
    public static String getModifier(Ttype typeOfField)
    {
        return (typeOfField.isIsConst() ? "const " : "") + (typeOfField.isIsVolatile() ? "volatile " : "");
    }

    //The method returns an object(fb), which represents some type.
    public static FieldBuilder recoursionProcessing(TtypeRepository rep, Ttype type, int indent)
    {
        switch (type.getKind())
        {
            case BASE:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append(getModifier(type)).append(type.getName()); //type.getName() -> int, char etc.
                return fb;
            }
            case POINTER:
            {
                //getting a new type on which "current" type points to
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());

                String name = rep.findOne(refType.getId()).getName();

                if(rep.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT && name.equals("<unnamed-tag>"))
                {
                    FieldBuilder fb = FieldBuilder.recoursionProcessing(rep,  rep.findOne(refType.getId()), indent);
                    fb.type.append("*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));
                    return fb;
                }

                if(rep.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT)
                {
                    FieldBuilder fb = new FieldBuilder();
                    fb.type.append("struct " + name + "*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));
                    return fb;
                }

                FieldBuilder fb = recoursionProcessing(rep, rep.findOne(refType.getId()), indent);
                fb.type.append("*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));
                return fb;

            }
            case ARRAY:
            {
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                type = rep.findOne(refType.getId());
                FieldBuilder fb = recoursionProcessing(rep, type, indent);
                fb.dim = new StringBuilder("[" + refType.getOffset() + "]" + fb.dim);
                return fb;
            }
            case FUNCTION:
            {
                FieldBuilder fb = new FieldBuilder();

                List<Tdata> funcComponents = Sorter.sortByOrdinal(type.getData().stream().collect(Collectors.toSet()));

                int counter = 0;
                for (Tdata component : funcComponents)
                {
                    Ttype typeOfComponent = rep.findOne(component.getId());
                    FieldBuilder fbType = recoursionProcessing(rep, typeOfComponent, indent);

                    if ("return".equals(component.getName()))
                    {
                        fb.retval.append(fbType.toString());
                    }
                    else
                    {
                        fbType.name = "arg"  + counter;
                        fb.args.append(fbType.toString());
                        if(counter != (funcComponents.size() - 1))
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

                fb.type.append("struct" + getModifier(type)).append(type.getName().equals("<unnamed-tag>") ? "" : " " +  type.getName());

                //if structure isn't bodiless
                if(type.getData() != null && type.getSizeof() != 0)
                {
                    fb.type.append("\n").append(retIndent(indent)).append("{");
                    indent++; //all structure fields have a deeper level of nesting
                    List<Tdata> structFields = Sorter.sortByOrdinal(type.getData());

                    for (Tdata currentField : structFields)
                    {
                        type = rep.findOne(currentField.getId());
                        FieldBuilder field = FieldBuilder.recoursionProcessing(rep, type, indent);
                        field.setName(currentField.getName());
                        fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");
                    }
                    fb.type.append("\n").append(retIndent(--indent)).append("}");// braces are always on the same level with a word 'struct'
                }
                //???
                if(indent == 0)
                {
                    fb.type.append(";");
                }
                return fb;
            }
            case ENUM:
            {
                FieldBuilder fb = new FieldBuilder();
                if(indent == 0)
                {
                    List<Tdata> enumData = Sorter.sortByOrdinal(type.getData());
                    fb.type.append(new StringBuilder().append((type.getName() != null)? "enum " + type.getName() + "\n{\n" : "enum\n{\n"));

                    indent++;

                    for(int i = 0; i < enumData.size() - 1; i++)
                    {
                        fb.type.append(retIndent(indent) + enumData.get(i).getName() + " = " + enumData.get(i).getOffset() + ",\n");
                    }

                    fb.type.append(retIndent(indent) + enumData.get(enumData.size() - 1).getName() + " = " + enumData.get(enumData.size() - 1).getOffset());
                    fb.type.append(retIndent(--indent) + "\n};");
                }
                else
                {
                    fb.type.append("enum " + getModifier(type)).append(type.getName());
                }
                return fb;
            }
            case UNION:
            {
                FieldBuilder fb = new FieldBuilder();
                if(indent == 0)
                {
                    List<Tdata> unionFields = Sorter.sortByOrdinal(type.getData());

                    fb.setType(new StringBuilder((type.getName() != null)? "union " + type.getName() + "\n{\n" : "union\n{\n"));

                    indent++;

                    for(Tdata i: unionFields)
                    {
                        type = rep.findOne(i.getId());
                        FieldBuilder field = FieldBuilder.recoursionProcessing(rep, type, 0);
                        field.setName(i.getName() + ";");
                        fb.setType(new StringBuilder(fb.getType() + retIndent(indent) + field.toString() + "\n"));
                    }
                    fb.setType(fb.getType().append(retIndent(--indent) + "};"));
                }
                else
                {
                    //unnamed unions sometimes include structures
                    if(type.getName().equals("<unnamed-tag>"))
                    {
                        fb.type.append("union " + getModifier(type));

                        if(type.getData() != null)
                        {
                            fb.type.append("\n").append(retIndent(indent)).append("{");

                            List<Tdata> fields = Sorter.sortByOrdinal(type.getData());
                            indent++;

                            //"beginning" means the beginning of a nested structure
                            boolean beginning = false;

                            int last = fields.size() - 1;
                            for(int i = 0; i < fields.size(); i++)
                            {
                                type = rep.findOne(fields.get(i).getId());

                                FieldBuilder field = FieldBuilder.recoursionProcessing(rep, type, indent);
                                field.setName(fields.get(i).getName());

                                if(i == last)
                                {
                                    //on last iteration we close braces for nested structure (if there was such structure before this iteration)
                                    if(beginning) fb.type.append("\n").append(retIndent(indent)).append("};");

                                    //processing of the last iteration
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");

                                    break; //cause comparing 'fields.size()-1' and 'fields.size()' iteration will lead to exception
                                }

                                //a same offset between two fields means that they are the fields of the same union or the same structure
                                if(fields.get(i).getOffset() == fields.get(i + 1).getOffset())
                                {
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");
                                }

                                //a different offset and the fields aren't inside of structure
                                if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && !beginning)
                                {
                                    //'opening' a structure
                                    beginning = true;

                                    field.type = new StringBuilder("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1) + field.type);

                                    //processing of current iteration
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");
                                }
                                else if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && beginning)
                                {
                                    //a different offset and previous field was inside of structure ->
                                    //processing a current iteration and 'closing' the structure
                                    beginning = false;
                                    fb.type.append("\n").append(retIndent(indent + 1)).append(field.toString()).append(";");
                                    fb.type.append("\n").append(retIndent(indent)).append("};");
                                }

                            }
                            fb.type.append("\n").append(retIndent(--indent)).append("}");
                        }
                    }
                    else
                    {
                        fb.type.append("union " + getModifier(type)).append(type.getName());
                    }
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
