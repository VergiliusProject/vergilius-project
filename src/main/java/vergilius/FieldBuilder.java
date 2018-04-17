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
    private int pocket = 0;
    private int realLength = 0;

    //The method returns a string indent for a type, which depends on nesting level of this type
    public static String retIndent(int indent)
    {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < indent; i++)
        {
            //4 spaces
            str.append("    ");
        }
        return str.toString();
    }

    public static String retSpaces(int length)
    {
        StringBuilder outputBuffer = new StringBuilder();
        if(length <= 50)
        {
            for (int i = 0; i <= 50 - length; i++){
                outputBuffer.append(" ");
            }
            return outputBuffer.toString() + "//0x";
        }
        else
        {
            //8 spaces
            return "        " + "//0x";
        }
    }

    public String toString()
    {
        if(retval.toString().isEmpty())
        {
            return type + " " + name + dim;
        }
        return retval + "(" + type + name + dim + ")" + "(" + args + ")";
    }

    public static boolean isTopLevel(int indent)
    {
        return indent == 0;
    }

    public static void printStructFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int sumOffset, String link)
    {
        //if structure isn't bodiless
        if(type.getData() != null && type.getSizeof() != 0)
        {
            fb.type.append("\n").append(retIndent(indent)).append("{");

            indent++; //all structure fields have a deeper level of nesting
            List<Tdata> structFields = Sorter.sortByOrdinal(type.getData());

            for (Tdata currentField : structFields)
            {
                type = repo.findOne(currentField.getId());

                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, sumOffset + currentField.getOffset(), link);
                field.setName(currentField.getName());
                field.pocket = sumOffset + currentField.getOffset();

                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";");

                //OFFSET
                if(field.realLength != 0)
                {
                    fb.type.append(retSpaces(field.realLength) + field.pocket);
                }
                else
                {
                    fb.type.append(retSpaces(field.toString().length()) + field.pocket);
                }

            }
            fb.type.append("\n").append(retIndent(--indent)).append("}");// braces are always on the same level with a word 'struct'
        }
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
        return (typeOfField.isIsConst() ? "const" : "") + (typeOfField.isIsVolatile() ? "volatile" : "");
    }

    //The method returns an object(fb), which represents some type.
    public static FieldBuilder recursionProcessing(TtypeRepository repo, Ttype type, int indent, int sumOffset, String link)
    {
        switch (type.getKind())
        {
            case BASE:
            {
                FieldBuilder fb = new FieldBuilder();
                fb.type.append(getModifier(type).isEmpty()? type.getName():getModifier(type) + " " + type.getName()); //type.getName() -> int, char etc.

                return fb;
            }
            case POINTER:
            {
                //getting a new type on which "current" type points to
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());

                String name = repo.findOne(refType.getId()).getName();

                if(repo.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT && name.equals("<unnamed-tag>"))
                {
                    FieldBuilder fb = FieldBuilder.recursionProcessing(repo, repo.findOne(refType.getId()), indent, sumOffset, link);
                    fb.type.append("*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));

                    fb.pocket = sumOffset;

                    return fb;
                }

                //union, enum...
                if(repo.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT)
                {
                    FieldBuilder fb = new FieldBuilder();

                    fb.type.append("struct " + "<a href='" + link + name + "'>" + name + "</a>" + "*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));

                    fb.realLength = (fb.type.toString() + " " + ";").length() - ("<a href='" + link +"'>" + "</a>").length();

                    return fb;
                }

                FieldBuilder fb = recursionProcessing(repo, repo.findOne(refType.getId()), indent, sumOffset, link);
               
                fb.type.append("*" + (getModifier(type).isEmpty() ? "" : " " + getModifier(type)));
                return fb;

            }
            case ARRAY:
            {
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                type = repo.findOne(refType.getId());
                FieldBuilder fb = recursionProcessing(repo, type, indent, sumOffset, link);
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
                    Ttype typeOfComponent = repo.findOne(component.getId());
                    FieldBuilder fbType = recursionProcessing(repo, typeOfComponent, indent, sumOffset, link);

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

                if(isTopLevel(indent))
                {
                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append("struct" + getModifier(type)).append(type.getName().equals("<unnamed-tag>") ? "" : (" " +  type.getName()));
                    printStructFields(fb, type, repo, indent, sumOffset, link);
                    fb.type.append(";");
                }
                else
                {
                    fb.type.append("struct" + getModifier(type)).append(type.getName().equals("<unnamed-tag>") ? "" : (" " +  "<a href='" + link + type.getName() +"'>" + type.getName()) +  "</a>");
                    printStructFields(fb, type, repo, indent, sumOffset, link);
                }
                return fb;
            }
            case ENUM:
            {
                FieldBuilder fb = new FieldBuilder();
                if(isTopLevel(indent))
                {
                    List<Tdata> enumData = Sorter.sortByOrdinal(type.getData());

                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

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
                    fb.type.append("enum " + getModifier(type)).append("<a href='" + link + type.getName() + "'>" + type.getName() +  "</a>");
                    fb.realLength = ("enum " + getModifier(type) + " " + type.getName() + ";").length();
                }
                return fb;
            }
            case UNION:
            {
                FieldBuilder fb = new FieldBuilder();
                if(isTopLevel(indent))
                {
                    List<Tdata> unionFields = Sorter.sortByOrdinal(type.getData());

                    //hex SIZE
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append(new StringBuilder((type.getName() != null)? ("union " +  type.getName() + "\n{\n") : "union\n{\n"));

                    indent++;

                    for(Tdata unionField: unionFields)
                    {
                        type = repo.findOne(unionField.getId());
                        FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, 0, sumOffset, link);
                        field.setName(unionField.getName() + ";");

                        fb.type.append(retIndent(indent) + field.toString() + "\n");
                    }
                    fb.type.append(retIndent(--indent) + "};");
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
                                type = repo.findOne(fields.get(i).getId());

                                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, sumOffset + fields.get(i).getOffset(), link);
                                field.setName(fields.get(i).getName());
                                field.pocket = sumOffset + fields.get(i).getOffset();

                                if(i == last)
                                {
                                    //on last iteration we close braces for nested structure (if there was such structure before this iteration)
                                    if(beginning)
                                    {
                                        fb.type.append("\n").append(retIndent(indent)).append("};");
                                    }

                                    //processing of the last iteration
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString() + ";" + retSpaces(field.toString().length()) + field.pocket);

                                    break; //cause comparing 'fields.size()-1' and 'fields.size()' iteration will lead to exception
                                }

                                //a same offset between two fields means that they are the fields of the same union or the same structure
                                if(fields.get(i).getOffset() == fields.get(i + 1).getOffset())
                                {
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString() + ";" + retSpaces(field.toString().length()) + field.pocket);
                                }

                                //a different offset and the fields aren't inside of structure
                                if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && !beginning)
                                {
                                    //'opening' a structure
                                    beginning = true;

                                    field.type = new StringBuilder("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1) + field.type);


                                    //processing of current iteration
                                    fb.type.append("\n").append(retIndent(indent)).append(field.toString() + ";" + retSpaces(field.toString().length()) + field.pocket);

                                }
                                else if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && beginning)
                                {
                                    //a different offset and previous field was inside of structure ->
                                    //processing a current iteration and 'closing' the structure
                                    beginning = false;
                                    fb.type.append("\n").append(retIndent(indent + 1)).append(field.toString() + ";" + retSpaces(field.toString().length())  + field.pocket);

                                    fb.type.append("\n").append(retIndent(indent)).append("};");
                                }

                            }
                            fb.type.append("\n").append(retIndent(--indent)).append("}");
                        }
                    }
                    else
                    {
                        fb.type.append("union " + getModifier(type)).append("<a href='" + link + type.getName() + "'>" + type.getName() +  "</a>");
                        fb.realLength = ("union " + getModifier(type) + " " + type.getName() + ";").length();
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
