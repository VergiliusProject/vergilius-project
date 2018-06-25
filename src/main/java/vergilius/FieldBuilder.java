package vergilius;

import vergilius.repos.TtypeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FieldBuilder
{
    private String name = new String();
    private StringBuilder type = new StringBuilder();
    private StringBuilder dim = new StringBuilder();
    private StringBuilder retval = new StringBuilder();
    private StringBuilder args = new StringBuilder();
    private int fbOffset = 0;

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
        if(length <= 75)
        {
            for (int i = 0; i <= 75 - length; i++){
                outputBuffer.append(" ");
            }
            return outputBuffer.toString() + "//0x";
        }
        else
        {
            return " //0x";
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

    public static void printEnumFields(FieldBuilder fb, Ttype type, int indent)
    {
        //if not bodiless
        if(type.getData() != null && type.getSizeof() != 0)
        {
            fb.type.append("\n" + retIndent(indent) + "{");

            List<Tdata> enumData = Sorter.sortByOrdinal(type.getData());
            indent++;

            for(int i = 0; i < enumData.size() - 1; i++)
            {
                fb.type.append("\n" + retIndent(indent) + enumData.get(i).getName() + " = " + enumData.get(i).getOffset() + ",");
            }

            fb.type.append("\n" + retIndent(indent) + enumData.get(enumData.size() - 1).getName() + " = " + enumData.get(enumData.size() - 1).getOffset());

            fb.type.append(retIndent(--indent) + "\n}");
        }
    }

    public static void printStructFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link)
    {
        //if structure isn't bodiless
        if(type.getData() != null && type.getSizeof() != 0)
        {
            fb.type.append("\n").append(retIndent(indent)).append("{");

            //all structure fields have a deeper level of nesting
            indent++;

            List<Tdata> structFields = Sorter.sortByOrdinal(type.getData());

            for (Tdata currentField : structFields)
            {
                type = repo.findOne(currentField.getId());

                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + currentField.getOffset(), link);
                field.setName(currentField.getName());
                field.fbOffset = rpOffset + currentField.getOffset();

                fb.type.append("\n").append(retIndent(indent)).append(field.toString()).append(";"); //string creation

                String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
                String[] splitted = str.toString().replaceAll("(<.+?>)", "").split("\n");

                int displayLength = splitted[splitted.length - 1].length();
                fb.type.append(retSpaces(displayLength) + Integer.toHexString(field.fbOffset));

            }
            fb.type.append("\n").append(retIndent(--indent)).append("}");// braces are always on the same level with a word 'struct'
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    //The method checks type's modifier (const or volatile)
    public static String getModifier(Ttype typeOfField)
    {
        return (typeOfField.isIsConst() ? "const" : "").isEmpty()?(typeOfField.isIsVolatile() ? "volatile" : ""): (typeOfField.isIsVolatile() ? "volatile" : "");
    }

    public static void printUnionFields(FieldBuilder fb, Ttype type, TtypeRepository repo, int indent, int rpOffset, String link)
    {
        if(type.getData() != null && type.getSizeof() != 0)
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

                FieldBuilder field = FieldBuilder.recursionProcessing(repo, type, indent, rpOffset + fields.get(i).getOffset(), link);
                field.setName(fields.get(i).getName());
                field.fbOffset = rpOffset + fields.get(i).getOffset();

                if(i == last)
                {
                    //on last iteration we close braces for nested structure (if there was such structure before this iteration)
                    if(beginning)
                    {
                        fb.type.append("\n").append(retIndent(indent)).append("};");
                    }
                    //processing of the last iteration
                    // in union struct{...} s;

                    String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
                    String[] splitted = str.toString().replaceAll("(<.+?>)", "").split("\n");
                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str + retSpaces(displayLength) + Integer.toHexString(field.fbOffset));

                    break; //comparing 'fields.size()-1' and 'fields.size()' iteration leads to exception
                }

                //a same offset between two fields means that they are the fields of the same union or the same structure
                if(fields.get(i).getOffset() == fields.get(i + 1).getOffset())
                {
                    String str = new StringBuilder("\n").append(retIndent(indent)).append(field.toString()).append(";").toString();
                    String[] splitted = str.toString().replaceAll("(<.+?>)", "").split("\n");
                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str + retSpaces(displayLength) + Integer.toHexString(field.fbOffset));

                }

                //a different offset and the fields aren't inside of structure
                if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && !beginning)
                {
                    //'opening' a structure
                    beginning = true;

                    //processing of current iteration

                    String str = new StringBuilder("\n" + retIndent(indent)).append("struct\n" + retIndent(indent) + "{\n" + retIndent(indent + 1)).append(field.toString()).append(";").toString();
                    String[] splitted = str.toString().replaceAll("(<.+?>)", "").split("\n");

                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str + retSpaces(displayLength) + Integer.toHexString(field.fbOffset));
                }
                else if(fields.get(i).getOffset() != fields.get(i + 1).getOffset() && beginning)
                {
                    //a different offset and previous field was inside of structure ->
                    //processing a current iteration and 'closing' the structure
                    beginning = false;

                    String str = new StringBuilder("\n").append(retIndent(indent + 1)).append(field.toString()).append(";").toString();
                    String[] splitted = str.toString().replaceAll("(<.+?>)", "").split("\n");

                    int displayLength = splitted[splitted.length - 1].length();
                    fb.type.append(str + retSpaces(displayLength) + Integer.toHexString(field.fbOffset));

                    fb.type.append("\n").append(retIndent(indent)).append("};");
                }
            }
            indent--;
            fb.type.append("\n").append(retIndent(indent)).append("}");
        }
    }

    //The method returns an object(fb), which represents some type.
    public static FieldBuilder recursionProcessing(TtypeRepository repo, Ttype type, int indent, int rpOffset, String link)
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
                    FieldBuilder fb = FieldBuilder.recursionProcessing(repo, repo.findOne(refType.getId()), indent, rpOffset, link);
                    fb.type.append("*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                    fb.fbOffset = rpOffset;
                    return fb;
                }

                //union, enum... ???
                if(repo.findOne(refType.getId()).getKind() == Ttype.Kind.STRUCT)
                {
                    FieldBuilder fb = new FieldBuilder();

                    fb.type.append("struct" + "<a class='str-link' tabindex='-1' href='" + link + name + "'> " + name + "</a>" + "*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                    return fb;
                }

                FieldBuilder fb = recursionProcessing(repo, repo.findOne(refType.getId()), indent, rpOffset, link);
                fb.type.append("*" + (getModifier(type).isEmpty() ? "" : (" " + getModifier(type))));
                return fb;

            }
            case ARRAY:
            {
                Tdata refType = type.getData().stream().findFirst().orElseThrow(()-> new NoSuchElementException());
                type = repo.findOne(refType.getId());
                FieldBuilder fb = recursionProcessing(repo, type, indent, rpOffset, link);
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
                    FieldBuilder fbType = recursionProcessing(repo, typeOfComponent, indent, rpOffset, link);
                    if ("return".equals(component.getName()))
                    {
                        fb.retval.append(fbType.toString());
                    }
                    else
                    {
                        fbType.name = "arg" + counter;
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
                    //size of structure in hex format
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append(getModifier(type).isEmpty()? "" : (getModifier(type) + " ")).append("struct" + " " +  type.getName());

                    printStructFields(fb, type, repo, indent, rpOffset, link);
                    fb.type.append(";");
                }
                else
                {   //not top-level structure without name should be displayed with it's all fields
                    //directly at that place, where it's declared
                    if(type.getName().equals("<unnamed-tag>"))
                    {
                        fb.type.append("struct");
                        printStructFields(fb, type, repo, indent, rpOffset, link);

                    }
                    else
                    {
                        //not top-level structures should be displayed with links
                        fb.type.append(getModifier(type).isEmpty()? "" : (getModifier(type) + " ")).append("struct" + " <a class='str-link' tabindex='-1' href='" + link + type.getName() +"'>" + type.getName() + "</a>");

                    }
                }
                return fb;
            }
            case ENUM:
            {
                FieldBuilder fb = new FieldBuilder();

                //top-level enum(structure, union) always have a name
                if(isTopLevel(indent))
                {
                    //size (hex)
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");
                    fb.type.append(new StringBuilder("enum " + type.getName()));
                    printEnumFields(fb, type, indent);
                    fb.type.append(";");
                }
                else
                {
                    if(type.getName().equals("<unnamed-tag>"))
                    {
                        fb.type.append(new StringBuilder("enum"));
                        printEnumFields(fb, type, indent);
                    }
                    else
                    {
                        fb.type.append("enum" + (getModifier(type).isEmpty()? "" : (getModifier(type)))).append("<a class='str-link' tabindex='-1' href='" + link + type.getName() + "'> " + type.getName() + "</a>");
                    }
                }
                return fb;
            }
            case UNION:
            {
                FieldBuilder fb = new FieldBuilder();
                if(isTopLevel(indent))
                {
                    //size (hex)
                    fb.type.append("//0x" + Integer.toHexString(type.getSizeof()) + " bytes (sizeof)\n");

                    fb.type.append(new StringBuilder("union " +  type.getName()));

                    printUnionFields(fb, type, repo, indent, rpOffset, link);

                    fb.type.append(";");
                }
                else
                {
                    if(type.getName().equals("<unnamed-tag>"))
                    {
                        fb.type.append("union " + getModifier(type));

                        printUnionFields(fb, type, repo, indent, rpOffset, link);

                    }
                    else
                    {
                        fb.type.append("union" + (getModifier(type).isEmpty()? "" : (getModifier(type)))).append("<a class='str-link' tabindex='-1' href='" + link + type.getName() + "'> " + type.getName() + "</a>");
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
